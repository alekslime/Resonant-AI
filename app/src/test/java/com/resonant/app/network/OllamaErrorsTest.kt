package com.resonant.app.network

import org.json.JSONException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class OllamaErrorsTest {

    private val body = """{"error":"model 'llama3.2' not found, try pulling it first"}"""

    @Test
    fun a_404_says_the_model_is_missing() {
        val spoken = spokenErrorFor(OllamaHttpException(404, body))
        assertTrue(spoken, spoken.contains("model"))
    }

    @Test
    fun other_http_errors_say_the_status_code() {
        assertTrue(spokenErrorFor(OllamaHttpException(500, null)).contains("500"))
    }

    @Test
    fun raw_server_text_is_never_spoken() {
        val errors = listOf(
            OllamaHttpException(404, body),
            OllamaHttpException(500, body),
            OllamaStreamException(body)
        )
        errors.forEach { e ->
            val spoken = spokenErrorFor(e)
            assertFalse(spoken, spoken.contains("{"))
            assertFalse(spoken, spoken.contains("llama3.2"))
        }
    }

    @Test
    fun unreachable_server_variants_say_so() {
        listOf(ConnectException("refused"), UnknownHostException("nope")).forEach {
            assertTrue(spokenErrorFor(it).contains("couldn't reach"))
        }
    }

    @Test
    fun timeouts_and_dropped_connections_are_told_apart() {
        assertTrue(spokenErrorFor(SocketTimeoutException("Read timed out")).contains("in time"))
        assertTrue(spokenErrorFor(SocketException("Connection reset")).contains("dropped"))
    }

    @Test
    fun blocked_cleartext_points_at_https() {
        val spoken = spokenErrorFor(IOException("Cleartext HTTP traffic to 192.168.1.50 not permitted"))
        assertTrue(spoken, spoken.contains("https"))
    }

    @Test
    fun unreadable_replies_and_unknown_failures_get_a_plain_sentence() {
        assertTrue(spokenErrorFor(JSONException("bad")).contains("couldn't read"))
        assertTrue(spokenErrorFor(IllegalStateException("boom")).contains("Something went wrong"))
    }

    @Test
    fun every_message_ends_by_saying_how_to_retry() {
        listOf(
            OllamaHttpException(404, null), OllamaStreamException("x"), ConnectException(),
            SocketTimeoutException(), SocketException(), JSONException("x"),
            IOException("Cleartext"), IllegalStateException()
        ).forEach { assertTrue(spokenErrorFor(it).endsWith("Tap the center to try again.")) }
    }

    @Test
    fun unreachable_covers_connection_and_resolution_failures() {
        listOf(
            ConnectException("refused"),
            NoRouteToHostException("no route"),
            UnknownHostException("nope"),
            SocketTimeoutException("Read timed out"),
            SocketException("Connection reset")
        ).forEach { assertTrue(it.toString(), isServerUnreachable(it)) }
    }

    @Test
    fun unreachable_is_false_once_the_server_has_answered() {
        // A response the server actually sent back - even an error one - is a
        // different situation from never getting through, and retrying makes
        // sense here in a way it doesn't for a dead connection.
        listOf(
            OllamaHttpException(404, body),
            OllamaHttpException(500, null),
            OllamaStreamException(body),
            JSONException("bad"),
            IOException("Cleartext HTTP traffic to 192.168.1.50 not permitted"),
            IllegalStateException("boom")
        ).forEach { assertFalse(it.toString(), isServerUnreachable(it)) }
    }
}
