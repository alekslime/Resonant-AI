package com.resonant.llama

import java.io.IOException

data class LlamaTurn(val role: String, val content: String)

/** What the engine could not do. [code] is one of the constants in [LlamaEngine]. */
class LlamaException(val code: Int) : IOException("llama engine error $code")

/**
 * One language model, loaded from a .gguf file, with one running conversation.
 *
 * Keeps what it has already read: asking again with the same start (same lesson text, same earlier
 * turns) only reads what is new, which is what makes the second question far quicker than the first.
 *
 * Not thread-safe: call [generate] from one thread at a time. [cancel] is the one exception.
 */
class LlamaEngine private constructor(private var handle: Long) : AutoCloseable {

    /**
     * Writes the reply to [turns], calling [onPiece] with each piece of text as it comes (always whole
     * characters). Return false from it to stop. Blocks until done. Returns the number of tokens written.
     * Throws [LlamaException]: [PROMPT_TOO_LONG] means shorten [turns] and try again; [CANCELLED] means
     * [cancel] was called while the prompt was still being read.
     */
    fun generate(
        turns: List<LlamaTurn>,
        maxTokens: Int,
        temperature: Float,
        onPiece: (String) -> Boolean
    ): Int {
        check(handle != 0L) { "engine is closed" }
        val sink = LlamaPieceSink { bytes -> onPiece(String(bytes, Charsets.UTF_8)) }
        val result = LlamaNative.nativeGenerate(
            handle,
            Array(turns.size) { turns[it].role },
            // As UTF-8 bytes, not Strings: JNI's own string conversion mangles emoji and other rare symbols.
            Array(turns.size) { turns[it].content.toByteArray(Charsets.UTF_8) },
            maxTokens,
            temperature,
            sink
        )
        if (result < 0) throw LlamaException(result)
        return result
    }

    /** From any thread. The running [generate] stops as soon as it can. */
    @Synchronized
    fun cancel() {
        if (handle != 0L) LlamaNative.nativeCancel(handle)
    }

    fun reset() {
        if (handle != 0L) LlamaNative.nativeReset(handle)
    }

    /** Prompt tokens and how many of them were already read, for the last [generate]. For the log. */
    fun lastPromptStats(): Pair<Int, Int> {
        if (handle == 0L) return 0 to 0
        val s = LlamaNative.nativeLastPromptStats(handle)
        return s[0] to s[1]
    }

    @Synchronized
    override fun close() {
        if (handle != 0L) {
            LlamaNative.nativeFree(handle)
            handle = 0L
        }
    }

    companion object {
        const val PROMPT_TOO_LONG = -2
        const val DECODE_FAILED = -3
        const val CANCELLED = -7

        /** Null if the file is missing, damaged, or the phone has no room for it. */
        fun load(path: String, contextTokens: Int, threads: Int, batchThreads: Int = threads): LlamaEngine? {
            val handle = LlamaNative.nativeLoad(path, contextTokens, threads, batchThreads)
            return if (handle == 0L) null else LlamaEngine(handle)
        }

        /** What the CPU backend found (NEON, dot product...), for the log. */
        fun systemInfo(): String = LlamaNative.nativeSystemInfo()
    }
}
