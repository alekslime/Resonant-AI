package com.resonant.app.livekit

import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Where a Live connection failed: getting the join token from the PC, or joining the room. */
enum class ConnectPhase { TOKEN, ROOM }

/** The PC's token server answered, but not with a token. */
class TokenServerException(val code: Int) : IOException("Token server answered $code")

/** What the person hears and reads when Live cannot connect. */
data class LiveError(val spoken: String, val short: String)

/**
 * Turns a failed Live connection into something a blind person can act on: what is wrong and what
 * to check first. Pure Kotlin, tested on the JVM.
 */
object LiveErrors {

    const val GENERIC_SPOKEN = "Couldn't connect to Resonant Live. Check your PC, then go back and open Chat again."
    const val GENERIC_SHORT = "Connection error"

    fun describe(phase: ConnectPhase, error: Throwable): LiveError {
        val causes = generateSequence(error) { it.cause }.take(6).toList()
        val token = causes.filterIsInstance<TokenServerException>().firstOrNull()
        val timedOut = causes.any { it is SocketTimeoutException || it is NoRouteToHostException }
        val refused = causes.any { it is ConnectException }
        val unknownHost = causes.any { it is UnknownHostException }

        return when (phase) {
            ConnectPhase.TOKEN -> when {
                token != null -> tokenServerAnswer(token.code)
                timedOut -> LiveError(
                    "I can't reach your PC. Check that it is on and on the same Wi-Fi as this phone. " +
                        "If you changed Wi-Fi, its address may have changed. Then go back and open Chat again.",
                    "PC not reachable"
                )
                refused -> LiveError(
                    "Your PC is there, but the Resonant servers aren't running on it. Start them on your PC, " +
                        "then go back and open Chat again.",
                    "Servers not running"
                )
                unknownHost -> LiveError(
                    "I can't find the PC address. Check the token address in Server setup.",
                    "Bad PC address"
                )
                else -> LiveError(GENERIC_SPOKEN, GENERIC_SHORT)
            }
            ConnectPhase.ROOM -> when {
                timedOut || refused || unknownHost -> LiveError(
                    "Your PC answered, but I can't reach the Live service. Live needs internet. " +
                        "Check that this phone is online, then go back and open Chat again.",
                    "No internet"
                )
                refusedByService(causes) -> LiveError(
                    "The Live service refused the connection. This is usually because the clock on your PC is wrong. " +
                        "Set its date and time to automatic, then open Chat again.",
                    "Live service refused"
                )
                else -> LiveError(GENERIC_SPOKEN, GENERIC_SHORT)
            }
        }
    }

    private fun tokenServerAnswer(code: Int): LiveError = when {
        code == 401 || code == 403 -> LiveError(
            "Your PC refused the token key. The key on the phone and the key on the PC don't match. " +
                "Fix it in Server setup, then open Chat again.",
            "Wrong token key"
        )
        code == 404 -> LiveError(
            "Your PC has no token page at that address. Check the token address in Server setup.",
            "Bad token address"
        )
        code >= 500 -> LiveError(
            "The token server on your PC had a problem. Look at the PC's terminal for the error, " +
                "then open Chat again.",
            "PC server error"
        )
        else -> LiveError(GENERIC_SPOKEN, GENERIC_SHORT)
    }

    private fun refusedByService(causes: List<Throwable>): Boolean = causes.any {
        val m = it.message?.lowercase() ?: ""
        "401" in m || "unauthorized" in m || "invalid token" in m || "expired" in m || "not allowed" in m
    }
}
