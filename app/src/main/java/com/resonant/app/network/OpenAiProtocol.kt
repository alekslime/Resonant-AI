package com.resonant.app.network

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * The OpenAI-style chat protocol that the Meta Model API (api.meta.ai/v1), Baseten, vLLM and most
 * other servers speak. Pure and tested on the JVM; the network part is OpenAiClient.kt.
 */

/** One line of a streamed reply (server-sent events). */
internal sealed class SseEvent {
    data class Text(val text: String) : SseEvent()
    data object Done : SseEvent()
    data object Skip : SseEvent()
}

/** The service answered, but not with 200. [code] and [type] come from its error body when it has one. */
internal class CloudAiHttpException(
    val status: Int,
    val code: String?,
    val type: String?,
    val detail: String?
) : IOException("AI service returned HTTP $status${code?.let { " ($it)" } ?: ""}")

/** The service reported a failure in the middle of a streamed reply. */
internal class CloudAiStreamException(val detail: String) : IOException("AI service error: $detail")

/** A string field, or null when it is missing, JSON null or empty. org.json's optString turns a null into the text "null". */
private fun JSONObject.text(key: String): String? =
    if (isNull(key)) null else optString(key).ifEmpty { null }

/**
 * Decodes one line of a streamed /chat/completions reply. Returns [SseEvent.Skip] for blank lines,
 * comments and chunks with no text (the first chunk only names the role, the last only the finish reason).
 * Throws [CloudAiStreamException] when the server reports an error mid-stream.
 */
internal fun parseSseLine(line: String): SseEvent {
    val trimmed = line.trim()
    if (!trimmed.startsWith("data:")) return SseEvent.Skip
    val payload = trimmed.removePrefix("data:").trim()
    if (payload.isEmpty()) return SseEvent.Skip
    if (payload == "[DONE]") return SseEvent.Done

    val obj = JSONObject(payload)
    obj.optJSONObject("error")?.let { throw CloudAiStreamException(it.text("message") ?: "unknown error") }

    // Only "content" is read. A reasoning model may also send its thinking in another field: that is never spoken.
    val delta = obj.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta") ?: return SseEvent.Skip
    val text = delta.text("content") ?: return SseEvent.Skip
    return SseEvent.Text(text)
}

private fun JSONObject.withModelOptions(model: String): JSONObject {
    // Muse Spark always reasons: "low" keeps a spoken answer quick. Other servers may not know the field.
    if (model.startsWith("muse-")) put("reasoning_effort", "low")
    return this
}

/** The body of a /chat/completions request. */
internal fun buildChatBody(model: String, messages: List<ChatMessage>, stream: Boolean, maxTokens: Int): JSONObject =
    JSONObject()
        .put("model", model)
        .put("stream", stream)
        .put("max_completion_tokens", maxTokens)
        .put(
            "messages",
            JSONArray().apply {
                messages.forEach { put(JSONObject().put("role", it.role).put("content", it.content)) }
            }
        )
        .withModelOptions(model)

/** The body of a one-shot request that asks [model] about one JPEG picture. */
internal fun buildCloudImageBody(model: String, prompt: String, base64Jpeg: String, maxTokens: Int): JSONObject =
    JSONObject()
        .put("model", model)
        .put("stream", false)
        .put("max_completion_tokens", maxTokens)
        .put(
            "messages",
            JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put(
                        "content",
                        JSONArray()
                            .put(JSONObject().put("type", "text").put("text", prompt))
                            .put(
                                JSONObject()
                                    .put("type", "image_url")
                                    .put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$base64Jpeg"))
                            )
                    )
            )
        )
        .withModelOptions(model)

/** The text of a non-streamed reply; throws [CloudAiStreamException] if the body is an error. */
internal fun parseCloudReply(body: String): String {
    val obj = JSONObject(body)
    obj.optJSONObject("error")?.let { throw CloudAiStreamException(it.text("message") ?: "unknown error") }
    val message = obj.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
    return (message?.text("content") ?: "").trim()
}

/** Reads the error envelope ({"error": {"code", "message", "type"}}) from a non-200 body. Never throws. */
internal fun parseCloudError(status: Int, body: String?): CloudAiHttpException {
    var code: String? = null
    var type: String? = null
    var message: String? = null
    try {
        val error = body?.let { JSONObject(it).optJSONObject("error") }
        code = error?.text("code")
        type = error?.text("type")
        message = error?.text("message")
    } catch (e: Exception) {
        message = body?.take(200)
    }
    return CloudAiHttpException(status, code, type, message)
}

/**
 * True when no later question will do better either: the key is wrong, or billing is not set up.
 * (A busy server, a timeout or a bad request is not that: the next question may work.)
 */
internal fun isCloudUnusable(e: Throwable): Boolean =
    e is CloudAiHttpException &&
        (e.status == 401 || e.status == 402 || e.status == 403 ||
            e.code == "billing_not_configured" || e.type == "billing_error" || e.code == "invalid_api_key")
