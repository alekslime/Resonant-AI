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
 *
 * Call [preload] as early as possible (see WhisperSpeechInputManager.preload)
 * so the model is already loaded by the time [transcribe] actually needs it —
 * without that, the very first transcription silently eats however long
 * loading a ~150MB model takes, with nothing telling the person why.
 */
class WhisperEngine(private val modelPath: String) {

    private val dispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, "whisper-native").apply { isDaemon = true }
    }.asCoroutineDispatcher()

    @Volatile private var handle: Long = 0L

    /**
     * Loads the model now if it isn't already, without transcribing anything.
     * Returns whether it's loaded (true even if it already was). Safe to call
     * concurrently with [transcribe] — both run on the same single dispatcher,
     * so whichever gets there first does the actual loading and the other
     * just finds `handle` already set.
     */
    suspend fun preload(): Boolean = withContext(dispatcher) {
        ensureLoaded()
        handle != 0L
    }

    suspend fun transcribe(pcm: FloatArray, sampleRate: Int = 16_000): String =
        withContext(dispatcher) {
            ensureLoaded()
            check(handle != 0L) { "Whisper model failed to load ($modelPath)." }
            WhisperNative.nativeTranscribe(handle, pcm, sampleRate).trim()
        }

    // Only ever called on `dispatcher`'s single thread — from preload() or
    // transcribe() — so this plain if-check is all the guarding it needs.
    private fun ensureLoaded() {
        if (handle == 0L) {
            handle = WhisperNative.nativeInit(modelPath)
        }
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

