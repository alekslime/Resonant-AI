package com.resonant.app.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.resonant.app.content.SemanticUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * The single owner of the TextToSpeech engine. No composable or screen talks to
 * android.speech.tts directly — everything goes through this class.
 *
 * Content is modeled as a queue of [SemanticUnit]s with a current index. Lessons,
 * the chat mockup, and quiz-option exploration all reuse the same queue/next/
 * previous primitives, which is what keeps navigation feeling identical across
 * every screen.
 *
 * A separate [announce] path exists for short interjections ("Correct.",
 * "Option B selected.", orientation status) that should interrupt speech
 * immediately without disturbing the queue position the user will resume from.
 */
class AudioManager(context: Context) {

    companion object {
        val SPEEDS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        const val DEFAULT_SPEED_INDEX = 1 // 1.0x
        private const val ANNOUNCE_UTTERANCE_ID = "resonant_announce"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var ready = false
    private var pendingAutoAdvance = false

    private val _queue = MutableStateFlow<List<SemanticUnit>>(emptyList())
    val queue: StateFlow<List<SemanticUnit>> = _queue

    private val _index = MutableStateFlow(0)
    val index: StateFlow<Int> = _index

    private val _currentUnit = MutableStateFlow<SemanticUnit?>(null)
    val currentUnit: StateFlow<SemanticUnit?> = _currentUnit

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    private val _speedIndex = MutableStateFlow(DEFAULT_SPEED_INDEX)
    val speedIndex: StateFlow<Int> = _speedIndex

    val speed: Float get() = SPEEDS[_speedIndex.value]

    private var autoAdvanceEnabled = false

    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                tts.setSpeechRate(speed)
                ready = true
            }
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (utteranceId != ANNOUNCE_UTTERANCE_ID) {
                    mainHandler.post {
                        _isSpeaking.value = true
                        _isPaused.value = false
                    }
                }
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId == ANNOUNCE_UTTERANCE_ID) return
                mainHandler.post {
                    _isSpeaking.value = false
                    if (autoAdvanceEnabled && !_isPaused.value) {
                        next()
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post { _isSpeaking.value = false }
            }
        })
    }

    // ---------------------------------------------------------------
    // Queue management
    // ---------------------------------------------------------------

    fun setQueue(units: List<SemanticUnit>, startIndex: Int = 0, autoAdvance: Boolean = false) {
        _queue.value = units
        autoAdvanceEnabled = autoAdvance
        val clamped = startIndex.coerceIn(0, (units.size - 1).coerceAtLeast(0))
        _index.value = clamped
        _currentUnit.value = units.getOrNull(clamped)
        if (_currentUnit.value != null) speakCurrent()
    }

    fun speakCurrent() {
        val unit = _currentUnit.value ?: return
        if (!ready) return
        _isPaused.value = false
        tts.setSpeechRate(speed)
        tts.speak(unit.text, TextToSpeech.QUEUE_FLUSH, null, unit.id)
    }

    fun repeatCurrent() = speakCurrent()

    /** Moves forward in the queue and speaks the new current unit. Returns false at the end. */
    fun next(): Boolean {
        val q = _queue.value
        val newIndex = _index.value + 1
        if (newIndex >= q.size) return false
        _index.value = newIndex
        _currentUnit.value = q[newIndex]
        speakCurrent()
        return true
    }

    /** Moves backward in the queue and speaks the new current unit. Returns false at the start. */
    fun previous(): Boolean {
        val q = _queue.value
        val newIndex = _index.value - 1
        if (newIndex < 0) return false
        _index.value = newIndex
        _currentUnit.value = q[newIndex]
        speakCurrent()
        return true
    }

    fun jumpTo(newIndex: Int) {
        val q = _queue.value
        val clamped = newIndex.coerceIn(0, (q.size - 1).coerceAtLeast(0))
        _index.value = clamped
        _currentUnit.value = q.getOrNull(clamped)
        speakCurrent()
    }

    // ---------------------------------------------------------------
    // Transport controls
    // ---------------------------------------------------------------

    fun pause() {
        if (!_isSpeaking.value) return
        tts.stop()
        _isPaused.value = true
        _isSpeaking.value = false
    }

    fun resume() {
        if (!_isPaused.value) return
        speakCurrent()
    }

    fun togglePause() {
        if (_isPaused.value) resume() else pause()
    }

    fun stop() {
        tts.stop()
        _isSpeaking.value = false
        _isPaused.value = false
    }

    // ---------------------------------------------------------------
    // Speed control — changing speed restarts only the current unit,
    // never the whole lesson/chat/quiz.
    // ---------------------------------------------------------------

    fun increaseSpeed(): Boolean {
        val newIndex = _speedIndex.value + 1
        if (newIndex >= SPEEDS.size) return false
        _speedIndex.value = newIndex
        if (_isSpeaking.value || _isPaused.value) speakCurrent()
        return true
    }

    fun decreaseSpeed(): Boolean {
        val newIndex = _speedIndex.value - 1
        if (newIndex < 0) return false
        _speedIndex.value = newIndex
        if (_isSpeaking.value || _isPaused.value) speakCurrent()
        return true
    }

    // ---------------------------------------------------------------
    // One-off interjections that don't disturb queue state
    // ---------------------------------------------------------------

    fun announce(text: String) {
        if (!ready) return
        tts.setSpeechRate(speed)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, ANNOUNCE_UTTERANCE_ID)
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
