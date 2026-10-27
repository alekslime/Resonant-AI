package com.resonant.app.phoneai

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads and keeps the language model that answers Chat on the phone alone
 * (Qwen2.5 1.5B Instruct, 4-bit, from the Qwen team's own Hugging Face repo, Apache 2.0).
 *
 * One per process ([get]): the Settings row and Chat must agree on whether it is there.
 * Like [com.resonant.app.speech.whisper.WhisperModelManager] it never downloads by itself; Settings
 * calls [ensureDownloaded]. Unlike it, a dropped connection can carry on where it stopped: the
 * half-finished file is kept and asked for again from its end.
 */
class PhoneAiModelManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val modelsDir get() = File(appContext.filesDir, "models")
    private val modelFile get() = File(modelsDir, MODEL_FILENAME)
    private val partFile get() = File(modelsDir, "$MODEL_FILENAME.part")

    sealed class State {
        data object NotDownloaded : State()
        data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : State()
        data object Ready : State()
        data class Failed(val message: String) : State()
        /** This phone cannot run it. [spoken] says why. Not retryable. */
        data class Unsupported(val spoken: String) : State()
    }

    private val verdict = PhoneAiSupport.check(Build.SUPPORTED_ABIS.toList(), readCpuInfo(), totalRam())

    private val _state = MutableStateFlow<State>(
        when {
            verdict is PhoneAiSupport.Verdict.No -> State.Unsupported(verdict.spoken)
            isModelPresent() -> State.Ready
            else -> State.NotDownloaded
        }
    )
    val state: StateFlow<State> = _state.asStateFlow()

    fun isModelPresent(): Boolean = modelFile.exists() && modelFile.length() >= MIN_EXPECTED_BYTES

    fun modelPath(): String = modelFile.absolutePath

    /** Safe to call again after a failure or from two places at once. */
    suspend fun ensureDownloaded() {
        if (_state.value is State.Unsupported) return
        if (isModelPresent()) {
            _state.value = State.Ready
            return
        }
        withContext(Dispatchers.IO) {
            downloadMutex.withLock {
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

    /** Frees the space. Chat answers from the lessons again until it is downloaded. */
    fun delete() {
        modelFile.delete()
        partFile.delete()
        if (_state.value !is State.Unsupported) _state.value = State.NotDownloaded
    }

    private fun download() {
        modelsDir.mkdirs()
        val part = partFile
        val alreadyHave = if (part.exists()) part.length() else 0L

        val needed = REQUIRED_FREE_BYTES - alreadyHave
        if (modelsDir.usableSpace < needed) {
            throw IOException("Not enough free storage. About 1.5 gigabytes are needed.")
        }

        val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            if (alreadyHave > 0) setRequestProperty("Range", "bytes=$alreadyHave-")
        }
        try {
            val status = connection.responseCode
            val plan = PhoneAiSupport.resumePlan(status, alreadyHave, connection.contentLengthLong)
                ?: throw IOException("Model server returned HTTP $status")

            var downloaded = if (plan.append) alreadyHave else 0L
            val total = plan.totalBytes
            var lastPercent = -1
            connection.inputStream.use { input ->
                RandomAccessFile(part, "rw").use { out ->
                    if (plan.append) out.seek(alreadyHave) else out.setLength(0)
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        out.write(buffer, 0, read)
                        downloaded += read
                        val percent = if (total > 0) (downloaded * 100 / total).toInt() else -1
                        if (percent != lastPercent) {
                            lastPercent = percent
                            _state.value = State.Downloading(downloaded, total)
                        }
                    }
                }
            }

            if (total > 0 && downloaded < total) {
                // Kept on purpose: the next try carries on from here.
                throw IOException("Download interrupted. Tap again to carry on.")
            }
            if (!looksLikeModel(part)) {
                part.delete()
                throw IOException("The downloaded file is not a model. Tap to try again.")
            }
            if (!part.renameTo(modelFile)) throw IOException("Couldn't save the downloaded model.")
        } finally {
            connection.disconnect()
        }
    }

    /** Every .gguf model file starts with the four letters GGUF. */
    private fun looksLikeModel(file: File): Boolean = try {
        file.inputStream().use { input ->
            val head = ByteArray(4)
            input.read(head) == 4 && String(head, Charsets.US_ASCII) == "GGUF"
        }
    } catch (e: IOException) {
        false
    }

    private fun readCpuInfo(): String = try {
        File("/proc/cpuinfo").readText()
    } catch (e: Exception) {
        "" // unreadable: the check lets it through rather than refusing a phone that would have worked
    }

    private fun totalRam(): Long {
        val manager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 0L
        return ActivityManager.MemoryInfo().also { manager.getMemoryInfo(it) }.totalMem
    }

    companion object {
        private const val MODEL_FILENAME = "qwen2.5-1.5b-instruct-q4_k_m.gguf"
        private const val MODEL_URL =
            "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/$MODEL_FILENAME"

        // A real model is about a gigabyte. This only turns away a stub or an error page.
        private const val MIN_EXPECTED_BYTES = 100L * 1024 * 1024

        // The file plus room for the phone to breathe.
        private const val REQUIRED_FREE_BYTES = 1_500_000_000L

        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000

        private val downloadMutex = Mutex()

        @Volatile private var instance: PhoneAiModelManager? = null

        /** The state, for code with no Context to hand. NotDownloaded until the first [get]. */
        val currentState: State get() = instance?.state?.value ?: State.NotDownloaded

        fun get(context: Context): PhoneAiModelManager =
            instance ?: synchronized(this) {
                instance ?: PhoneAiModelManager(context).also { instance = it }
            }
    }
}
