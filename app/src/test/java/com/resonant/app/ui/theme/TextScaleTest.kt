package com.resonant.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class TextScaleTest {

    @Test
    fun scale_stays_between_normal_and_two_and_a_half() {
        assertEquals(1f, TextScale.apply(1f, 0.5f), 0f)
        assertEquals(2.5f, TextScale.apply(2f, 3f), 0f)
        assertEquals(1.5f, TextScale.apply(1f, 1.5f), 0f)
    }

    @Test
    fun steps_change_every_quarter() {
        assertEquals(TextScale.stepOf(1.0f), TextScale.stepOf(1.2f))
        assertEquals(TextScale.stepOf(1.0f) + 1, TextScale.stepOf(1.25f))
    }

    @Test
    fun percent_is_rounded() {
        assertEquals(150, TextScale.percent(1.5f))
        assertEquals(250, TextScale.percent(2.5f))
    }
}
