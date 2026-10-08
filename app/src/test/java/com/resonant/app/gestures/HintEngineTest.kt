package com.resonant.app.gestures

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HintEngineTest {

    private fun engine() = HintEngine(startMs = 0)

    @Test
    fun pause_hint_comes_after_the_pause_and_not_before() {
        val e = engine()
        assertNull(e.check(1_000, silent = true))
        assertNull(e.check(24_000, silent = true))
        assertEquals(HintReason.PAUSE, e.check(25_000, silent = true))
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
        e.noteGesture(20_000)
        assertNull(e.check(40_000, silent = true))
        assertEquals(HintReason.PAUSE, e.check(45_000, silent = true))
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
        assertEquals(HintReason.PAUSE, e.check(25_000, silent = true))
        // Cooldown is 45 s from the hint, and the pause timer also restarted.
        assertNull(e.check(60_000, silent = true))
        assertEquals(HintReason.PAUSE, e.check(70_000, silent = true))
        assertEquals(HintReason.PAUSE, e.check(140_000, silent = true))
        // Three given: that is the cap for this screen visit.
        assertNull(e.check(300_000, silent = true))
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
