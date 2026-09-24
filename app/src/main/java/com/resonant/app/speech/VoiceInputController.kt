package com.resonant.app.speech

import android.content.Context
import com.resonant.app.speech.whisper.WhisperModelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
 * Downloading the model is NOT automatic — expose [modelState] and
 * [downloadOfflineModel] from a Settings screen (or similar) so the person
 * decides when to pull ~148MB, rather than it happening silently.
 */
class VoiceInputController(context: Context) {

    private val appContext = context.applicationContext

    private val whisper = WhisperSpeechInputManager(appContext)
    private val fallback = SpeechInputManager(appContext)
    private val modelManager = WhisperModelManager(appContext)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val modelState get() = modelManager.state

    fun isAvailable(): Boolean = whisper.isAvailable() || fallback.isAvailable()

    fun startListening(onOutcome: (SpeechInputManager.Outcome) -> Unit) {
        if (whisper.isAvailable()) {
            whisper.startListening(onOutcome)
        } else {
            fallback.startListening(onOutcome)
        }
    }

    fun stopListening() {
        whisper.stopListening()
        fallback.stopListening()
    }

    /** Call once from an explicit user action — see the class doc for why this isn't automatic. */
    fun downloadOfflineModel() {
        scope.launch { modelManager.ensureDownloaded() }
    }

    fun release() {
        whisper.release()
        fallback.stopListening()
        // Deliberately NOT cancelling `scope` here: if a download is in flight
        // (once something actually calls downloadOfflineModel — see the class
        // doc, that's not wired to any UI yet) it should keep running even if
        // the screen that happened to trigger it goes away, the same way any
        // background download would. This does mean the scope outlives this
        // object with nothing else to hold onto it — fine for one lightweight
        // SupervisorJob, but worth revisiting once there's a real call site
        // (a Settings toggle is the natural one) with its own lifecycle to
        // anchor this to instead.
    }
}
