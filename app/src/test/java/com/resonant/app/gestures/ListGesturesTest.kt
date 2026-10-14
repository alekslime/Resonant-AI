package com.resonant.app.gestures

import com.resonant.app.gestures.InteractionZone.CENTER
import com.resonant.app.gestures.InteractionZone.LEFT_EDGE
import com.resonant.app.gestures.InteractionZone.RIGHT_EDGE
import com.resonant.app.gestures.ResonantGesture.DoubleTap
import com.resonant.app.gestures.ResonantGesture.LongPress
import com.resonant.app.gestures.ResonantGesture.Swipe
import com.resonant.app.gestures.ResonantGesture.Tap
import org.junit.Assert.assertEquals
import org.junit.Test

class ListGesturesTest {

    private fun r(g: ResonantGesture) = ListGestures.resolve(g)

    @Test
    fun vertical_swipes_in_the_centre_move_focus() {
        // The detector already flips the axis: a finger moving down arrives as UP and means next.
        assertEquals(ListAction.Next, r(Swipe(CENTER, SwipeDirection.UP)))
        assertEquals(ListAction.Previous, r(Swipe(CENTER, SwipeDirection.DOWN)))
    }

    @Test
    fun horizontal_swipes_and_edge_swipes_do_nothing_on_a_list() {
        assertEquals(ListAction.None, r(Swipe(CENTER, SwipeDirection.LEFT)))
        assertEquals(ListAction.None, r(Swipe(CENTER, SwipeDirection.RIGHT)))
        for (zone in listOf(LEFT_EDGE, RIGHT_EDGE)) {
            assertEquals(ListAction.None, r(Swipe(zone, SwipeDirection.UP)))
            assertEquals(ListAction.None, r(Swipe(zone, SwipeDirection.DOWN)))
        }
    }

    @Test
    fun taps_by_zone() {
        assertEquals(ListAction.Activate, r(Tap(CENTER)))
        assertEquals(ListAction.TogglePause, r(Tap(LEFT_EDGE)))
        assertEquals(ListAction.None, r(Tap(RIGHT_EDGE)))
    }

    @Test
    fun double_tap_repeats_only_on_the_left_edge() {
        assertEquals(ListAction.Repeat, r(DoubleTap(LEFT_EDGE)))
        assertEquals(ListAction.None, r(DoubleTap(CENTER)))
        assertEquals(ListAction.None, r(DoubleTap(RIGHT_EDGE)))
    }

    @Test
    fun holding_the_right_edge_goes_back_and_other_holds_do_not() {
        assertEquals(ListAction.Back, r(LongPress(RIGHT_EDGE)))
        assertEquals(ListAction.None, r(LongPress(CENTER)))
        assertEquals(ListAction.None, r(LongPress(LEFT_EDGE)))
        assertEquals(ListAction.None, r(ResonantGesture.LongPressEnd(CENTER)))
    }

    @Test
    fun speed_drag_and_its_start_and_end() {
        assertEquals(ListAction.FasterSpeech, r(ResonantGesture.HoldSpeedUp))
        assertEquals(ListAction.SlowerSpeech, r(ResonantGesture.HoldSpeedDown))
        assertEquals(ListAction.None, r(ResonantGesture.HoldStart))
        assertEquals(ListAction.None, r(ResonantGesture.HoldEnd))
    }

    @Test
    fun three_finger_gestures() {
        assertEquals(ListAction.Repeat, r(ResonantGesture.ThreeFingerTap))
        assertEquals(ListAction.WhereAmI, r(ResonantGesture.ThreeFingerHold))
    }
}
