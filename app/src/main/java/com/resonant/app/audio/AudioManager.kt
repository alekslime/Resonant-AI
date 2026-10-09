package com.resonant.app.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.resonant.app.content.SemanticUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.Locale
import kotlin.math.exp
import kotlin.math.max

class AudioManager(context: Context, initialEngine: String? = null) {

    companion object {
        val SPEEDS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f)
        const val DEFAULT_SPEED_INDEX = 1
        /** Every announcement gets a unique id under this prefix (see [announce]). */
        private const val ANNOUNCE_PREFIX = "resonant_announce_"
        private const val PCM_PREFIX = "resonant_pcm_"
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

    // Speech loudness for the dots. Unit speech is synthesized to a file and played by [pcm], so
    // its level is the real PCM loudness. The word-timed estimate below (each word boundary
    // kicks the level, which decays) only covers the fallback path where synthesis fails.
    private val pcm = PcmSpeaker(mainHandler)

    // Announcements go through the same synthesize -> PCM path, on their own player, so the
    // dots get their real loudness too. Short and often repeated ("Listening."), so decoded
    // audio is cached per text+speed; a repeat starts with no synthesis delay.
    private val annPcm = PcmSpeaker(mainHandler)
    private class PendingSynth(val text: String, val key: String)
    private val annPending = mutableMapOf<String, PendingSynth>()
    private val annFallbackIds = mutableSetOf<String>() // announcements spoken by plain TTS instead
    private val annCache = object : LinkedHashMap<String, WavPcm>(16, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, WavPcm>) = size > 16
    }
    private var pcmGen = 0
    // A unit that must start once the currently playing announcement has finished.
    private var deferredUnit = false
    @Volatile private var levelPeak = 0f
    @Volatile private var levelAt = 0L

    /** 0..1, safe to call every frame from any thread. 0 when not speaking or paused. */
    fun speechLevel(): Float {
        val announcing = _isAnnouncing.value
        if (!(_isSpeaking.value || announcing) || (_isPaused.value && !announcing)) return 0f
        (pcm.level() ?: annPcm.level())?.let { return it } // real loudness of the PCM being played
        val dt = (SystemClock.uptimeMillis() - levelAt).toFloat()
        return max(.12f, levelPeak * exp(-dt / 220f))
    }

    private val _isAnnouncing = MutableStateFlow(false)
    /** True while an announcement (greeting, errors, confirmations) is being spoken. */
    val isAnnouncing: StateFlow<Boolean> = _isAnnouncing

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    private val _speedIndex = MutableStateFlow(DEFAULT_SPEED_INDEX)
    val speedIndex: StateFlow<Int> = _speedIndex

    val speed: Float get() = SPEEDS[_speedIndex.value]

    /** Called with the new index after every speed change, so it can be saved. */
    var onSpeedIndexChanged: ((Int) -> Unit)? = null

    /**
     * Sets the speed without announcing it — for restoring the saved value at
     * startup, before any speech is queued. Out-of-range values are clamped, so a
     * stale saved index can never crash the lookup in [speed].
     */
    fun restoreSpeedIndex(index: Int) {
        _speedIndex.value = index.coerceIn(0, SPEEDS.lastIndex)
    }

    private var autoAdvanceEnabled = false
    private var tts: TextToSpeech? = null

    /** Package name of the chosen engine. Null means the system default. */
    private var enginePackage: String? = initialEngine?.takeIf { it.isNotBlank() }
    private var installedEngines: List<EngineOption> = emptyList()

    /** Called with the engine package (null = system default) after every engine change, so it can be saved. */
    var onEngineChanged: ((String?) -> Unit)? = null

    class EngineOption(val packageName: String?, val label: String)

    /** The system default first, then every installed engine. Empty list of extras until the engine is ready. */
    fun engineOptions(): List<EngineOption> {
        return listOf(EngineOption(null, "System default")) + installedEngines
    }

    fun engineLabel(packageName: String? = enginePackage): String =
        engineOptions().firstOrNull { it.packageName == packageName }?.label
            ?: packageName?.substringAfterLast('.')
            ?: "System default"

    val currentEngine: String? get() = enginePackage

    /** Moves to the next engine in [engineOptions], wrapping around, and says which one it landed on. */
    fun cycleEngine() {
        val options = engineOptions()
        if (options.size < 2) {
            announce("No other voice engine is installed.")
            return
        }
        val at = options.indexOfFirst { it.packageName == enginePackage }
        setEngine(options[(at + 1) % options.size].packageName)
    }

    fun setEngine(packageName: String?) {
        val chosen = packageName?.takeIf { it.isNotBlank() }
        if (chosen == enginePackage && tts != null) return
        enginePackage = chosen
        onEngineChanged?.invoke(chosen)
        val wasPaused = _isPaused.value
        rebuildEngine()
        announce("Voice engine: ${engineLabel(chosen)}.")
        if (_currentUnit.value != null && !wasPaused) deferredUnit = true
    }

    private fun rebuildEngine() {
        ready = false
        _isReady.value = false
        _isSpeaking.value = false
        awaitingMore = false
        deferredUnit = false
        cancelUnitSpeech()
        tts?.stop()
        tts?.shutdown()
        tts = null
        settleAllAnnouncements()
        annCache.clear()
        initEngine()
    }

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
        tts = TextToSpeech(appContext, { status ->
            // Posted rather than run inline: the init callback can fire before the
            // TextToSpeech constructor has returned, which would leave `tts` still
            // null here. Posting guarantees the assignment below has landed.
            mainHandler.post {
                if (status != TextToSpeech.SUCCESS && enginePackage != null) {
                    // The chosen engine is gone or broken: go back to the system default.
                    tts?.shutdown()
                    tts = null
                    enginePackage = null
                    onEngineChanged?.invoke(null)
                    initEngine()
                    announce("That voice engine is not available. Using the system default.")
                    return@post
                }
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
                installedEngines = t.engines.map { EngineOption(it.name, it.label) }
                t.language = Locale.US
                t.setSpeechRate(speed)
                // Listener registered here — inside the ready callback — so it's
                // guaranteed to be set before any speak() call can complete.
                t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        if (isAnnouncement(utteranceId)) {
                            // Only the plain-TTS fallback is audible from here; the PCM path
                            // flags itself when playback actually starts.
                            mainHandler.post {
                                if (activeAnnounceId == utteranceId && utteranceId in annFallbackIds) _isAnnouncing.value = true
                            }
                            return
                        }
                        if (isPcm(utteranceId)) return
                        mainHandler.post {
                            _isSpeaking.value = true
                            _isPaused.value = false
                        }
                    }

                    override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                        if (isPcm(utteranceId)) return // unit speech has real PCM level
                        levelPeak = (.55f + .06f * (end - start)).coerceAtMost(1f)
                        levelAt = SystemClock.uptimeMillis()
                    }

                    override fun onDone(utteranceId: String?) {
                        if (isAnnouncement(utteranceId)) {
                            mainHandler.post { onAnnouncementSynthesized(utteranceId!!) }
                            return
                        }
                        if (isPcm(utteranceId)) {
                            // Synthesis finished (not playback): hand the WAV to our player.
                            mainHandler.post { onSynthesized(pcmGenOf(utteranceId)) }
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
                            mainHandler.post { onAnnouncementFailed(utteranceId!!) }
                            return
                        }
                        if (isPcm(utteranceId)) {
                            mainHandler.post { onSynthesisFailed(pcmGenOf(utteranceId)) }
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
        }, enginePackage)
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
        if (queueBehindAnnouncement && activeAnnounceId != null) {
            // Wait for the announcement; settleAnnouncement starts this unit when it ends.
            cancelUnitSpeech()
            deferredUnit = true
            return
        }
        deferredUnit = false
        cancelUnitSpeech()
        cancelAnnouncement()
        t.stop() // flush semantics: anything still queued (announcement, old synthesis) goes
        requestUnitSpeech(t, unit)
    }

    // Unit speech: synthesize to a WAV, then play it ourselves so the dots get real PCM loudness.

    /** Drops any in-flight synthesis or playback of the current unit. */
    private fun cancelUnitSpeech() {
        pcmGen++ // late callbacks from the old request see a stale generation and are ignored
        pcm.stop()
        _isSpeaking.value = false
    }

    private fun isPcm(id: String?) = id?.startsWith(PCM_PREFIX) == true
    private fun pcmGenOf(id: String?) = id?.removePrefix(PCM_PREFIX)?.toIntOrNull() ?: -1
    private fun pcmFile(gen: Int) = File(appContext.cacheDir, "resonant_pcm_$gen.wav")

    private fun requestUnitSpeech(t: TextToSpeech, unit: SemanticUnit) {
        val gen = ++pcmGen
        t.setSpeechRate(speed)
        val result = t.synthesizeToFile(unit.text, null, pcmFile(gen), PCM_PREFIX + gen)
        if (result == TextToSpeech.ERROR) fallbackSpeak(unit)
    }

    private fun onSynthesized(gen: Int) {
        val file = pcmFile(gen)
        val unit = _currentUnit.value
        if (gen != pcmGen || tts == null || unit == null) { file.delete(); return }
        val ok = pcm.play(
            file,
            onStarted = { if (gen == pcmGen) { _isSpeaking.value = true; _isPaused.value = false } },
            onFinished = { onUnitFinished(gen) }
        )
        if (!ok) fallbackSpeak(unit)
    }

    private fun onSynthesisFailed(gen: Int) {
        pcmFile(gen).delete()
        if (gen != pcmGen) return
        _currentUnit.value?.let { fallbackSpeak(it) }
    }

    /** Plain TTS (no real level; dots use the word-timed estimate). Same behavior as before PCM. */
    private fun fallbackSpeak(unit: SemanticUnit) {
        val t = tts ?: return
        t.setSpeechRate(speed)
        t.speak(unit.text, TextToSpeech.QUEUE_ADD, null, unit.id)
    }

    private fun onUnitFinished(gen: Int) {
        if (gen != pcmGen) return
        _isSpeaking.value = false
        if (autoAdvanceEnabled && !_isPaused.value) {
            // Ran off the end of a stream that is still open: remember to pick up with the
            // next appended unit.
            if (!next() && streamOpen) awaitingMore = true
        }
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
        cancelUnitSpeech()
        cancelAnnouncement()
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
        deferredUnit = false
        cancelUnitSpeech()
        cancelAnnouncement()
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
        onSpeedIndexChanged?.invoke(newIndex)
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
        onSpeedIndexChanged?.invoke(newIndex)
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
        val wasActive = _isSpeaking.value || _isPaused.value
        t.setSpeechRate(speed)
        speakAnnouncement(t, text, null)

        // Resume whatever was playing, queued behind the confirmation.
        if (_currentUnit.value != null && wasActive) {
            _isPaused.value = false
            deferredUnit = true
        }
    }

    /**
     * "1.5x" reads badly aloud; "one point five times speed" reads correctly. Anything
     * that speaks the current speed should go through this, not print the raw float.
     */
    fun speedLabel(value: Float = speed): String {
        val spoken = when (value) {
            0.75f -> "zero point seven five"
            1.0f -> "normal"
            1.25f -> "one point two five"
            1.5f -> "one point five"
            1.75f -> "one point seven five"
            2.0f -> "double"
            2.5f -> "two point five"
            3.0f -> "triple"
            3.5f -> "three point five"
            4.0f -> "quadruple"
            else -> value.toString()
        }
        return if (value == 1.0f || value == 2.0f || value == 3.0f || value == 4.0f) "$spoken speed" else "$spoken times speed"
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

    private fun annFile(id: String) = File(appContext.cacheDir, "$id.wav")

    private fun speakAnnouncement(t: TextToSpeech, text: String, onFinished: (() -> Unit)?) {
        cancelUnitSpeech() // an announcement interrupts unit speech, as QUEUE_FLUSH used to
        val previous = activeAnnounceId
        annPcm.stop()
        val id = ANNOUNCE_PREFIX + (++announceCounter)
        activeAnnounceId = id
        if (onFinished != null) announceCallbacks[id] = onFinished
        if (previous != null) {
            annPending.remove(previous)
            annFallbackIds.remove(previous)
            settleAnnouncement(previous) // superseded: its waiter must still be released
        }
        t.stop() // flush anything still queued
        t.setSpeechRate(speed)
        val key = "${enginePackage}|$speed|$text"
        val cached = annCache[key]
        if (cached != null) {
            playAnnouncement(id, cached)
            return
        }
        annPending[id] = PendingSynth(text, key)
        val result = t.synthesizeToFile(text, null, annFile(id), id)
        if (result == TextToSpeech.ERROR) onAnnouncementFailed(id)
    }

    private fun playAnnouncement(id: String, wav: WavPcm) {
        annPcm.play(
            wav,
            onStarted = { if (activeAnnounceId == id) _isAnnouncing.value = true },
            onFinished = { settleAnnouncement(id) }
        )
    }

    /** Synthesis finished (main thread) — or, for a fallback id, the spoken fallback finished. */
    private fun onAnnouncementSynthesized(id: String) {
        if (annFallbackIds.remove(id)) { settleAnnouncement(id); return }
        val pending = annPending.remove(id)
        val file = annFile(id)
        if (pending == null || id != activeAnnounceId) { file.delete(); return } // superseded/cancelled
        val wav = try { parseWav16(file.readBytes()) } catch (e: Exception) { null }
        file.delete()
        if (wav == null) { fallbackAnnounce(id, pending.text); return }
        if (wav.frames < wav.sampleRate * 6) annCache[pending.key] = wav
        playAnnouncement(id, wav)
    }

    private fun onAnnouncementFailed(id: String) {
        annFile(id).delete()
        if (annFallbackIds.remove(id)) { settleAnnouncement(id); return }
        val pending = annPending.remove(id) ?: return
        if (id == activeAnnounceId) fallbackAnnounce(id, pending.text) else settleAnnouncement(id)
    }

    /** Plain TTS (no real level; the dots use the word-timed estimate). Never leaves a waiter hanging. */
    private fun fallbackAnnounce(id: String, text: String) {
        val t = tts
        if (t == null) { settleAnnouncement(id); return }
        annFallbackIds.add(id)
        if (t.speak(text, TextToSpeech.QUEUE_ADD, null, id) == TextToSpeech.ERROR) {
            annFallbackIds.remove(id)
            settleAnnouncement(id)
        }
    }

    /** Stops the announcement in flight (synthesis, playback or fallback) and releases its waiter. */
    private fun cancelAnnouncement() {
        val id = activeAnnounceId ?: return
        annPcm.stop()
        annPending.remove(id)
        annFallbackIds.remove(id)
        settleAnnouncement(id)
    }

    /** Safe to call from any thread; the state change and callback run on main. */
    private fun settleAnnouncement(id: String?) {
        if (id == null) return
        mainHandler.post {
            if (activeAnnounceId == id) activeAnnounceId = null
            if (activeAnnounceId == null) _isAnnouncing.value = false
            announceCallbacks.remove(id)?.invoke()
            if (activeAnnounceId == null && deferredUnit) {
                deferredUnit = false
                val t = tts
                val unit = _currentUnit.value
                if (ready && t != null && unit != null) requestUnitSpeech(t, unit)
            }
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
        annPcm.stop()
        annPending.clear()
        annFallbackIds.clear()
        activeAnnounceId = null
        _isAnnouncing.value = false
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
        deferredUnit = false
        cancelUnitSpeech()
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
        deferredUnit = false
        cancelUnitSpeech()
        ready = false
        _isReady.value = false
        tts?.stop()
        tts?.shutdown()
        tts = null
        settleAllAnnouncements()
        dropPending()
    }
}
