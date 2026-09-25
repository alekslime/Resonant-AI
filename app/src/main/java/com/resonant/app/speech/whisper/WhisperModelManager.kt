package com.resonant.app.speech.whisper

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads and stores the on-device Whisper model (English-only "base",
 * ~148MB — see the product decision this came from: bigger than "tiny" for
 * better accuracy, downloaded on first use rather than bundled so install
 * size stays small).
 *
 * Deliberately HttpURLConnection + no new library, matching OllamaClient's
 * pattern elsewhere in this app.
 *
 * This class never triggers a download on its own — a ~148MB pull shouldn't
 * start silently, especially on cellular. Something explicit (a Settings
 * toggle, an onboarding step) should call [ensureDownloaded].
 *
 * Safe to construct more than once (e.g. one instance per screen that needs
 * it) — [downloadMutex] is shared across every instance in the process, so
 * two instances calling [ensureDownloaded] around the same time serialize
 * onto one real download instead of both writing to the same temp file.
 */
class WhisperModelManager(context: Context) {

    private val appContext = context.applicationContext

    private val modelFile: File
        get() = File(File(appContext.filesDir, "models"), MODEL_FILENAME)

    sealed class State {
        data object NotDownloaded : State()
        data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : State()
        data object Ready : State()
        data class Failed(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(
        if (isModelPresent()) State.Ready else State.NotDownloaded
    )
    val state: StateFlow<State> = _state.asStateFlow()

    fun isModelPresent(): Boolean = modelFile.exists() && modelFile.length() >= MIN_EXPECTED_BYTES

    fun modelPath(): String = modelFile.absolutePath

    /** Idempotent — safe to call again after a failed or interrupted attempt, or concurrently from another instance. */
    suspend fun ensureDownloaded() {
        if (isModelPresent()) {
            _state.value = State.Ready
            return
        }
        withContext(Dispatchers.IO) {
            downloadMutex.withLock {
                // Re-check: another instance may have finished while we were waiting for the lock.
                if (isModelPresent()) {
                    _state.value = State.Ready
                    return@withLock
                }
                try {
                    download()
                    _state.value = State.Ready
                } catch (e: IOException) {
                    _state.value = State.Failed(e.message ?: "Download failed.")
                }
            }
        }
    }

    private fun download() {
        val modelsDir = File(appContext.filesDir, "models")
        modelsDir.mkdirs()
        val tmp = File(modelsDir, "$MODEL_FILENAME.part")

        val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
        }
        try {
            val status = connection.responseCode
            if (status != HttpURLConnection.HTTP_OK) {
                throw IOException("Model server returned HTTP $status")
            }

            val total = connection.contentLengthLong
            var downloaded = 0L
            // Emitted state drives UI (and, indirectly, accessibility announcements) —
            // only emit on an actual percentage change, not every ~64KB chunk. On a fast
            // connection that chunk loop can fire hundreds of times a second; nothing
            // downstream needs updates anywhere near that granular.
            var lastEmittedPercent = -1
            connection.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val percent = (downloaded * 100 / total).toInt()
                            if (percent != lastEmittedPercent) {
                                lastEmittedPercent = percent
                                _state.value = State.Downloading(downloaded, total)
                            }
                        } else {
                            // No Content-Length from the server — can't compute a percentage,
                            // so just report raw bytes as they come (rare fallback case).
                            _state.value = State.Downloading(downloaded, total)
                        }
                    }
                }
            }

            if (total > 0 && downloaded < total) {
                throw IOException("Download incomplete ($downloaded of $total bytes).")
            }
            if (!tmp.renameTo(modelFile)) {
                throw IOException("Couldn't save the downloaded model.")
            }
        } finally {
            connection.disconnect()
            tmp.delete() // no-op once renamed; cleans up a half-written file on failure
        }
    }

    companion object {
        private const val MODEL_FILENAME = "ggml-base.en.bin"

        // Official whisper.cpp model distribution (ggerganov/whisper.cpp on Hugging
        // Face) — the same source whisper.cpp's own download-ggml-model.sh script uses.
        private const val MODEL_URL =
            "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/$MODEL_FILENAME"

        // The real file is ~148MB. Anything drastically smaller means a previous
        // download died partway and shouldn't be mistaken for a good model.
        private const val MIN_EXPECTED_BYTES = 100L * 1024 * 1024

        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000 // per-read stall timeout, not total transfer time

        // Process-wide on purpose: guards the shared temp-file path against two
        // independent WhisperModelManager instances (one per screen, say) both
        // downloading at once.
        private val downloadMutex = Mutex()
    }
}

