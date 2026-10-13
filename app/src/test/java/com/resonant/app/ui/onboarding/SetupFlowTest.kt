package com.resonant.app.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupFlowTest {

    @Test
    fun steps_run_theme_font_size_speed_name() {
        assertEquals(
            listOf(SetupStep.THEME, SetupStep.FONT, SetupStep.TEXT_SIZE, SetupStep.SPEED, SetupStep.NAME),
            SetupFlow.STEPS
        )
    }

    @Test
    fun only_the_name_step_has_no_options() {
        assertFalse(SetupFlow.hasOptions(SetupStep.NAME))
        assertTrue(SetupFlow.STEPS.filter { it != SetupStep.NAME }.all { SetupFlow.hasOptions(it) })
    }

    @Test
    fun an_old_saved_scale_lands_on_the_nearest_option() {
        assertEquals(0, SetupFlow.scaleIndexOf(1f))
        assertEquals(1, SetupFlow.scaleIndexOf(1.2f))
        assertEquals(3, SetupFlow.scaleIndexOf(1.9f))
        assertEquals(4, SetupFlow.scaleIndexOf(2.5f))
    }

    @Test
    fun scale_is_spoken_as_a_percentage() {
        assertEquals("125 percent", SetupFlow.scaleLabel(1.25f))
        assertEquals("250 percent", SetupFlow.scaleLabel(2.5f))
    }

    @Test
    fun position_bar_fills_up_and_never_overflows() {
        assertEquals(0.25f, SetupFlow.position(0, 4), 0.0001f)
        assertEquals(1f, SetupFlow.position(3, 4), 0.0001f)
        assertEquals(1f, SetupFlow.position(9, 4), 0.0001f)
        assertEquals(0f, SetupFlow.position(0, 0), 0.0001f)
    }

    @Test
    fun every_step_has_a_title_and_a_prompt() {
        SetupFlow.STEPS.forEach {
            assertTrue(SetupFlow.title(it).isNotBlank())
            assertTrue(SetupFlow.prompt(it).isNotBlank())
        }
    }
}
