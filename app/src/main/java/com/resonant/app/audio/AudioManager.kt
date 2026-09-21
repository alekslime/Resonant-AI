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
        /** Every announcement gets a unique id under this prefix (see [announce]). */
        private const val ANNOUNCE_PREFIX = "resonant_announce_"
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

    // Announcements are tracked individually (unique ids) so the app can (a) wait for
    // one to finish before doing something that must not overlap it — e.g. opening the
    // microphone after "Listening." — and (b) queue a screen's content behind one instead
    // of cutting it off. Main-thread only.
    private var announceCounter = 0
    private var activeAnnounceId: String? = null
    private val announceCallbacks = mutableMapOf<String, () -> Unit>()

    // Streamed content (chat replies arriving sentence by sentence). While a stream is
    // open, running off the end of the queue means "more is coming", not "finished".
    private var streamOpen = false
    private var awaitingMore = false

    // Speech requested while the engine is still starting (cold launch, or coming back
    // from the background) is held here and replayed the moment the engine is ready,
    // instead of being silently dropped. Main-thread only.
    private class PendingAnnouncement(val text: String, val onFinished: (() -> Unit)?)
    private var pendingAnnouncement: PendingAnnouncement? = null
    private var pendingCurrent = false

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
                if (status != TextToSpeech.SUCCESS) {
                    // No usable engine. Release it so the next onStart can try again, and
                    // let anything waiting on speech go: announce() promises callers that
                    // onFinished is never left hanging.
                    tts?.shutdown()
                    tts = null
                    dropPending()
                    return@post
                }
                val t = tts ?: return@post
                t.language = Locale.US
                t.setSpeechRate(speed)
                // Listener registered here — inside the ready callback — so it's
                // guaranteed to be set before any speak() call can complete.
                t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        if (isAnnouncement(utteranceId)) return
                        mainHandler.post {
                            _isSpeaking.value = true
                            _isPaused.value = false
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        if (isAnnouncement(utteranceId)) {
                            settleAnnouncement(utteranceId)
                            return
                        }
                        mainHandler.post {
                            _isSpeaking.value = false
                            if (autoAdvanceEnabled && !_isPaused.value) {
                                // Ran off the end of a stream that is still open: remember
                                // to pick up with the next appended unit.
                                if (!next() && streamOpen) awaitingMore = true
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (isAnnouncement(utteranceId)) {
                            settleAnnouncement(utteranceId)
                            return
                        }
                        mainHandler.post { _isSpeaking.value = false }
                    }

                    // Flushed or stopped before finishing. Still settle: a caller waiting
                    // on an announcement must never be left waiting forever.
                    override fun onStop(utteranceId: String?, interrupted: Boolean) {
                        if (isAnnouncement(utteranceId)) settleAnnouncement(utteranceId)
                    }
                })
                ready = true
                _isReady.value = true
                replayPending(t)
            }
        }
    }

    // Queue management

    /**
     * @param queueBehindAnnouncement when true and an announcement is still playing, the
     * first unit waits for it instead of cutting it off. For content that arrives right
     * after a spoken acknowledgement (chat: "You said … Thinking." → the reply). Everything
     * the user does by touch — next, previous, repeat — still interrupts immediately.
     */
    fun setQueue(
        units: List<SemanticUnit>,
        startIndex: Int = 0,
        autoAdvance: Boolean = false,
        queueBehindAnnouncement: Boolean = false
    ) {
        _queue.value = units
        autoAdvanceEnabled = autoAdvance
        awaitingMore = false
        val clamped = startIndex.coerceIn(0, (units.size - 1).coerceAtLeast(0))
        _index.value = clamped
        _currentUnit.value = units.getOrNull(clamped)
        if (_currentUnit.value != null) speakCurrent(queueBehindAnnouncement)
    }

    /**
     * Adds units to the end of the current queue without disturbing playback. If the
     * queue had already played to its end while a stream was open, playback continues
     * with the first appended unit.
     */
    fun appendUnits(units: List<SemanticUnit>) {
        if (units.isEmpty()) return
        _queue.value = _queue.value + units
        if (awaitingMore && !_isPaused.value) {
            awaitingMore = false
            next()
        }
    }

    /** Mark the queue as still being filled (see [appendUnits]). Pair with [endStream]. */
    fun beginStream() {
        streamOpen = true
        awaitingMore = false
    }

    fun endStream() {
        streamOpen = false
        awaitingMore = false
    }

    fun speakCurrent(queueBehindAnnouncement: Boolean = false) {
        val unit = _currentUnit.value ?: return
        if (!ready) {
            // Engine still starting: speak as soon as it can. With no engine at all
            // there is nothing to wait for.
            if (tts != null) pendingCurrent = true
            return
        }
        _isPaused.value = false
        val t = tts ?: return
        t.setSpeechRate(speed)
        val mode = if (queueBehindAnnouncement && activeAnnounceId != null) {
            TextToSpeech.QUEUE_ADD
        } else {
            TextToSpeech.QUEUE_FLUSH
        }
        t.speak(unit.text, mode, null, unit.id)
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

    fun jumpTo(newIndex: Int, queueBehindAnnouncement: Boolean = false) {
        val q = _queue.value
        val clamped = newIndex.coerceIn(0, (q.size - 1).coerceAtLeast(0))
        _index.value = clamped
        _currentUnit.value = q.getOrNull(clamped)
        speakCurrent(queueBehindAnnouncement)
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
        speakAnnouncement(t, text, null)

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

    /**
     * Speaks [text] immediately, interrupting whatever is playing.
     *
     * @param onFinished invoked exactly once, on the main thread, when the announcement
     * finishes, is interrupted by other speech, or fails — and immediately if speech is
     * unavailable, so a caller waiting on it (e.g. before opening the microphone, which
     * would otherwise hear the app's own voice) can never be left waiting.
     */
    fun announce(text: String, onFinished: (() -> Unit)? = null) {
        val t = tts
        if (t == null) {
            // No engine at all: nothing will ever be spoken, so don't make the caller wait.
            onFinished?.let { mainHandler.post(it) }
            return
        }
        if (!ready) {
            // Engine still starting. Hold the latest announcement and speak it when ready;
            // one it replaces counts as interrupted, so its caller is released too.
            pendingAnnouncement?.onFinished?.let { mainHandler.post(it) }
            pendingAnnouncement = PendingAnnouncement(text, onFinished)
            return
        }
        t.setSpeechRate(speed)
        speakAnnouncement(t, text, onFinished)
    }

    private fun isAnnouncement(utteranceId: String?): Boolean =
        utteranceId?.startsWith(ANNOUNCE_PREFIX) == true

    private fun speakAnnouncement(t: TextToSpeech, text: String, onFinished: (() -> Unit)?) {
        val id = ANNOUNCE_PREFIX + (++announceCounter)
        activeAnnounceId = id
        if (onFinished != null) announceCallbacks[id] = onFinished
        val result = t.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        // speak() can fail outright (engine died); no progress callback will ever come.
        if (result == TextToSpeech.ERROR) settleAnnouncement(id)
    }

    /** Safe to call from any thread; the state change and callback run on main. */
    private fun settleAnnouncement(id: String?) {
        if (id == null) return
        mainHandler.post {
            if (activeAnnounceId == id) activeAnnounceId = null
            announceCallbacks.remove(id)?.invoke()
        }
    }

    /** Speaks whatever was requested while the engine was starting. Announcement first. */
    private fun replayPending(t: TextToSpeech) {
        val announcement = pendingAnnouncement
        val current = pendingCurrent
        pendingAnnouncement = null
        pendingCurrent = false
        if (announcement != null) {
            t.setSpeechRate(speed)
            speakAnnouncement(t, announcement.text, announcement.onFinished)
        }
        if (current) speakCurrent(queueBehindAnnouncement = announcement != null)
    }

    /** Gives up on speech that was waiting for an engine, releasing any caller waiting on it. */
    private fun dropPending() {
        pendingCurrent = false
        val announcement = pendingAnnouncement
        pendingAnnouncement = null
        announcement?.onFinished?.invoke()
    }

    private fun settleAllAnnouncements() {
        activeAnnounceId = null
        val pending = announceCallbacks.values.toList()
        announceCallbacks.clear()
        pending.forEach { it.invoke() }
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
        awaitingMore = false
        tts?.stop()
        tts?.shutdown()
        tts = null
        settleAllAnnouncements()
        dropPending()
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
        settleAllAnnouncements()
        dropPending()
    }
}
