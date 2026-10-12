package com.resonant.app.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/** One turn in the conversation sent to Ollama. role is "system"/"user"/"assistant". */
data class ChatMessage(val role: String, val content: String)

/** One decoded line of Ollama's streamed /api/chat response. */
internal data class StreamChunk(val delta: String, val done: Boolean)

/** The server answered, but not with 200. [detail] is Ollama's own error body: for logs, never for speech. */
internal class OllamaHttpException(val status: Int, val detail: String?) :
    IOException("Ollama returned HTTP $status${detail?.let { ": $it" } ?: ""}")

/** Ollama reported a failure in the middle of a streamed reply (`{"error": "..."}`). */
internal class OllamaStreamException(val detail: String) : IOException("Ollama error: $detail")

/**
 * Decodes one newline-delimited-JSON line from a streamed /api/chat response.
 * Returns null for a blank line; throws [IOException] if the server reported an
 * error mid-stream (Ollama does this as `{"error": "..."}`).
 */
internal fun parseStreamLine(line: String): StreamChunk? {
    if (line.isBlank()) return null
    val obj = JSONObject(line)
    val error = obj.optString("error")
    if (error.isNotEmpty()) throw OllamaStreamException(error)
    val delta = obj.optJSONObject("message")?.optString("content").orEmpty()
    return StreamChunk(delta, obj.optBoolean("done", false))
}

/** The body of a one-shot /api/chat request that asks [model] about one picture. Pure. */
internal fun buildImageRequest(model: String, prompt: String, jpeg: ByteArray): JSONObject {
    val image = java.util.Base64.getEncoder().encodeToString(jpeg)
    return buildImageRequest(model, prompt, image)
}

internal fun buildImageRequest(model: String, prompt: String, base64Image: String): JSONObject =
    JSONObject()
        .put("model", model)
        .put("stream", false)
        .put("keep_alive", OllamaClient.KEEP_ALIVE)
        .put(
            "messages",
            JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put("content", prompt)
                    .put("images", JSONArray().put(base64Image))
            )
        )

/** The text of a non-streamed /api/chat reply; throws [OllamaStreamException] if the server reported an error. Pure. */
internal fun parseChatReply(body: String): String {
    val obj = JSONObject(body)
    val error = obj.optString("error")
    if (error.isNotEmpty()) throw OllamaStreamException(error)
    return obj.optJSONObject("message")?.optString("content").orEmpty().trim()
}

/**
 * Talks to a local Ollama server's /api/chat endpoint. Deliberately just
 * HttpURLConnection + org.json — both already on the Android platform, so this
 * adds zero new Gradle dependencies.
 *
 * Streams the reply so the app can start speaking the first sentence while the
 * model is still writing the rest, instead of sitting silent until the whole
 * answer exists.
 */
object OllamaClient {

    private const val CONNECT_TIMEOUT_MS = 8_000

    /**
     * Applies between chunks, not to the whole reply. Generous because the very
     * first chunk waits on the model loading into memory, which on a cold server
     * can take far longer than the gaps between tokens ever will.
     */
    private const val READ_TIMEOUT_MS = 90_000

    /** Ask the server to keep the model loaded, so only the first question pays the load cost. */
    internal const val KEEP_ALIVE = "10m"

    /**
     * Emits the reply as it is generated, as small text deltas (typically a token or
     * a few). Collecting throws on connection failure, a non-200 status, or an error
     * reported mid-stream. Cancelling the collector closes the connection, which also
     * unblocks the read — a cancelled question does not keep the server generating.
     */
    fun chatStream(history: List<ChatMessage>): Flow<String> = callbackFlow {
        val connection = (URL("${OllamaConfig.BASE_URL}/api/chat").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Content-Type", "application/json")
        }

        val producer = launch(Dispatchers.IO) {
            try {
                val messagesJson = JSONArray().apply {
                    history.forEach { msg ->
                        put(JSONObject().put("role", msg.role).put("content", msg.content))
                    }
                }
                val body = JSONObject()
                    .put("model", OllamaConfig.MODEL)
                    .put("messages", messagesJson)
                    .put("stream", true)
                    .put("keep_alive", KEEP_ALIVE)

                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }

                val status = connection.responseCode
                if (status != HttpURLConnection.HTTP_OK) {
                    val errText = connection.errorStream?.let { readAll(it) }
                    throw OllamaHttpException(status, errText)
                }

                BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                    while (isActive) {
                        val line = reader.readLine() ?: break
                        val chunk = parseStreamLine(line) ?: continue
                        if (chunk.delta.isNotEmpty()) this@callbackFlow.send(chunk.delta)
                        if (chunk.done) break
                    }
                }
                this@callbackFlow.close()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                this@callbackFlow.close(e)
            }
        }

        awaitClose {
            producer.cancel()
            connection.disconnect()
        }
    }

    /** A picture is described by a vision model, which can be slow on the first call while it loads. */
    private const val VISION_READ_TIMEOUT_MS = 240_000

    /**
     * Asks the vision model ([OllamaConfig.VISION_MODEL]) about one picture and waits for the whole
     * answer. Blocks: call off the main thread. Null when the server cannot be reached, does not
     * have a model that reads pictures, or answers with nothing: the caller falls back to the text
     * recognised on the phone.
     */
    fun describeImage(jpeg: ByteArray, prompt: String): String? = try {
        val connection = (URL("${OllamaConfig.BASE_URL}/api/chat").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = VISION_READ_TIMEOUT_MS
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            val body = buildImageRequest(OllamaConfig.VISION_MODEL, prompt, jpeg)
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                null
            } else {
                parseChatReply(connection.inputStream.let { readAll(it) }).takeIf { it.isNotEmpty() }
            }
        } finally {
            connection.disconnect()
        }
    } catch (e: Exception) {
        null
    }

    private fun readAll(stream: java.io.InputStream): String =
        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
}
