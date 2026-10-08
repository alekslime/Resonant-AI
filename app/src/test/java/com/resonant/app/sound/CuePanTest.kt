package com.resonant.app.sound

import com.resonant.app.haptics.HapticPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CuePanTest {

    @Test
    fun no_cue_sits_in_the_middle_that_belongs_to_the_voice() {
        HapticPattern.values().forEach {
            assertTrue("$it is dead centre", CuePan.panFor(it) != 0f)
        }
    }

    @Test
    fun back_slower_and_wrong_go_left_and_forward_goes_right() {
        listOf(HapticPattern.PREVIOUS, HapticPattern.BACK, HapticPattern.SPEED_DOWN, HapticPattern.INCORRECT, HapticPattern.ERROR)
            .forEach { assertTrue("$it should be left", CuePan.panFor(it) < 0f) }
        listOf(HapticPattern.NEXT, HapticPattern.SECTION_CHANGE, HapticPattern.SPEED_UP, HapticPattern.CORRECT, HapticPattern.OPTION_A)
            .forEach { assertTrue("$it should be right", CuePan.panFor(it) > 0f) }
    }

    @Test
    fun next_and_previous_end_up_on_opposite_sides() {
        assertEquals(-CuePan.panFor(HapticPattern.NEXT), CuePan.panFor(HapticPattern.PREVIOUS), 0f)
    }

    @Test
    fun the_far_ear_is_turned_down_but_never_muted() {
        HapticPattern.values().forEach {
            val (left, right) = CuePan.gains(CuePan.panFor(it))
            assertEquals("$it near ear", 1f, maxOf(left, right), 0f)
            assertTrue("$it far ear too quiet for one earbud", minOf(left, right) >= 0.6f)
            assertTrue("$it not panned at all", minOf(left, right) < 1f)
        }
    }

    @Test
    fun centre_is_full_level_in_both_ears_and_out_of_range_is_clamped() {
        assertEquals(Pair(1f, 1f), CuePan.gains(0f))
        assertEquals(Pair(0f, 1f), CuePan.gains(5f))
        assertEquals(Pair(1f, 0f), CuePan.gains(-5f))
    }
}
