package com.resonant.app.speech.whisper

/**
 * Raw JNI bridge to whisper.cpp — see app/src/main/cpp/whisper_jni.cpp for the
 * native side and app/src/main/cpp/CMakeLists.txt for what has to be vendored
 * before this builds at all.
 *
 * whisper.cpp is NOT thread-safe: every call for a given handle must happen
 * on the same thread. This object doesn't enforce that itself — WhisperEngine
 * does, by confining all use to one dedicated thread. Don't call this object
 * directly from anywhere else.
 */
internal object WhisperNative {
    init {
        System.loadLibrary("whisper_jni")
    }

    /** Returns an opaque context handle, or 0 if the model failed to load. */
    external fun nativeInit(modelPath: String): Long

    /** [handle] must be a value previously returned by [nativeInit] (and non-zero). */
    external fun nativeTranscribe(handle: Long, pcm: FloatArray, sampleRate: Int): String

    external fun nativeFree(handle: Long)
}
