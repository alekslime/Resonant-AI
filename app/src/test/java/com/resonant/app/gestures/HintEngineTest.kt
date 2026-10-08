package com.resonant.app.gestures

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HintEngineTest {

    private fun engine() = HintEngine(startMs = 0)

    // Checks once a second, like the screen does. Returns the time and reason of the first hint.
    private fun HintEngine.run(fromMs: Long, toMs: Long, silent: Boolean = true): Pair<Long, HintReason>? {
        var t = fromMs
        while (t <= toMs) {
            check(t, silent)?.let { return t to it }
            t += 1_000
        }
        return null
    }

    @Test
    fun pause_hint_comes_after_the_pause_and_not_before() {
        val e = engine()
        assertNull(e.run(0, 24_000))
        assertEquals(25_000L to HintReason.PAUSE, e.run(25_000, 60_000))
    }

    @Test
    fun no_hint_while_speaking_and_the_wait_starts_when_speech_ends() {
        val e = engine()
        assertNull(e.check(30_000, silent = false))
        // Speech just ended at 30 s: the 25 s pause counts from here, not from 0.
        assertNull(e.check(31_000, silent = true))
        assertNull(e.check(55_000, silent = true))
        assertEquals(HintReason.PAUSE, e.check(56_000, silent = true))
    }

    @Test
    fun a_gesture_restarts_the_pause() {
        val e = engine()
        assertNull(e.run(0, 19_000))
        e.noteGesture(20_000)
        assertEquals(45_000L to HintReason.PAUSE, e.run(20_000, 100_000))
    }

    @Test
    fun three_misses_in_a_row_means_struggle() {
        val e = engine()
        e.noteMiss(1_000)
        e.noteMiss(2_000)
        assertNull(e.check(2_500, silent = true))
        e.noteMiss(3_000)
        assertEquals(HintReason.STRUGGLE, e.check(3_500, silent = true))
    }

    @Test
    fun misses_spread_out_or_broken_by_a_good_gesture_do_not_count() {
        val spread = engine()
        spread.noteMiss(1_000)
        spread.noteMiss(6_000)
        spread.noteMiss(12_000)
        assertNull(spread.check(12_500, silent = true))

        val broken = engine()
        broken.noteMiss(1_000)
        broken.noteMiss(2_000)
        broken.noteGesture(2_500)
        broken.noteMiss(3_000)
        assertNull(broken.check(3_500, silent = true))
    }

    @Test
    fun struggle_waits_for_the_app_to_stop_speaking() {
        val e = engine()
        e.noteMiss(1_000); e.noteMiss(2_000); e.noteMiss(3_000)
        assertNull(e.check(3_500, silent = false))
        assertEquals(HintReason.STRUGGLE, e.check(4_000, silent = true))
    }

    @Test
    fun cooldown_and_cap() {
        val e = engine()
        assertEquals(25_000L to HintReason.PAUSE, e.run(0, 100_000))
        // The pause is over again by 51 s, but the cooldown lasts 45 s from the hint.
        assertNull(e.run(26_000, 69_000))
        assertEquals(70_000L to HintReason.PAUSE, e.run(70_000, 200_000))
        assertEquals(115_000L to HintReason.PAUSE, e.run(71_000, 300_000))
        // Three given: that is the cap for this screen visit.
        assertNull(e.run(116_000, 600_000))
    }

    @Test
    fun coming_back_to_the_app_resets_the_clock() {
        val e = engine()
        e.noteMiss(1_000); e.noteMiss(2_000)
        e.resetClock(100_000)
        e.noteMiss(100_500)
        assertNull(e.check(101_000, silent = true))
        assertNull(e.check(110_000, silent = true))
    }

    @Test
    fun struggle_adds_a_lead_in() {
        assertEquals("Having trouble? Do it.", HintText.say(HintReason.STRUGGLE, "Do it."))
        assertEquals("Do it.", HintText.say(HintReason.PAUSE, "Do it."))
    }
}
