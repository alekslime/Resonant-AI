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

    /**
     * [hosted] is a build where the server is run for the person (`app.hosted` in local.properties): they have no PC,
     * address or key to fix, so the messages are shorter and never blame their PC.
     */
    fun describe(phase: ConnectPhase, error: Throwable, hosted: Boolean = false): LiveError {
        if (hosted) return describeHosted(phase, error)
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

    const val HOSTED_GENERIC_SPOKEN =
        "Couldn't connect to Resonant. Check your internet connection, then go back and open Chat again."

    /** What a person hears when the server is run for them: only things they can actually do something about. */
    private fun describeHosted(phase: ConnectPhase, error: Throwable): LiveError {
        val causes = generateSequence(error) { it.cause }.take(6).toList()
        val token = causes.filterIsInstance<TokenServerException>().firstOrNull()
        val network = causes.any {
            it is SocketTimeoutException || it is NoRouteToHostException || it is ConnectException || it is UnknownHostException
        }
        return when {
            phase == ConnectPhase.TOKEN && token != null -> when {
                token.code == 401 || token.code == 403 -> LiveError(
                    "Resonant couldn't verify this copy of the app. Update the app from where you got it, then try again.",
                    "Not allowed"
                )
                token.code == 404 -> LiveError(
                    "Resonant has moved. Update the app from where you got it, then try again.",
                    "Update needed"
                )
                token.code >= 500 -> LiveError(
                    "Resonant has a problem on its side right now. Please try again in a few minutes.",
                    "Server problem"
                )
                else -> LiveError(HOSTED_GENERIC_SPOKEN, GENERIC_SHORT)
            }
            network -> LiveError(
                "Couldn't reach Resonant. Check that this phone is online, then go back and open Chat again.",
                "No internet"
            )
            phase == ConnectPhase.ROOM && refusedByService(causes) -> LiveError(
                "The Live service refused the connection. Please try again in a few minutes.",
                "Live service refused"
            )
            else -> LiveError(HOSTED_GENERIC_SPOKEN, GENERIC_SHORT)
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

/**
 * The other spoken and shown lines that name the PC. For a hosted build they talk about Resonant instead, since
 * the person has no PC to check. Plain strings, so the wording is unit tested.
 */
object LiveWording {
    fun agentMissingSpoken(hosted: Boolean) =
        if (hosted) "Resonant isn't responding right now. Please try again in a few minutes."
        else "Your PC isn't responding. Check that the Resonant agent is running on your computer."

    fun lostSpoken(hosted: Boolean) = if (hosted) "Lost connection to Resonant." else "Lost connection to your PC."

    fun notConnectedSpoken(hosted: Boolean) = if (hosted) "Resonant isn't connected." else "Your PC isn't connected."

    fun statusAgentMissing(hosted: Boolean) = if (hosted) "Live · Not responding" else "Live · PC not responding"

    fun statusConnecting(hosted: Boolean) = if (hosted) "Live · Connecting…" else "Live · Connecting to your PC…"
}
