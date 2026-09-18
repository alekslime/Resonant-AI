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

class AudioManager(context: Context) {

    companion object {
        val SPEEDS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        const val DEFAULT_SPEED_INDEX = 1
        private const val ANNOUNCE_UTTERANCE_ID = "resonant_announce"
    }

    private val appContext: Context = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var ready = false
    private val _isReady = MutableStateFlow(false)
    /** True once the engine is speech-capable. False at launch, and stays false
     *  if the device has no usable TTS engine — screens can use this to warn
     *  the user instead of silently doing nothing when speak calls no-op. */
    val isReady: StateFlow<Boolean> = _isReady

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
    private var tts: TextToSpeech? = null

    init {
        initEngine()
    }

    /**
     * Builds the TTS engine. Separate from [init] because the engine is fully
     * released whenever the app goes to the background (see [releaseForBackground])
     * and has to be rebuilt on return — a shut-down TextToSpeech cannot be reused.
     */
    private fun initEngine() {
        if (tts != null) return
        tts = TextToSpeech(appContext) { status ->
            // Posted rather than run inline: the init callback can fire before the
            // TextToSpeech constructor has returned, which would leave `tts` still
            // null here. Posting guarantees the assignment below has landed.
            mainHandler.post {
                if (status != TextToSpeech.SUCCESS) return@post
                val t = tts ?: return@post
                t.language = Locale.US
                t.setSpeechRate(speed)
                // Listener registered here — inside the ready callback — so it's
                // guaranteed to be set before any speak() call can complete.
                t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        if (utteranceId == ANNOUNCE_UTTERANCE_ID) return
                        mainHandler.post {
                            _isSpeaking.value = true
                            _isPaused.value = false
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        if (utteranceId == ANNOUNCE_UTTERANCE_ID) return
                        mainHandler.post {
                            _isSpeaking.value = false
                            if (autoAdvanceEnabled && !_isPaused.value) next()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        mainHandler.post { _isSpeaking.value = false }
                    }
                })
                ready = true
                _isReady.value = true
            }
        }
    }

    // Queue management

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
        val t = tts ?: return
        t.setSpeechRate(speed)
        t.speak(unit.text, TextToSpeech.QUEUE_FLUSH, null, unit.id)
    }

    fun repeatCurrent() = speakCurrent()

    fun next(): Boolean {
        val q = _queue.value
        val newIndex = _index.value + 1
        if (newIndex >= q.size) return false
        _index.value = newIndex
        _currentUnit.value = q[newIndex]
        speakCurrent()
        return true
    }

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

    // Transport

    fun pause() {
        if (!_isSpeaking.value) return
        tts?.stop()
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
        tts?.stop()
        _isSpeaking.value = false
        _isPaused.value = false
    }

    // Speed — every change is spoken, then the current unit resumes at the new
    // rate. Without the spoken confirmation a non-sighted user gets a haptic tick
    // and no idea which step they landed on.

    fun increaseSpeed(): Boolean {
        val newIndex = _speedIndex.value + 1
        if (newIndex >= SPEEDS.size) {
            announceSpeed(atLimit = true)
            return false
        }
        _speedIndex.value = newIndex
        announceSpeed(atLimit = false)
        return true
    }

    fun decreaseSpeed(): Boolean {
        val newIndex = _speedIndex.value - 1
        if (newIndex < 0) {
            announceSpeed(atLimit = true)
            return false
        }
        _speedIndex.value = newIndex
        announceSpeed(atLimit = false)
        return true
    }

    /**
     * Speaks the new rate, then re-queues the current unit behind it so playback
     * continues at the new speed. Both utterances go out at the new rate, so the
     * spoken number is itself a sample of what was just chosen.
     */
    private fun announceSpeed(atLimit: Boolean) {
        val t = tts ?: return
        val label = speedLabel(speed)
        val text = when {
            atLimit && _speedIndex.value == SPEEDS.lastIndex -> "Fastest speed, $label."
            atLimit -> "Slowest speed, $label."
            else -> label
        }
        if (!ready) return
        t.setSpeechRate(speed)
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, ANNOUNCE_UTTERANCE_ID)

        // Resume whatever was playing, queued behind the confirmation.
        val unit = _currentUnit.value
        if (unit != null && (_isSpeaking.value || _isPaused.value)) {
            _isPaused.value = false
            t.speak(unit.text, TextToSpeech.QUEUE_ADD, null, unit.id)
        }
    }

    /** "1.5x" reads badly aloud; "one point five times speed" reads correctly. */
    private fun speedLabel(value: Float): String {
        val spoken = when (value) {
            0.75f -> "zero point seven five"
            1.0f -> "normal"
            1.25f -> "one point two five"
            1.5f -> "one point five"
            1.75f -> "one point seven five"
            2.0f -> "double"
            else -> value.toString()
        }
        return if (value == 1.0f || value == 2.0f) "$spoken speed" else "$spoken times speed"
    }

    // Announce — interrupts current speech without disturbing queue position

    fun announce(text: String) {
        val t = tts ?: return
        if (!ready) return
        t.setSpeechRate(speed)
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, ANNOUNCE_UTTERANCE_ID)
    }

    // Lifecycle

    /**
     * Called from Activity.onStop. Fully releases the TTS engine so nothing keeps
     * talking while the app is backgrounded. Queue position, speed and paused
     * state all survive — only the engine goes away.
     */
    fun releaseForBackground() {
        if (tts == null) return
        ready = false
        _isReady.value = false
        _isSpeaking.value = false
        _isPaused.value = _currentUnit.value != null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    /** Called from Activity.onStart. Rebuilds the engine released above. */
    fun restoreFromBackground() {
        initEngine()
    }

    fun shutdown() {
        ready = false
        _isReady.value = false
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
