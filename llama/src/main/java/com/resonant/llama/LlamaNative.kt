package com.resonant.llama

/** Gets each piece of the answer as UTF-8 bytes. Return false to stop. Called on the thread that called generate. */
fun interface LlamaPieceSink {
    fun onPiece(bytes: ByteArray): Boolean
}

/**
 * Raw JNI bridge to llama.cpp: see src/main/cpp/llama_jni.cpp. Use [LlamaEngine], not this.
 * An engine is not thread-safe except [nativeCancel]: one call at a time per handle.
 */
internal object LlamaNative {
    init {
        System.loadLibrary("llama_jni")
    }

    /** An opaque handle, or 0 if the file is not a model this build can load. */
    external fun nativeLoad(path: String, nCtx: Int, nThreads: Int, nThreadsBatch: Int): Long

    external fun nativeFree(handle: Long)

    /** Forget what was read, so the next prompt starts from the beginning. */
    external fun nativeReset(handle: Long)

    /** Safe from any thread: stops the running [nativeGenerate]. */
    external fun nativeCancel(handle: Long)

    external fun nativeSystemInfo(): String

    /** [prompt tokens, tokens that did not need reading again] for the last [nativeGenerate]. */
    external fun nativeLastPromptStats(handle: Long): IntArray

    /** Tokens written (>= 0), or a negative code (see [LlamaEngine]). */
    external fun nativeGenerate(
        handle: Long,
        roles: Array<String>,
        contents: Array<ByteArray>, // UTF-8
        maxTokens: Int,
        temperature: Float,
        sink: LlamaPieceSink
    ): Int
}
