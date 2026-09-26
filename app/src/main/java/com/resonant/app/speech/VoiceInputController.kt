package com.resonant.app.speech

import android.content.Context

/**
 * Picks Whisper (offline, on-device, same behavior everywhere) when its
 * model is downloaded, and falls back to the platform SpeechRecognizer
 * (SpeechInputManager) otherwise — so voice input works from the very first
 * launch, and gets more private and more consistent once the model finishes
 * downloading.
 *
 * Drop-in for SpeechInputManager at a call site: same isAvailable() /
 * startListening(onOutcome) / stopListening() shape, so swapping
 * `SpeechInputManager(context)` for `VoiceInputController(context)` is the
 * only change ChatScreen needs.
 *
 * Downloading the model is NOT automatic — SettingsScreen owns that via its
 * own WhisperModelManager instance and scope, so the person decides when to
 * pull ~148MB rather than it happening silently.
 */
class VoiceInputController(context: Context) {

    private val appContext = context.applicationContext

    private val whisper = WhisperSpeechInputManager(appContext)
    private val fallback = SpeechInputManager(appContext)

    val whisperLoadState get() = whisper.loadState

    init {
        // Start warming up the model the moment a screen that might need voice
        // input exists — not lazily on first tap. A no-op if the model isn't
        // downloaded yet. See WhisperSpeechInputManager.preload() for why this
        // matters: without it, the first real transcription silently eats
        // however long loading a ~150MB model takes.
        whisper.preload()
    }

    fun isAvailable(): Boolean = whisper.isAvailable() || fallback.isAvailable()

    /**
     * [onStatus] only ever fires on the Whisper path, and only when someone
     * starts listening before the background preload from init{} has finished
     * — see WhisperSpeechInputManager.startListening for the detail, including
     * why the caller MUST wait for onSpoken() before treating the mic as open.
     */
    fun startListening(
        onOutcome: (SpeechInputManager.Outcome) -> Unit,
        onStatus: (String, onSpoken: () -> Unit) -> Unit = { _, onSpoken -> onSpoken() }
    ) {
        if (whisper.isAvailable()) {
            whisper.startListening(onOutcome, onStatus)
        } else {
            fallback.startListening(onOutcome)
        }
    }

    fun stopListening() {
        whisper.stopListening()
        fallback.stopListening()
    }

    fun release() {
        whisper.release()
        fallback.stopListening()
    }
}
