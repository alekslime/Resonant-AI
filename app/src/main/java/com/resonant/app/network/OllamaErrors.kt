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
