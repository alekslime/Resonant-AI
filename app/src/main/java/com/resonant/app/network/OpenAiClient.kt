package com.resonant.app.network

import android.util.Log
import com.resonant.app.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Where the cloud model lives. Set in local.properties (git-ignored), like the Ollama address:
 *
 *     cloud.apiKey=...                          (the Meta Model API key from dev.meta.ai)
 *     cloud.baseUrl=https://api.meta.ai/v1      (default; any OpenAI-compatible server works)
 *     cloud.model=muse-spark-1.3                (default)
 *
 * The key is compiled into the APK you build for yourself. Do not hand that APK to other people.
 */
object CloudAiConfig {
    val BASE_URL: String = BuildConfig.CLOUD_AI_BASE_URL.trimEnd('/')
    val API_KEY: String = BuildConfig.CLOUD_AI_API_KEY
    val MODEL: String = BuildConfig.CLOUD_AI_MODEL

    /** Set after a wrong key or missing billing, so every later question does not wait on a service that will refuse it. Cleared when the app restarts. */
    @Volatile var disabledReason: String? = null

    val usable: Boolean get() = API_KEY.isNotBlank() && disabledReason == null
}

/** Chat and picture description over the OpenAI-style protocol (see OpenAiProtocol.kt). */
object OpenAiClient {

    private const val TAG = "ResonantCloudAi"
    private const val CONNECT_TIMEOUT_MS = 8_000

    /** Between chunks, not for the whole reply. A reasoning model can think a few seconds before its first word. */
    private const val READ_TIMEOUT_MS = 60_000
    private const val IMAGE_READ_TIMEOUT_MS = 120_000

    /** About 250 spoken words: more than the two to four sentences Chat asks for, so a long answer is not cut off. */
    private const val MAX_REPLY_TOKENS = 500
    private const val MAX_IMAGE_TOKENS = 800

    private fun open(path: String, readTimeoutMs: Int, stream: Boolean): HttpURLConnection =
        (URL("${CloudAiConfig.BASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = readTimeoutMs
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${CloudAiConfig.API_KEY}")
            if (stream) setRequestProperty("Accept", "text/event-stream")
        }

    private fun fail(e: Exception): Exception {
        if (isCloudUnusable(e)) {
            CloudAiConfig.disabledReason = e.message ?: "unusable"
            Log.w(TAG, "Cloud AI switched off until the app restarts: ${e.message}")
        }
        return e
    }

    /**
     * The reply as small pieces of text, in the same way as [OllamaClient.chatStream]. Collecting throws on
     * a connection failure, a non-200 answer, or an error reported mid-stream. Cancelling the collector
     * closes the connection.
     */
    fun chatStream(history: List<ChatMessage>): Flow<String> = callbackFlow {
        val connection = open("/chat/completions", READ_TIMEOUT_MS, stream = true)

        val producer = launch(Dispatchers.IO) {
            try {
                val body = buildChatBody(CloudAiConfig.MODEL, history, stream = true, maxTokens = MAX_REPLY_TOKENS)
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }

                val status = connection.responseCode
                if (status != HttpURLConnection.HTTP_OK) {
                    throw parseCloudError(status, connection.errorStream?.let { readAll(it) })
                }

                BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                    while (isActive) {
                        val line = reader.readLine() ?: break
                        when (val event = parseSseLine(line)) {
                            is SseEvent.Text -> this@callbackFlow.send(event.text)
                            SseEvent.Done -> break
                            SseEvent.Skip -> {}
                        }
                    }
                }
                this@callbackFlow.close()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                this@callbackFlow.close(fail(e))
            }
        }

        awaitClose {
            producer.cancel()
            connection.disconnect()
        }
    }

    /**
     * Asks the cloud model about one picture and waits for the whole answer. Blocks: call off the main
     * thread. Null when the service cannot be used or answers with nothing, so the caller can try the next one.
     */
    fun describeImage(jpeg: ByteArray, prompt: String): String? {
        if (!CloudAiConfig.usable) return null
        return try {
            val connection = open("/chat/completions", IMAGE_READ_TIMEOUT_MS, stream = false)
            try {
                val base64 = java.util.Base64.getEncoder().encodeToString(jpeg)
                val body = buildCloudImageBody(CloudAiConfig.MODEL, prompt, base64, MAX_IMAGE_TOKENS)
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
                val status = connection.responseCode
                if (status != HttpURLConnection.HTTP_OK) {
                    throw parseCloudError(status, connection.errorStream?.let { readAll(it) })
                }
                parseCloudReply(readAll(connection.inputStream)).takeIf { it.isNotEmpty() }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            fail(e)
            Log.w(TAG, "Picture description failed: ${e.message}")
            null
        }
    }

    private fun readAll(stream: InputStream): String =
        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
}
