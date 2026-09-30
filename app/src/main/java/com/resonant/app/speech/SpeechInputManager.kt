package com.resonant.app.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * One-shot voice capture using Android's built-in SpeechRecognizer — no server,
 * no extra dependency. Quality depends on the device's installed recognizer
 * (usually Google's), which is the fastest thing to wire up but is a swap
 * point later if a self-hosted Whisper server is ever wanted instead.
 *
 * Must be driven from the main thread (SpeechRecognizer requires it), which is
 * naturally satisfied when called from Compose gesture callbacks.
 */
class SpeechInputManager(context: Context) {

    sealed class Outcome {
        data class Success(val text: String) : Outcome()
        data class Error(val message: String) : Outcome()
    }

    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null

    /** Live mic loudness 0..1 while the recognizer is listening, 0 otherwise. */
    @Volatile var micLevel = 0f
        private set

    private val _transcribing = kotlinx.coroutines.flow.MutableStateFlow(false)
    /** True from the end of speech until the recognizer returns a result. */
    val transcribing: kotlinx.coroutines.flow.StateFlow<Boolean> = _transcribing

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun startListening(onOutcome: (Outcome) -> Unit) {
        stopListening()

        if (!isAvailable()) {
            onOutcome(Outcome.Error("No speech recognizer is available on this device."))
            return
        }

        val r = SpeechRecognizer.createSpeechRecognizer(appContext)
        recognizer = r

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        r.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                micLevel = 0f
                _transcribing.value = false
                val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (text.isNullOrBlank()) {
                    onOutcome(Outcome.Error("I didn't catch that."))
                } else {
                    onOutcome(Outcome.Success(text))
                }
            }

            override fun onError(error: Int) {
                micLevel = 0f
                _transcribing.value = false
                onOutcome(Outcome.Error(errorMessage(error)))
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) { micLevel = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { micLevel = 0f; _transcribing.value = true }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        r.startListening(intent)
    }

    fun stopListening() {
        micLevel = 0f
        _transcribing.value = false
        recognizer?.destroy()
        recognizer = null
    }

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't catch that."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Speech recognition needs a network connection."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy."
        else -> "Speech recognition failed."
    }
}
