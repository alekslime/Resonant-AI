package com.resonant.app.speech

import android.content.Context
import com.resonant.app.speech.whisper.WhisperEngine
import com.resonant.app.speech.whisper.WhisperModelManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    sealed class LoadState {
        data object NotLoaded : LoadState()
        data object Loading : LoadState()
        data object Ready : LoadState()
        data class Failed(val message: String) : LoadState()
    }

    private val _loadState = MutableStateFlow<LoadState>(LoadState.NotLoaded)
    val loadState: StateFlow<LoadState> = _loadState.asStateFlow()

    fun isAvailable(): Boolean = modelManager.isModelPresent()

    /**
     * Warms up the model in the background, ahead of any actual voice
     * command — call this as soon as a screen with voice input opens (see
     * VoiceInputController's init block), not lazily on first use. Without
     * this, whoever taps the mic first pays the full model-load time in
     * total silence, in the middle of what should be a quick voice command.
     * Safe to call repeatedly, and a no-op if the model isn't downloaded yet.
     */
    fun preload() {
        if (!modelManager.isModelPresent()) return
        if (_loadState.value is LoadState.Ready || _loadState.value is LoadState.Loading) return
        _loadState.value = LoadState.Loading
        scope.launch {
            val activeEngine = engine ?: WhisperEngine(modelManager.modelPath()).also { engine = it }
            val loaded = try {
                activeEngine.preload()
            } catch (e: Exception) {
                false
            }
            _loadState.value = if (loaded) LoadState.Ready
                else LoadState.Failed("Couldn't load the offline voice model.")
        }
    }

    /**
     * [onStatus] fires only when the model isn't loaded yet at the moment
     * this is called (the common case: [preload] — already running since
     * this controller was created — has already finished by then, and this
     * never fires at all).
     *
     * Its signature is (message, onSpoken) rather than a plain string: the
     * caller MUST NOT start recording until onSpoken() is invoked, and must
     * only call onSpoken() once the message has actually finished being
     * spoken aloud — recording is deferred until then. Same reasoning as why
     * ChatScreen already waits for "Listening." to finish before opening the
     * mic: without that wait, Whisper would transcribe this app's own status
     * announcement as if it were the person's voice command. The default
     * here (call onSpoken immediately) is only safe for a caller with no TTS
     * to collide with in the first place.
     */
    fun startListening(
        onOutcome: (SpeechInputManager.Outcome) -> Unit,
        onStatus: (String, onSpoken: () -> Unit) -> Unit = { _, onSpoken -> onSpoken() }
    ) {
        stopListening()

        if (!modelManager.isModelPresent()) {
            onOutcome(SpeechInputManager.Outcome.Error("The offline voice model isn't downloaded yet."))
            return
        }

        fun beginRecording() {
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

        if (_loadState.value !is LoadState.Ready) {
            // Kick off (or continue) loading now, in parallel with however long
            // the status message takes to actually speak — by the time it's
            // done, loading has often finished too.
            preload()
            onStatus("Loading offline voice model, one moment.", ::beginRecording)
        } else {
            beginRecording()
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
        _loadState.value = LoadState.NotLoaded
        if (toRelease != null) scope.launch { toRelease.release() }
    }

    private suspend fun deliver(onOutcome: (SpeechInputManager.Outcome) -> Unit, outcome: SpeechInputManager.Outcome) {
        withContext(Dispatchers.Main) { onOutcome(outcome) }
    }
}

