package com.resonant.app.gestures

import com.resonant.app.gestures.GestureClassifier.HoldStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureClassifierTest {

    private val width = 1000
    private val edge = 48f

    @Test
    fun zones_use_inclusive_edge_boundaries() {
        assertEquals(InteractionZone.LEFT_EDGE, GestureClassifier.zoneFor(0f, width, edge, edge))
        assertEquals(InteractionZone.LEFT_EDGE, GestureClassifier.zoneFor(48f, width, edge, edge))
        assertEquals(InteractionZone.CENTER, GestureClassifier.zoneFor(49f, width, edge, edge))
        assertEquals(InteractionZone.CENTER, GestureClassifier.zoneFor(951f, width, edge, edge))
        assertEquals(InteractionZone.RIGHT_EDGE, GestureClassifier.zoneFor(952f, width, edge, edge))
        assertEquals(InteractionZone.RIGHT_EDGE, GestureClassifier.zoneFor(999f, width, edge, edge))
    }

    @Test
    fun movement_threshold_is_strictly_greater_than_drift() {
        assertFalse(GestureClassifier.hasMoved(24f, 0f))
        assertFalse(GestureClassifier.hasMoved(0f, -24f))
        assertTrue(GestureClassifier.hasMoved(24.1f, 0f))
        assertTrue(GestureClassifier.hasMoved(0f, -24.1f))
    }

    @Test
    fun a_wobble_is_not_a_swipe() {
        assertFalse(GestureClassifier.isSwipe(40f, 40f))
        assertFalse(GestureClassifier.isSwipe(64f, 0f))
        assertTrue(GestureClassifier.isSwipe(65f, 0f))
        assertTrue(GestureClassifier.isSwipe(0f, -65f))
    }

    @Test
    fun horizontal_swipe_direction_and_inversion() {
        assertEquals(
            SwipeDirection.RIGHT,
            GestureClassifier.swipeDirection(100f, 10f, invertVertical = false, invertHorizontal = false)
        )
        assertEquals(
            SwipeDirection.LEFT,
            GestureClassifier.swipeDirection(-100f, 10f, invertVertical = false, invertHorizontal = false)
        )
        assertEquals(
            SwipeDirection.LEFT,
            GestureClassifier.swipeDirection(100f, 10f, invertVertical = false, invertHorizontal = true)
        )
    }

    @Test
    fun vertical_swipe_direction_and_inversion() {
        // y grows downward: dy > 0 is a finger moving DOWN the screen.
        assertEquals(
            SwipeDirection.DOWN,
            GestureClassifier.swipeDirection(5f, 100f, invertVertical = false, invertHorizontal = false)
        )
        assertEquals(
            SwipeDirection.UP,
            GestureClassifier.swipeDirection(5f, 100f, invertVertical = true, invertHorizontal = false)
        )
        assertEquals(
            SwipeDirection.DOWN,
            GestureClassifier.swipeDirection(5f, -100f, invertVertical = true, invertHorizontal = false)
        )
    }

    @Test
    fun a_diagonal_tie_resolves_to_the_vertical_axis() {
        assertEquals(
            SwipeDirection.DOWN,
            GestureClassifier.swipeDirection(80f, 80f, invertVertical = false, invertHorizontal = false)
        )
    }

    @Test
    fun hold_drag_steps_only_once_the_step_distance_is_reached() {
        assertEquals(HoldStep.NONE, GestureClassifier.holdStep(0f, 56f))
        assertEquals(HoldStep.NONE, GestureClassifier.holdStep(-55.9f, 56f))
        assertEquals(HoldStep.UP, GestureClassifier.holdStep(-56f, 56f))
        assertEquals(HoldStep.NONE, GestureClassifier.holdStep(55.9f, 56f))
        assertEquals(HoldStep.DOWN, GestureClassifier.holdStep(56f, 56f))
    }
}
