package com.resonant.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DotsPingTest {

    @Test
    fun the_wave_starts_strong_near_the_dots_and_is_gone_at_the_end() {
        assertEquals(DotsPingTiming.START_ALPHA, DotsPingTiming.alpha(0f), 0.0001f)
        assertEquals(0f, DotsPingTiming.alpha(1f), 0.0001f)
        assertTrue(DotsPingTiming.alpha(0.3f) > DotsPingTiming.alpha(0.6f))
        assertEquals(0f, DotsPingTiming.alpha(7f), 0.0001f)
    }

    @Test
    fun the_wave_grows_outwards_and_stays_in_range() {
        assertEquals(50f * DotsPingTiming.RADIUS_FROM, DotsPingTiming.radius(0f, 50f), 0.0001f)
        assertEquals(50f * DotsPingTiming.RADIUS_TO, DotsPingTiming.radius(1f, 50f), 0.0001f)
        assertEquals(DotsPingTiming.radius(1f, 50f), DotsPingTiming.radius(3f, 50f), 0.0001f)
        assertTrue(DotsPingTiming.radius(0.5f, 50f) > DotsPingTiming.radius(0.1f, 50f))
    }

    @Test
    fun a_flickering_state_does_not_ping_again_at_once() {
        assertFalse(DotsPingTiming.shouldPing(100L))
        assertFalse(DotsPingTiming.shouldPing(DotsPingTiming.MIN_GAP_MS - 1))
        assertTrue(DotsPingTiming.shouldPing(DotsPingTiming.MIN_GAP_MS))
    }

    @Test
    fun the_ping_is_soft_and_short() {
        assertTrue(DotsPingTiming.START_ALPHA < 0.4f)
        assertTrue(DotsPingTiming.PING_MS in 400..1000)
    }
}
