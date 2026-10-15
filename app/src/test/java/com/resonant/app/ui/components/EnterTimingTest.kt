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
}
