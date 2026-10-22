package com.resonant.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnterTimingTest {

    @Test
    fun the_first_item_starts_at_once_and_each_later_one_waits_a_step_longer() {
        assertEquals(0, EnterTiming.delayMs(0))
        assertEquals(EnterTiming.STEP_MS, EnterTiming.delayMs(1))
        assertEquals(3 * EnterTiming.STEP_MS, EnterTiming.delayMs(3))
    }

    @Test
    fun a_long_list_never_waits_longer_than_the_cap() {
        val cap = EnterTiming.MAX_STEPS * EnterTiming.STEP_MS
        assertEquals(cap, EnterTiming.delayMs(EnterTiming.MAX_STEPS))
        assertEquals(cap, EnterTiming.delayMs(50))
    }

    @Test
    fun a_bad_index_is_treated_as_the_first() {
        assertEquals(0, EnterTiming.delayMs(-4))
    }

    @Test
    fun the_whole_arrival_is_over_in_well_under_a_second() {
        assertTrue(EnterTiming.delayMs(Int.MAX_VALUE) + EnterTiming.DURATION_MS < 1000)
    }

    @Test
    fun the_ping_starts_strong_at_the_card_and_is_gone_at_the_end() {
        assertEquals(GlowTiming.START_ALPHA, GlowTiming.alpha(0f), 0.0001f)
        assertEquals(0f, GlowTiming.alpha(1f), 0.0001f)
        assertEquals(GlowTiming.START_ALPHA, GlowTiming.alpha(-3f), 0.0001f)
        assertEquals(0f, GlowTiming.alpha(9f), 0.0001f)
        assertTrue(GlowTiming.alpha(0.3f) > GlowTiming.alpha(0.6f))
        assertEquals(0f, GlowTiming.spreadDp(0f), 0.0001f)
        assertEquals(GlowTiming.MAX_SPREAD_DP, GlowTiming.spreadDp(1f), 0.0001f)
        assertEquals(GlowTiming.MAX_SPREAD_DP, GlowTiming.spreadDp(5f), 0.0001f)
    }

    @Test
    fun the_ping_is_soft_short_and_never_solid() {
        assertTrue(GlowTiming.START_ALPHA < 0.4f)
        assertTrue(GlowTiming.PING_MS in 300..800)
        assertTrue(GlowTiming.LAYERS >= 2)
    }
}
