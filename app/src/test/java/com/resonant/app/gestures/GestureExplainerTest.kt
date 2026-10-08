package com.resonant.app.gestures

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureExplainerTest {

    private val center = InteractionZone.CENTER

    @Test
    fun finger_down_is_next_and_finger_up_is_previous() {
        // SwipeDirection.UP is the "next" gesture, and it is a finger moving down.
        assertEquals(
            "You just swiped down with two fingers, which means next.",
            explain(ResonantGesture.Swipe(center, SwipeDirection.UP))!!.spoken
        )
        assertEquals(
            "You just swiped up with two fingers, which means previous.",
            explain(ResonantGesture.Swipe(center, SwipeDirection.DOWN))!!.spoken
        )
    }

    @Test
    fun every_gesture_the_user_can_make_gets_an_answer() {
        val all = listOf(
            ResonantGesture.Swipe(center, SwipeDirection.UP),
            ResonantGesture.Swipe(center, SwipeDirection.DOWN),
            ResonantGesture.Swipe(center, SwipeDirection.LEFT),
            ResonantGesture.Swipe(center, SwipeDirection.RIGHT),
            ResonantGesture.Swipe(InteractionZone.LEFT_EDGE, SwipeDirection.UP),
            ResonantGesture.Tap(InteractionZone.CENTER),
            ResonantGesture.Tap(InteractionZone.LEFT_EDGE),
            ResonantGesture.Tap(InteractionZone.RIGHT_EDGE),
            ResonantGesture.DoubleTap(InteractionZone.LEFT_EDGE),
            ResonantGesture.LongPress(InteractionZone.CENTER),
            ResonantGesture.LongPress(InteractionZone.RIGHT_EDGE),
            ResonantGesture.HoldSpeedUp,
            ResonantGesture.HoldSpeedDown,
            ResonantGesture.ThreeFingerTap,
            ResonantGesture.ThreeFingerHold
        )
        all.forEach { assertNotNull(it.toString(), explain(it)) }
    }

    @Test
    fun mechanical_events_are_not_explained() {
        assertNull(explain(ResonantGesture.HoldStart))
        assertNull(explain(ResonantGesture.HoldEnd))
        assertNull(explain(ResonantGesture.LongPressEnd(center)))
        // A left-edge hold is the skip gesture, handled before the explainer.
        assertNull(explain(ResonantGesture.LongPress(InteractionZone.LEFT_EDGE)))
    }

    @Test
    fun different_gestures_have_different_titles() {
        val titles = listOf(
            ResonantGesture.Swipe(center, SwipeDirection.UP),
            ResonantGesture.Swipe(center, SwipeDirection.DOWN),
            ResonantGesture.Swipe(center, SwipeDirection.LEFT),
            ResonantGesture.Swipe(center, SwipeDirection.RIGHT),
            ResonantGesture.Tap(InteractionZone.CENTER),
            ResonantGesture.Tap(InteractionZone.LEFT_EDGE),
            ResonantGesture.DoubleTap(InteractionZone.LEFT_EDGE),
            ResonantGesture.LongPress(InteractionZone.RIGHT_EDGE),
            ResonantGesture.HoldSpeedUp,
            ResonantGesture.HoldSpeedDown,
            ResonantGesture.ThreeFingerTap,
            ResonantGesture.ThreeFingerHold
        ).map { explain(it)!!.title }
        assertEquals(titles.size, titles.toSet().size)
    }

    @Test
    fun dead_right_edge_tap_says_so_and_stays_silent_on_haptics() {
        val e = explain(ResonantGesture.Tap(InteractionZone.RIGHT_EDGE))!!
        assertNull(e.haptic)
        assertTrue(e.spoken.contains("does nothing"))
    }
}
