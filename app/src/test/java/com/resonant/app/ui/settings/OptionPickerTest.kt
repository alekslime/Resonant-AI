package com.resonant.app.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OptionPickerTest {

    @Test
    fun moves_one_step_either_way() {
        assertEquals(4, OptionPicker.move(3, 1, 12))
        assertEquals(2, OptionPicker.move(3, -1, 12))
    }

    @Test
    fun stops_at_both_ends_instead_of_wrapping() {
        assertNull(OptionPicker.move(11, 1, 12))
        assertNull(OptionPicker.move(0, -1, 12))
    }

    @Test
    fun an_empty_list_has_nowhere_to_go() {
        assertNull(OptionPicker.move(0, 1, 0))
    }

    @Test
    fun says_the_position_counting_from_one() {
        assertEquals("Warm, 7 of 12", OptionPicker.describe("Warm", 6, 12))
    }
}
