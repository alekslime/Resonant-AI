package com.resonant.app.speech.whisper

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Owns one whisper.cpp context and confines every native call to a single
 * dedicated thread — whisper.cpp is not thread-safe, and pinning to one
 * thread is the simplest way to guarantee that regardless of which coroutine
 * calls in. The model loads lazily on first use, not at construction, so
 * creating a WhisperEngine before the model is downloaded is harmless.
 */
class WhisperEngine(private val modelPath: String) {

    private val dispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, "whisper-native").apply { isDaemon = true }
    }.asCoroutineDispatcher()

    @Volatile private var handle: Long = 0L

    suspend fun transcribe(pcm: FloatArray, sampleRate: Int = 16_000): String =
        withContext(dispatcher) {
            if (handle == 0L) {
                handle = WhisperNative.nativeInit(modelPath)
            }
            check(handle != 0L) { "Whisper model failed to load ($modelPath)." }
            WhisperNative.nativeTranscribe(handle, pcm, sampleRate).trim()
        }

    suspend fun release() {
        withContext(dispatcher) {
            if (handle != 0L) {
                WhisperNative.nativeFree(handle)
                handle = 0L
            }
        }
    }
}
