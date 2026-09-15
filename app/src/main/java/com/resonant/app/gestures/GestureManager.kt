package com.resonant.app.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * RESONANT GESTURE GRAMMAR
 *
 * LEFT EDGE  — tap = pause/resume, double-tap = repeat current
 * CENTER     — swipe ↑↓←→ = navigate, tap = select/confirm
 * RIGHT EDGE — hold + drag ↑ = faster / ↓ = slower, long press = back
 * ANYWHERE   — three-finger tap = repeat last, three-finger hold = orientation
 *
 * No double-tap in CENTER. No hold on LEFT EDGE.
 * Right edge tap is a dead zone (no accidental triggers).
 */

private const val LONG_PRESS_MS = 500L
private const val DOUBLE_TAP_MAX_INTERVAL_MS = 300L
private const val TAP_MAX_DURATION_MS = 300L
private const val TAP_MAX_DRIFT_PX = 24f
private const val SWIPE_MIN_DISTANCE_PX = 64f

private const val INVERT_VERTICAL_SWIPES = true
private const val INVERT_HORIZONTAL_SWIPES = false

/**
 * Speed drag is deliberately NOT tied to [INVERT_VERTICAL_SWIPES]. Swipe
 * inversion is about list navigation feel; the speed drag is a physical
 * metaphor — the finger is a slider. Drag UP = faster, drag DOWN = slower.
 * Flip this only if user testing says the slider itself should invert.
 */
private const val INVERT_SPEED_DRAG = false

private val LEFT_EDGE_WIDTH = 48.dp   // wider = safer from accidental center triggers
private val RIGHT_EDGE_WIDTH = 48.dp
private val HOLD_SPEED_STEP = 56.dp

