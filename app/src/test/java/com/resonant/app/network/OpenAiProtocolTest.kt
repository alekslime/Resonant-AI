package com.resonant.app.network

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiProtocolTest {

    private fun text(line: String) = (parseSseLine(line) as SseEvent.Text).text

    @Test fun `a content chunk gives its text`() {
        assertEquals("A fraction", text("""data: {"choices":[{"index":0,"delta":{"content":"A fraction"}}]}"""))
    }

    @Test fun `spaces and the data prefix with no space are handled`() {
        assertEquals(" is", text("""data:{"choices":[{"delta":{"content":" is"}}]}"""))
    }

    @Test fun `the done marker ends the stream`() {
        assertEquals(SseEvent.Done, parseSseLine("data: [DONE]"))
    }

    @Test fun `blank lines comments and other fields are skipped`() {
        assertEquals(SseEvent.Skip, parseSseLine(""))
        assertEquals(SseEvent.Skip, parseSseLine(": keep-alive"))
        assertEquals(SseEvent.Skip, parseSseLine("event: message"))
        assertEquals(SseEvent.Skip, parseSseLine("data: "))
    }

    @Test fun `the role-only first chunk is skipped`() {
        assertEquals(SseEvent.Skip, parseSseLine("""data: {"choices":[{"delta":{"role":"assistant"}}]}"""))
    }

    @Test fun `a null content is skipped and never spoken as the word null`() {
        assertEquals(SseEvent.Skip, parseSseLine("""data: {"choices":[{"delta":{"content":null},"finish_reason":"stop"}]}"""))
    }

    @Test fun `the last chunk with an empty delta is skipped`() {
        assertEquals(SseEvent.Skip, parseSseLine("""data: {"choices":[{"delta":{},"finish_reason":"stop"}]}"""))
        assertEquals(SseEvent.Skip, parseSseLine("""data: {"choices":[]}"""))
    }

    @Test fun `reasoning text is never returned`() {
        assertEquals(
            SseEvent.Skip,
            parseSseLine("""data: {"choices":[{"delta":{"reasoning_content":"let me think about fractions"}}]}""")
        )
        assertEquals(
            "Half.",
            text("""data: {"choices":[{"delta":{"reasoning_content":"hmm","content":"Half."}}]}""")
        )
    }

    @Test fun `unicode and escapes come through`() {
        assertEquals("½ \"of\" it\n", text("""data: {"choices":[{"delta":{"content":"½ \"of\" it\n"}}]}"""))
    }

    @Test fun `an error in the stream throws with its message`() {
        try {
            parseSseLine("""data: {"error":{"message":"rate limited","code":"429"}}""")
            throw AssertionError("should have thrown")
        } catch (e: CloudAiStreamException) {
            assertEquals("rate limited", e.detail)
        }
    }

    // ---- requests ----

    private val chat = listOf(ChatMessage("system", "Be brief."), ChatMessage("user", "Hi"))

    @Test fun `a chat body has the model, the stream flag, the limit and every message`() {
        val body = JSONObject(buildChatBody("muse-spark-1.3", chat, stream = true, maxTokens = 500).toString())
        assertEquals("muse-spark-1.3", body.getString("model"))
        assertTrue(body.getBoolean("stream"))
        assertEquals(500, body.getInt("max_completion_tokens"))
        assertFalse(body.has("max_tokens"))
        val messages = body.getJSONArray("messages")
        assertEquals(2, messages.length())
        assertEquals("system", messages.getJSONObject(0).getString("role"))
        assertEquals("Hi", messages.getJSONObject(1).getString("content"))
    }

    @Test fun `only Muse models are asked for low reasoning`() {
        assertEquals("low", buildChatBody("muse-spark-1.3", chat, true, 10).getString("reasoning_effort"))
        assertFalse(buildChatBody("meta-models/Muse-Glimmer-30B", chat, true, 10).has("reasoning_effort"))
        assertFalse(buildChatBody("llama3.2", chat, true, 10).has("reasoning_effort"))
    }

    @Test fun `a picture body sends the prompt and the image as a data url`() {
        val body = JSONObject(buildCloudImageBody("muse-spark-1.3", "Describe it", "QUJD", 800).toString())
        assertFalse(body.getBoolean("stream"))
        val content = body.getJSONArray("messages").getJSONObject(0).getJSONArray("content")
        assertEquals("text", content.getJSONObject(0).getString("type"))
        assertEquals("Describe it", content.getJSONObject(0).getString("text"))
        assertEquals("image_url", content.getJSONObject(1).getString("type"))
        assertEquals("data:image/jpeg;base64,QUJD", content.getJSONObject(1).getJSONObject("image_url").getString("url"))
    }

    // ---- replies and errors ----

    @Test fun `a whole reply is read and trimmed`() {
        assertEquals("A half.", parseCloudReply("""{"choices":[{"message":{"role":"assistant","content":"  A half.\n"}}]}"""))
    }

    @Test fun `a reply with null or no content is empty`() {
        assertEquals("", parseCloudReply("""{"choices":[{"message":{"content":null}}]}"""))
        assertEquals("", parseCloudReply("""{"choices":[]}"""))
    }

    @Test fun `an error body as a reply throws`() {
        try {
            parseCloudReply("""{"error":{"message":"bad"}}""")
            throw AssertionError("should have thrown")
        } catch (e: CloudAiStreamException) {
            assertEquals("bad", e.detail)
        }
    }

    private val billing = """{"error":{"code":"billing_not_configured","message":"Billing verification failed.","param":null,"type":"billing_error"}}"""

    @Test fun `the billing error from Meta is read and counts as unusable`() {
        val e = parseCloudError(402, billing)
        assertEquals("billing_not_configured", e.code)
        assertEquals("billing_error", e.type)
        assertEquals("Billing verification failed.", e.detail)
        assertTrue(isCloudUnusable(e))
    }

    @Test fun `billing is unusable whatever the status`() {
        assertTrue(isCloudUnusable(parseCloudError(400, billing)))
    }

    @Test fun `a wrong key is unusable`() {
        assertTrue(isCloudUnusable(parseCloudError(401, """{"error":{"message":"bad key"}}""")))
    }

    @Test fun `a busy server or a bad request is not unusable`() {
        assertFalse(isCloudUnusable(parseCloudError(429, """{"error":{"message":"slow down"}}""")))
        assertFalse(isCloudUnusable(parseCloudError(503, null)))
        assertFalse(isCloudUnusable(parseCloudError(400, """{"error":{"message":"context too long"}}""")))
        assertFalse(isCloudUnusable(java.net.SocketTimeoutException("timeout")))
    }

    @Test fun `a body that is not json still gives an error`() {
        val e = parseCloudError(502, "<html>Bad gateway</html>")
        assertEquals(502, e.status)
        assertNull(e.code)
        assertEquals("<html>Bad gateway</html>", e.detail)
    }
}
