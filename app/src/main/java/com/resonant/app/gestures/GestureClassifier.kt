package com.resonant.app.gestures

import kotlin.math.abs

/**
 * The pure, Android-free decisions behind [resonantGestureDetector]: which zone a
 * touch is in, whether a finger has "moved", which way a swipe went, and when a
 * speed-drag has travelled far enough to count as one step.
 *
 * Kept separate from the pointer-input loop so it can be unit tested on the JVM
 * (see GestureClassifierTest) — the loop itself needs a device, this doesn't.
 */
object GestureClassifier {

    const val TAP_MAX_DRIFT_PX = 24f
    const val SWIPE_MIN_DISTANCE_PX = 64f

    fun zoneFor(x: Float, width: Int, leftEdgePx: Float, rightEdgePx: Float): InteractionZone = when {
        x <= leftEdgePx -> InteractionZone.LEFT_EDGE
        x >= width - rightEdgePx -> InteractionZone.RIGHT_EDGE
        else -> InteractionZone.CENTER
    }

    /** True once the finger has drifted far enough that this is no longer a tap or a press. */
    fun hasMoved(dx: Float, dy: Float): Boolean =
        abs(dx) > TAP_MAX_DRIFT_PX || abs(dy) > TAP_MAX_DRIFT_PX

    /** True when a moved touch travelled far enough to count as a swipe rather than a wobble. */
    fun isSwipe(dx: Float, dy: Float): Boolean =
        abs(dx) > SWIPE_MIN_DISTANCE_PX || abs(dy) > SWIPE_MIN_DISTANCE_PX

    /**
     * Dominant-axis swipe direction. [invertVertical] / [invertHorizontal] flip the
     * reported direction (list-navigation feel), they do not change which axis wins.
     */
    fun swipeDirection(
        dx: Float,
        dy: Float,
        invertVertical: Boolean,
        invertHorizontal: Boolean
    ): SwipeDirection =
        if (abs(dx) > abs(dy)) {
            val movedRight = dx > 0
            val right = if (invertHorizontal) !movedRight else movedRight
            if (right) SwipeDirection.RIGHT else SwipeDirection.LEFT
        } else {
            val movedDown = dy > 0
            val down = if (invertVertical) !movedDown else movedDown
            if (down) SwipeDirection.DOWN else SwipeDirection.UP
        }

    enum class HoldStep { NONE, UP, DOWN }

    /**
     * Turns accumulated vertical drag (screen pixels, y grows downward) into at most
     * one speed step. Dragging up past [stepPx] is UP (faster); down is DOWN (slower).
     */
    fun holdStep(accumulatedY: Float, stepPx: Float): HoldStep = when {
        accumulatedY <= -stepPx -> HoldStep.UP
        accumulatedY >= stepPx -> HoldStep.DOWN
        else -> HoldStep.NONE
    }
}
