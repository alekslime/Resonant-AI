package com.resonant.app.network

import org.json.JSONException
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private const val RETRY = " Tap the center to try again."

/**
 * True when [e] means the server was never reached at all — refused the connection,
 * timed out, or the address doesn't resolve — as opposed to the server answering with
 * an error (bad HTTP status, a stream error, an unreadable body). That distinction is
 * what [com.resonant.app.content.OfflineAnswers] keys off of: retrying an unreachable
 * server right away won't help, so Chat falls back to lesson content instead of just
 * repeating [spokenErrorFor]. A server that *did* answer, just badly, is left alone —
 * a wrong model name or a malformed reply says nothing about whether the network is up.
 */
internal fun isServerUnreachable(e: Throwable): Boolean = when (e) {
    is ConnectException, is NoRouteToHostException, is UnknownHostException -> true
    // A connect timeout and a read timeout are both SocketTimeoutException; either way
    // nothing came back, which is the same situation as a refused connection.
    is SocketTimeoutException -> true
    is SocketException -> true
    else -> false
}

/**
 * What to SAY when a chat request fails.
 *
 * The exception's own message is for logs. It can be an HTTP body, a JSON blob or a raw
 * socket error, and none of that helps someone who can only hear it. This maps each failure
 * to one plain sentence that says what went wrong and, where there is one, what to do.
 */
internal fun spokenErrorFor(e: Throwable): String {
    val what = when {
        e is OllamaHttpException && e.status == 404 ->
            "The AI server doesn't have the model installed. Pull it on the server."
        e is OllamaHttpException -> "The AI server returned an error, code ${e.status}."
        e is OllamaStreamException -> "The AI server reported an error while answering."
        e is ConnectException || e is NoRouteToHostException || e is UnknownHostException ->
            "I couldn't reach the AI server. Check that it's running and on the same Wi-Fi."
        // A connect timeout and a read timeout are both SocketTimeoutException and can't
        // be told apart by type, so the wording has to cover both.
        e is SocketTimeoutException ->
            "The AI server didn't respond in time. Check that it's running and on the same Wi-Fi."
        e is SocketException -> "The connection to the AI server dropped."
        e is JSONException -> "The AI server sent a reply I couldn't read."
        e is IOException && e.message?.contains("Cleartext", ignoreCase = true) == true ->
            "This build blocks plain HTTP. Point it at an https address."
        else -> "Something went wrong talking to the AI."
    }
    return what + RETRY
}
