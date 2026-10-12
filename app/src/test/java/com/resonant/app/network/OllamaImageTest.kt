package com.resonant.app.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test

class OllamaImageTest {

    @Test
    fun the_request_carries_the_picture_as_base64_and_is_not_streamed() {
        val body = buildImageRequest("llama3.2-vision", "Describe it.", byteArrayOf(1, 2, 3))
        assertEquals("llama3.2-vision", body.getString("model"))
        assertFalse(body.getBoolean("stream"))
        val message = body.getJSONArray("messages").getJSONObject(0)
        assertEquals("user", message.getString("role"))
        assertEquals("Describe it.", message.getString("content"))
        assertEquals("AQID", message.getJSONArray("images").getString(0))
    }

    @Test
    fun the_reply_text_is_read_and_trimmed() {
        assertEquals("A bar chart.", parseChatReply("""{"message":{"role":"assistant","content":"  A bar chart.\n"},"done":true}"""))
        assertEquals("", parseChatReply("""{"done":true}"""))
    }

    @Test
    fun an_error_body_is_reported() {
        try {
            parseChatReply("""{"error":"model 'x' not found"}""")
            fail("expected an exception")
        } catch (e: OllamaStreamException) {
            assertEquals("Ollama error: model 'x' not found", e.message)
        }
    }
}
