package com.resonant.app.gestures

enum class HintReason { PAUSE, STRUGGLE }

/**
 * Decides when a screen should speak a short hint. No clock and no Android in here: every
 * call is given the time, so the rules can be unit tested.
 *
 * Two triggers:
 * - PAUSE: the user has done nothing and the app has been silent for [pauseMs].
 * - STRUGGLE: [missLimit] gestures in a row did nothing within [missWindowMs].
 *
 * One engine per screen visit. It stays quiet while the app is speaking or paused, waits
 * [cooldownMs] between hints and gives at most [maxHints] before the user leaves the screen.
 */
class HintEngine(
    startMs: Long,
    private val pauseMs: Long = 25_000,
    private val missWindowMs: Long = 8_000,
    private val missLimit: Int = 3,
    private val cooldownMs: Long = 45_000,
    private val maxHints: Int = 3
) {
    private var lastActivity = startMs
    private var silentSince: Long? = null
    private var lastHintAt: Long? = null
    private var given = 0
    private val misses = ArrayDeque<Long>()

    /** A gesture that did something. Ends a run of misses. */
    fun noteGesture(now: Long) {
        lastActivity = now
        misses.clear()
    }

    /** A gesture that did nothing (no cue played) or only bumped an edge or an error. */
    fun noteMiss(now: Long) {
        lastActivity = now
        misses.addLast(now)
    }

    /** The app came back to the screen: time spent away is not the user being stuck. */
    fun resetClock(now: Long) {
        lastActivity = now
        silentSince = null
        misses.clear()
    }

    /** [silent] = the app is not speaking and the user has not paused it. */
    fun check(now: Long, silent: Boolean): HintReason? {
        if (!silent) {
            silentSince = null
            return null
        }
        if (silentSince == null) silentSince = now
        if (given >= maxHints) return null
        lastHintAt?.let { if (now - it < cooldownMs) return null }

        while (misses.isNotEmpty() && now - misses.first() > missWindowMs) misses.removeFirst()
        if (misses.size >= missLimit) return fire(now, HintReason.STRUGGLE)

        val quietSince = maxOf(lastActivity, silentSince ?: now)
        if (now - quietSince >= pauseMs) return fire(now, HintReason.PAUSE)
        return null
    }

    private fun fire(now: Long, reason: HintReason): HintReason {
        given += 1
        lastHintAt = now
        lastActivity = now
        silentSince = null
        misses.clear()
        return reason
    }
}

/** What a hint says. Screens can pass their own sentence; this is the default. */
object HintText {
    fun general(twoFingerSwipe: Boolean): String =
        if (twoFingerSwipe) {
            "Swipe down with two fingers for the next item, tap the middle to choose, or hold three fingers to hear where you are."
        } else {
            "Swipe down for the next option, tap the middle to choose, or hold three fingers to hear where you are."
        }

    fun say(reason: HintReason, hint: String): String =
        if (reason == HintReason.STRUGGLE) "Having trouble? $hint" else hint
}