fun Modifier.resonantGestureDetector(
    onGesture: (ResonantGesture) -> Unit
): Modifier = this.pointerInput(Unit) {

    val leftEdgePx = LEFT_EDGE_WIDTH.toPx()
    val rightEdgePx = RIGHT_EDGE_WIDTH.toPx()
    val holdStepPx = HOLD_SPEED_STEP.toPx()

    var lastTapUpTimeMs = 0L
    var lastTapZone: InteractionZone? = null

    fun zoneFor(x: Float, width: Int): InteractionZone = when {
        x <= leftEdgePx -> InteractionZone.LEFT_EDGE
        x >= width - rightEdgePx -> InteractionZone.RIGHT_EDGE
        else -> InteractionZone.CENTER
    }

    awaitEachGesture {
        val firstEvent = awaitPointerEvent(PointerEventPass.Initial)
        val firstDown = firstEvent.changes.first { it.pressed }
        val downTimeMs = System.currentTimeMillis()
        val zone = zoneFor(firstDown.position.x, size.width)

        var maxPointerCount = 1
        var totalDrag = Offset.Zero
        var holdModeActive = false
        var holdAccumY = 0f
        var holdDidStep = false
        var longPressFired = false

        while (true) {
            val event = awaitPointerEvent()
            val changes = event.changes
            val pressedCount = changes.count { it.pressed }
            if (pressedCount > maxPointerCount) maxPointerCount = pressedCount

            val primary = changes.firstOrNull { it.id == firstDown.id }
            if (primary != null && primary.pressed) {
                val delta = primary.positionChange()
                totalDrag += delta
                primary.consume()
            }

            val elapsed = System.currentTimeMillis() - downTimeMs
            val moved = abs(totalDrag.x) > TAP_MAX_DRIFT_PX || abs(totalDrag.y) > TAP_MAX_DRIFT_PX

            // Right-edge hold-to-adjust-speed.
            // We check !moved only at activation time so the user can press and
            // hold briefly before dragging. Once holdModeActive is true, drag is
            // the whole point — don't cancel it because totalDrag grew.
            val rightEdgeMoved = abs(totalDrag.x) > TAP_MAX_DRIFT_PX || abs(totalDrag.y) > TAP_MAX_DRIFT_PX
            if (zone == InteractionZone.RIGHT_EDGE && maxPointerCount == 1 &&
                !holdModeActive && elapsed > LONG_PRESS_MS && !rightEdgeMoved
            ) {
                holdModeActive = true
                onGesture(ResonantGesture.HoldStart)
            }
            if (holdModeActive) {
                // positionChange() was already consumed above for totalDrag,
                // so re-read from the change's current vs previous position instead.
                val rawDeltaY = primary?.let {
                    it.position.y - it.previousPosition.y
                } ?: 0f
                holdAccumY += if (INVERT_SPEED_DRAG) -rawDeltaY else rawDeltaY
                if (holdAccumY <= -holdStepPx) {
                    onGesture(ResonantGesture.HoldSpeedUp)
                    holdAccumY = 0f
                    holdDidStep = true
                } else if (holdAccumY >= holdStepPx) {
                    onGesture(ResonantGesture.HoldSpeedDown)
                    holdAccumY = 0f
                    holdDidStep = true
                }
            }

            // Long press: CENTER and LEFT_EDGE only (right edge hold is speed mode above)
            if (!holdModeActive && zone != InteractionZone.RIGHT_EDGE && maxPointerCount == 1 &&
                !longPressFired && elapsed > LONG_PRESS_MS && !moved
            ) {
                longPressFired = true
                onGesture(ResonantGesture.LongPress(zone))
            }

            if (changes.all { !it.pressed }) break
        }

        val durationMs = System.currentTimeMillis() - downTimeMs
        val moved = abs(totalDrag.x) > TAP_MAX_DRIFT_PX || abs(totalDrag.y) > TAP_MAX_DRIFT_PX

        when {
            // A right-edge hold that never actually stepped the speed (the user
            // pressed and held still, without dragging past the step threshold)
            // is not a speed adjustment — it's a long press. Only emit HoldEnd
            // when a HoldSpeedUp/Down actually fired; otherwise resolve it as the
            // LongPress(RIGHT_EDGE) every screen is listening for.
            holdModeActive -> {
                if (holdDidStep) {
                    onGesture(ResonantGesture.HoldEnd)
                } else {
                    onGesture(ResonantGesture.LongPress(InteractionZone.RIGHT_EDGE))
                }
            }

            longPressFired -> { /* already emitted */ }

            maxPointerCount >= 3 -> {
                if (durationMs > LONG_PRESS_MS) {
                    onGesture(ResonantGesture.ThreeFingerHold)
                } else {
                    onGesture(ResonantGesture.ThreeFingerTap)
                }
            }

            moved -> {
                val direction = if (abs(totalDrag.x) > abs(totalDrag.y)) {
                    val movedRight = totalDrag.x > 0
                    val right = if (INVERT_HORIZONTAL_SWIPES) !movedRight else movedRight
                    if (right) SwipeDirection.RIGHT else SwipeDirection.LEFT
                } else {
                    val movedDown = totalDrag.y > 0
                    val down = if (INVERT_VERTICAL_SWIPES) !movedDown else movedDown
                    if (down) SwipeDirection.DOWN else SwipeDirection.UP
                }
                if (abs(totalDrag.x) > SWIPE_MIN_DISTANCE_PX || abs(totalDrag.y) > SWIPE_MIN_DISTANCE_PX) {
                    onGesture(ResonantGesture.Swipe(zone, direction))
                }
            }

            durationMs <= TAP_MAX_DURATION_MS -> {
                // Right edge tap is intentionally dead — no accidental triggers
                if (zone == InteractionZone.RIGHT_EDGE) return@awaitEachGesture

                val now = System.currentTimeMillis()
                // Double-tap only valid on LEFT EDGE (repeat current)
                if (zone == InteractionZone.LEFT_EDGE &&
                    lastTapZone == InteractionZone.LEFT_EDGE &&
                    (now - lastTapUpTimeMs) <= DOUBLE_TAP_MAX_INTERVAL_MS
                ) {
                    onGesture(ResonantGesture.DoubleTap(zone))
                    lastTapUpTimeMs = 0L
                    lastTapZone = null
                } else {
                    onGesture(ResonantGesture.Tap(zone))
                    lastTapUpTimeMs = now
                    lastTapZone = zone
                }
            }

            else -> { /* unclassified — ignore */ }
        }
    }
}
