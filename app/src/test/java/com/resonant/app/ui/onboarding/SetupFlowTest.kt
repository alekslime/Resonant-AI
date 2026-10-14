package com.resonant.app.ui.onboarding

import com.resonant.app.gestures.InteractionZone
import com.resonant.app.gestures.ResonantGesture
import com.resonant.app.gestures.SwipeDirection
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

    // ---- gestures ----

    private fun act(g: ResonantGesture, step: SetupStep? = SetupStep.THEME) = SetupGestures.resolve(g, step)

    @Test
    fun swipe_left_and_right_in_the_centre_move_through_options() {
        assertEquals(SetupAction.Move(+1), act(ResonantGesture.Swipe(InteractionZone.CENTER, SwipeDirection.RIGHT)))
        assertEquals(SetupAction.Move(-1), act(ResonantGesture.Swipe(InteractionZone.CENTER, SwipeDirection.LEFT)))
    }

    @Test
    fun vertical_and_edge_swipes_do_nothing() {
        assertEquals(SetupAction.None, act(ResonantGesture.Swipe(InteractionZone.CENTER, SwipeDirection.UP)))
        assertEquals(SetupAction.None, act(ResonantGesture.Swipe(InteractionZone.CENTER, SwipeDirection.DOWN)))
        assertEquals(SetupAction.None, act(ResonantGesture.Swipe(InteractionZone.LEFT_EDGE, SwipeDirection.RIGHT)))
        assertEquals(SetupAction.None, act(ResonantGesture.Swipe(InteractionZone.RIGHT_EDGE, SwipeDirection.LEFT)))
    }

    @Test
    fun on_the_name_step_a_swipe_skips_instead_of_moving() {
        assertEquals(
            SetupAction.SkipName,
            act(ResonantGesture.Swipe(InteractionZone.CENTER, SwipeDirection.RIGHT), SetupStep.NAME)
        )
        assertEquals(
            SetupAction.SkipName,
            act(ResonantGesture.Swipe(InteractionZone.CENTER, SwipeDirection.LEFT), SetupStep.NAME)
        )
    }

    @Test
    fun tap_in_the_centre_keeps_and_other_taps_do_nothing() {
        assertEquals(SetupAction.Keep, act(ResonantGesture.Tap(InteractionZone.CENTER)))
        assertEquals(SetupAction.Keep, act(ResonantGesture.Tap(InteractionZone.CENTER), SetupStep.NAME))
        assertEquals(SetupAction.None, act(ResonantGesture.Tap(InteractionZone.LEFT_EDGE)))
        assertEquals(SetupAction.None, act(ResonantGesture.Tap(InteractionZone.RIGHT_EDGE)))
    }

    @Test
    fun left_edge_hold_skips_everything_even_during_the_intro() {
        assertEquals(SetupAction.SkipAll, act(ResonantGesture.LongPress(InteractionZone.LEFT_EDGE)))
        assertEquals(SetupAction.SkipAll, act(ResonantGesture.LongPress(InteractionZone.LEFT_EDGE), step = null))
    }

    @Test
    fun right_edge_hold_goes_back() {
        assertEquals(SetupAction.Back, act(ResonantGesture.LongPress(InteractionZone.RIGHT_EDGE)))
        assertEquals(SetupAction.None, act(ResonantGesture.LongPress(InteractionZone.CENTER)))
    }

    @Test
    fun nothing_but_skipping_works_before_the_first_step_opens() {
        val intro: SetupStep? = null
        assertEquals(SetupAction.None, act(ResonantGesture.Tap(InteractionZone.CENTER), intro))
        assertEquals(SetupAction.None, act(ResonantGesture.Swipe(InteractionZone.CENTER, SwipeDirection.RIGHT), intro))
        assertEquals(SetupAction.None, act(ResonantGesture.LongPress(InteractionZone.RIGHT_EDGE), intro))
        assertEquals(SetupAction.None, act(ResonantGesture.ThreeFingerHold, intro))
    }

    @Test
    fun speed_hold_start_and_end_are_ignored_and_three_finger_hold_says_where_you_are() {
        assertEquals(SetupAction.None, act(ResonantGesture.HoldStart))
        assertEquals(SetupAction.None, act(ResonantGesture.HoldEnd))
        assertEquals(SetupAction.None, act(ResonantGesture.HoldSpeedUp))
        assertEquals(SetupAction.WhereAmI, act(ResonantGesture.ThreeFingerHold))
    }
}
