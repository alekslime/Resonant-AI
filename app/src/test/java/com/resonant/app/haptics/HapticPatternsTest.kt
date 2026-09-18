package com.resonant.app.haptics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticPatternsTest {

    private val timings = HapticPatterns.timings

    private val optionPatterns = listOf(
        HapticPattern.OPTION_A,
        HapticPattern.OPTION_B,
        HapticPattern.OPTION_C,
        HapticPattern.OPTION_D
    )

    @Test
    fun every_pattern_has_timings() {
        HapticPattern.values().forEach { assertNotNull("$it has no timings", timings[it]) }
    }

    @Test
    fun timings_are_valid_waveforms() {
        // createWaveform wants: initial delay, then alternating ON, OFF, ON ... ending on ON.
        timings.forEach { (pattern, t) ->
            assertTrue("$pattern too short", t.size >= 2)
            assertEquals("$pattern must end on an ON segment", 0, t.size % 2)
            assertEquals("$pattern must start with a 0 ms delay", 0L, t[0])
            t.forEachIndexed { i, ms ->
                if (i % 2 == 1) assertTrue("$pattern ON segment must be > 0", ms > 0)
                else assertTrue("$pattern OFF segment must be >= 0", ms >= 0)
            }
        }
    }

    @Test
    fun option_identifiers_are_a_counted_pulse_family() {
        // A = 1 long pulse ... D = 4. Counting is what makes them learnable by touch.
        optionPatterns.forEachIndexed { i, p ->
            assertEquals("$p pulse count", i + 1, timings.getValue(p).size / 2)
        }
    }

    @Test
    fun option_identifiers_never_share_a_pattern_with_anything_else() {
        // Regression guard: the option patterns used to reuse NEXT / PREVIOUS /
        // SECTION_CHANGE timings, making "I moved" indistinguishable from "this is option B".
        optionPatterns.forEach { option ->
            val mine = timings.getValue(option)
            timings.forEach { (other, theirs) ->
                if (other != option) {
                    assertFalse("$option collides with $other", mine.contentEquals(theirs))
                }
            }
        }
    }

    @Test
    fun the_cue_only_patterns_are_unique() {
        listOf(HapticPattern.HOLD_ENGAGED, HapticPattern.THINKING, HapticPattern.EDGE).forEach { cue ->
            val mine = timings.getValue(cue)
            timings.forEach { (other, theirs) ->
                if (other != cue) {
                    assertFalse("$cue collides with $other", mine.contentEquals(theirs))
                }
            }
        }
    }
}
