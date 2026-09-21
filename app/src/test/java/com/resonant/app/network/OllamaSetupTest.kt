package com.resonant.app.network

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class OllamaSetupTest {

    // ---- normalizeBaseUrl

    @Test
    fun a_bare_host_gets_http_and_the_default_port() {
        assertEquals("http://192.168.1.50:11434", normalizeBaseUrl("192.168.1.50"))
        assertEquals("http://localhost:11434", normalizeBaseUrl("localhost"))
    }

    @Test
    fun host_and_port_without_a_scheme_gets_http() {
        assertEquals("http://192.168.1.50:11434", normalizeBaseUrl("192.168.1.50:11434"))
        assertEquals("http://10.0.0.7:8080", normalizeBaseUrl("10.0.0.7:8080"))
    }

    @Test
    fun whitespace_and_trailing_slashes_are_dropped() {
        assertEquals("http://192.168.1.50:11434", normalizeBaseUrl("  http://192.168.1.50:11434/  "))
        assertEquals("http://host:8080", normalizeBaseUrl("http://host:8080///"))
    }

    @Test
    fun https_is_kept_and_no_port_is_forced_on_it() {
        assertEquals("https://ollama.example.com", normalizeBaseUrl("https://ollama.example.com"))
        assertEquals("https://ollama.example.com:8443", normalizeBaseUrl("https://ollama.example.com:8443"))
    }

    @Test
    fun a_path_prefix_is_kept() {
        assertEquals("https://example.com/ollama", normalizeBaseUrl("https://example.com/ollama/"))
    }

    @Test
    fun the_scheme_is_case_insensitive() {
        assertEquals("http://192.168.1.50:11434", normalizeBaseUrl("HTTP://192.168.1.50"))
    }

    @Test
    fun things_that_are_not_addresses_are_rejected() {
        assertNull(normalizeBaseUrl(""))
        assertNull(normalizeBaseUrl("   "))
        assertNull(normalizeBaseUrl("not an address"))
        assertNull(normalizeBaseUrl("ftp://192.168.1.50"))
        assertNull(normalizeBaseUrl("http://"))
    }

    // ---- parseModelNames / modelIsInstalled

    @Test
    fun reads_model_names_from_a_tags_response() {
        val body = """{"models":[{"name":"llama3.2:latest","size":1},{"name":"qwen2.5:7b","size":2}]}"""
        assertEquals(listOf("llama3.2:latest", "qwen2.5:7b"), parseModelNames(body))
    }

    @Test
    fun an_empty_or_missing_model_list_is_empty() {
        assertEquals(emptyList<String>(), parseModelNames("""{"models":[]}"""))
        assertEquals(emptyList<String>(), parseModelNames("""{}"""))
    }

    @Test
    fun a_body_that_is_not_json_throws() {
        try {
            parseModelNames("<html>nope</html>")
            fail("expected a JSONException")
        } catch (expected: JSONException) {
            // this is what lets describeFailure say "doesn't look like Ollama"
        }
    }

    @Test
    fun a_name_without_a_tag_matches_its_latest() {
        val installed = listOf("llama3.2:latest", "qwen2.5:7b")
        assertTrue(modelIsInstalled(installed, "llama3.2"))
        assertTrue(modelIsInstalled(installed, "llama3.2:latest"))
        assertTrue(modelIsInstalled(installed, "  LLAMA3.2  "))
    }

    @Test
    fun a_tagged_name_must_match_exactly() {
        val installed = listOf("qwen2.5:7b")
        assertTrue(modelIsInstalled(installed, "qwen2.5:7b"))
        assertFalse(modelIsInstalled(installed, "qwen2.5:14b"))
        assertFalse(modelIsInstalled(installed, "qwen2.5"))
    }

    @Test
    fun a_blank_or_missing_model_is_not_installed() {
        assertFalse(modelIsInstalled(listOf("llama3.2:latest"), ""))
        assertFalse(modelIsInstalled(emptyList(), "llama3.2"))
    }

    // ---- describeCheck / describeFailure

    @Test
    fun reachable_with_the_model_says_so() {
        val result = ConnectionCheck.Reachable(listOf("llama3.2:latest"), modelInstalled = true)
        assertEquals("Connected. Model \"llama3.2\" is installed.", describeCheck(result, "llama3.2"))
    }

    @Test
    fun reachable_without_the_model_says_how_to_get_it() {
        val result = ConnectionCheck.Reachable(listOf("qwen2.5:7b"), modelInstalled = false)
        val text = describeCheck(result, "llama3.2")
        assertTrue(text.contains("ollama pull llama3.2"))
        assertTrue(text.contains("qwen2.5:7b"))
    }

    @Test
    fun reachable_with_no_models_at_all_says_so() {
        val result = ConnectionCheck.Reachable(emptyList(), modelInstalled = false)
        assertTrue(describeCheck(result, "llama3.2").contains("no models"))
    }

    @Test
    fun unreachable_explains_the_likely_cause() {
        assertTrue(describeFailure(ConnectException("refused")).contains("same Wi-Fi"))
        assertTrue(describeFailure(UnknownHostException("x")).contains("same Wi-Fi"))
        // A timeout is the classic "Ollama only listens on localhost" symptom.
        assertTrue(describeFailure(SocketTimeoutException("t")).contains("OLLAMA_HOST=0.0.0.0"))
        assertTrue(describeFailure(JSONException("bad")).contains("doesn't look like Ollama"))
        assertTrue(describeFailure(OllamaHttpException(500, null)).contains("500"))
    }

    @Test
    fun a_blocked_cleartext_request_points_at_https() {
        val e = java.io.IOException("Cleartext HTTP traffic to 192.168.1.5 not permitted")
        assertTrue(describeFailure(e).contains("https"))
    }

    @Test
    fun an_unreachable_result_reads_as_a_failure_not_a_success() {
        val text = describeCheck(ConnectionCheck.Unreachable(ConnectException("x")), "llama3.2")
        assertTrue(text.startsWith("Couldn't reach"))
        assertFalse(text.startsWith("Connected"))
    }

    @Test
    fun an_unknown_failure_still_says_something_useful() {
        val text = describeFailure(IllegalStateException("weird"))
        assertTrue(text.contains("weird"))
    }
}
