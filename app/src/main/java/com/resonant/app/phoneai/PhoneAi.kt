package com.resonant.app.phoneai

import android.content.Context
import android.util.Log
import com.resonant.app.network.ChatMessage
import com.resonant.llama.LlamaEngine
import com.resonant.llama.LlamaException
import com.resonant.llama.LlamaTurn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/** The on-phone model could not answer. Chat falls back to the lesson text. */
class PhoneAiUnavailable(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Chat's answer without a PC: a small model running on this phone's processor, streamed the same
 * way as [com.resonant.app.network.OllamaClient.chatStream] so Chat can use either.
 *
 * The model is loaded on the first question and kept until [unload] (Chat calls it when it closes),
 * so a second question only reads what is new. Everything runs on the processor: the phone's GPU
 * is slower than its CPU for this.
 */
object PhoneAi {

    private const val TAG = "ResonantPhoneAi"

    /** Words the model can hold at once: the lesson, the recent turns and the answer. */
    const val CONTEXT_TOKENS = 3072

    /** About 150 spoken words: enough for the two to four sentences Chat asks for. */
    const val MAX_REPLY_TOKENS = 200

    /** Low, so an answer sticks to the lesson text. */
    const val TEMPERATURE = 0.4f

    private val lock = Mutex()
    @Volatile private var engine: LlamaEngine? = null

    fun isReady(context: Context): Boolean =
        PhoneAiModelManager.get(context).state.value is PhoneAiModelManager.State.Ready

    /**
     * The reply, as small pieces of text. Collecting throws [PhoneAiUnavailable] if the model is
     * missing or will not load. Cancelling the collector stops the model, even mid-prompt.
     */
    fun chatStream(context: Context, history: List<ChatMessage>): Flow<String> = callbackFlow {
        val appContext = context.applicationContext
        // Set once the answer is complete, so the cleanup below never cancels the NEXT question.
        val finished = AtomicBoolean(false)
        val job = launch(Dispatchers.IO) {
            try {
                lock.withLock {
                    val model = loadEngine(appContext)
                    generateFitting(model, history) { piece ->
                        isActive && this@callbackFlow.trySendBlocking(piece).isSuccess
                    }
                }
                finished.set(true)
                close()
            } catch (e: CancellationException) {
                throw e
            } catch (e: LlamaException) {
                // Cancelled by the person: not an error.
                finished.set(true)
                if (e.code == LlamaEngine.CANCELLED) close() else close(PhoneAiUnavailable("The on-phone AI failed (${e.code}).", e))
            } catch (e: LinkageError) {
                // No native library for this phone's processor (an x86 emulator, say).
                close(PhoneAiUnavailable("The on-phone AI is not built for this phone.", e))
            } catch (e: Exception) {
                close(e)
            }
        }
        awaitClose {
            if (!finished.get()) engine?.cancel()
            job.cancel()
        }
    }

    /** Frees the model's memory. Cancels an answer in progress. Safe to call when nothing is loaded. */
    fun unload() {
        engine?.cancel()
        CoroutineScope(Dispatchers.IO).launch {
            lock.withLock {
                engine?.close()
                engine = null
            }
        }
    }

    private fun loadEngine(context: Context): LlamaEngine {
        engine?.let { return it }
        val manager = PhoneAiModelManager.get(context)
        if (manager.state.value !is PhoneAiModelManager.State.Ready) {
            throw PhoneAiUnavailable("The on-phone AI is not downloaded.")
        }
        val cores = Runtime.getRuntime().availableProcessors()
        // The fast cores on mid-range phones are few; more threads than that mostly wait for the slow ones.
        // Worth trying 2, 3 and 4 on the device and keeping the quickest (see the log line below).
        val threads = if (cores >= 8) 4 else maxOf(2, cores / 2)
        val loaded = LlamaEngine.load(manager.modelPath(), CONTEXT_TOKENS, threads)
            ?: throw PhoneAiUnavailable("The on-phone AI would not load.")
        Log.i(TAG, "Loaded with $threads threads. ${LlamaEngine.systemInfo()}")
        engine = loaded
        return loaded
    }

    /** Fits the conversation to the model, and tries a shorter one if it still does not fit. */
    private fun generateFitting(model: LlamaEngine, history: List<ChatMessage>, onPiece: (String) -> Boolean) {
        var scale = 1.0
        while (true) {
            val turns = PhoneAiPrompt.fit(history, scale).map { LlamaTurn(it.role, it.content) }
            val started = System.nanoTime()
            var firstPieceMs = -1L
            try {
                val tokens = model.generate(turns, MAX_REPLY_TOKENS, TEMPERATURE) { piece ->
                    if (firstPieceMs < 0) firstPieceMs = (System.nanoTime() - started) / 1_000_000
                    onPiece(piece)
                }
                val totalMs = (System.nanoTime() - started) / 1_000_000
                val (prompt, reused) = model.lastPromptStats()
                // The numbers to tune with: prompt read, how much of it was already known, time to the
                // first word, and speed once it is writing.
                val writingMs = (totalMs - maxOf(firstPieceMs, 0)).coerceAtLeast(1)
                Log.i(TAG, "prompt=$prompt reused=$reused firstWord=${firstPieceMs}ms tokens=$tokens " +
                    "speed=${"%.1f".format(tokens * 1000.0 / writingMs)} tokens/s")
                return
            } catch (e: LlamaException) {
                if (e.code != LlamaEngine.PROMPT_TOO_LONG || scale <= 0.25) throw e
                scale /= 2
            }
        }
    }
}
