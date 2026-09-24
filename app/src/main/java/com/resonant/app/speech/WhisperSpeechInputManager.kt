package com.resonant.app.speech

import android.content.Context
import com.resonant.app.speech.whisper.WhisperEngine
import com.resonant.app.speech.whisper.WhisperModelManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Voice capture via a local Whisper model instead of whatever speech engine
 * (if any) the device happens to ship with: fully offline, same behavior on
 * every device, and nothing leaves the phone.
 *
 * Matches SpeechInputManager's shape on purpose (isAvailable / startListening
 * / stopListening) so a call site can swap between them, or — more usefully
 * — go through VoiceInputController, which picks automatically and falls
 * back to SpeechInputManager while the model isn't downloaded yet.
 *
 * Requires the model to already be present; this class never triggers a
 * download itself (see WhisperModelManager for why).
 */
class WhisperSpeechInputManager(context: Context) {

    private val appContext = context.applicationContext
    private val modelManager = WhisperModelManager(appContext)
    private var engine: WhisperEngine? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var activeJob: Job? = null

    fun isAvailable(): Boolean = modelManager.isModelPresent()

    fun startListening(onOutcome: (SpeechInputManager.Outcome) -> Unit) {
        stopListening()

        if (!modelManager.isModelPresent()) {
            onOutcome(SpeechInputManager.Outcome.Error("The offline voice model isn't downloaded yet."))
            return
        }

        activeJob = scope.launch {
            try {
                val recording = withContext(Dispatchers.IO) { AudioRecorder().record() }
                if (recording.samples.isEmpty()) {
                    deliver(onOutcome, SpeechInputManager.Outcome.Error("I didn't catch that."))
                    return@launch
                }

                val activeEngine = engine ?: WhisperEngine(modelManager.modelPath()).also { engine = it }
                val text = activeEngine.transcribe(recording.samples, recording.sampleRate)

                val outcome = if (text.isBlank()) {
                    SpeechInputManager.Outcome.Error("I didn't catch that.")
                } else {
                    SpeechInputManager.Outcome.Success(text)
                }
                deliver(onOutcome, outcome)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                deliver(onOutcome, SpeechInputManager.Outcome.Error("Offline voice recognition failed."))
            }
        }
    }

    fun stopListening() {
        activeJob?.cancel()
        activeJob = null
    }

    /** Releases the loaded model. Call when the owning screen is done with voice input for good. */
    fun release() {
        stopListening()
        val toRelease = engine
        engine = null
        if (toRelease != null) scope.launch { toRelease.release() }
    }

    private suspend fun deliver(onOutcome: (SpeechInputManager.Outcome) -> Unit, outcome: SpeechInputManager.Outcome) {
        withContext(Dispatchers.Main) { onOutcome(outcome) }
    }
}
