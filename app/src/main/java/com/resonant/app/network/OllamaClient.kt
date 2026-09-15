package com.resonant.app.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/** One turn in the conversation sent to Ollama. role is "system"/"user"/"assistant". */
data class ChatMessage(val role: String, val content: String)

/**
 * Talks to a local Ollama server's /api/chat endpoint. Deliberately just
 * HttpURLConnection + org.json — both already on the Android platform, so this
 * adds zero new Gradle dependencies for one API call.
 */
object OllamaClient {

    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 30_000

    suspend fun chat(history: List<ChatMessage>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${OllamaConfig.BASE_URL}/api/chat")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json")
            }

            val messagesJson = JSONArray().apply {
                history.forEach { msg ->
                    put(JSONObject().put("role", msg.role).put("content", msg.content))
                }
            }
            val body = JSONObject()
                .put("model", OllamaConfig.MODEL)
                .put("messages", messagesJson)
                .put("stream", false)

            OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }

            val status = connection.responseCode
            if (status != HttpURLConnection.HTTP_OK) {
                val errText = connection.errorStream?.let { readStream(it) }
                connection.disconnect()
                return@withContext Result.failure(
                    Exception("Ollama returned HTTP $status${errText?.let { ": $it" } ?: ""}")
                )
            }

            val responseText = readStream(connection.inputStream)
            connection.disconnect()

            val reply = JSONObject(responseText)
                .getJSONObject("message")
                .getString("content")
            Result.success(reply.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readStream(stream: java.io.InputStream): String =
        BufferedReader(InputStreamReader(stream)).use { it.readText() }
}
