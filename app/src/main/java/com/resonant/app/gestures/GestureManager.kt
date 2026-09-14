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
 * ============================================================================
 * RESONANT GESTURE GRAMMAR
 * ============================================================================
 * This is the single place raw touches are turned into [ResonantGesture]s.
 * Screens never read raw pointer input directly — they attach
 * [Modifier.resonantGestureDetector] and react to the gestures it emits.
 *
 * Zones (LEFT_EDGE / RIGHT_EDGE / CENTER) are computed from the *current*
 * width of the surface the modifier is attached to, never a fixed pixel
 * value, so the model survives rotation/resizing.
 *
 * The critical design rule enforced here: a plain tap in CENTER is NOT
 * "pause" and is NOT "select" on its own — it does nothing by default,
 * because a bare tap in the middle of the screen must never collide with
 * quiz selection (double tap) or edge-based pause. Zones are detected
 * independently and never bleed into one another.
 */

private const val LONG_PRESS_MS = 500L
private const val DOUBLE_TAP_MAX_INTERVAL_MS = 300L
private const val TAP_MAX_DURATION_MS = 300L
private const val TAP_MAX_DRIFT_PX = 24f
private const val SWIPE_MIN_DISTANCE_PX = 64f

// Flip either of these if a swipe axis ever feels backwards on-device — nothing
// else in the detection logic needs to change.
private const val INVERT_VERTICAL_SWIPES = true
private const val INVERT_HORIZONTAL_SWIPES = false

private val EDGE_ZONE_WIDTH = 32.dp
private val HOLD_SPEED_STEP = 56.dp

fun Modifier.resonantGestureDetector(
    onGesture: (ResonantGesture) -> Unit
): Modifier = this.pointerInput(Unit) {

    val edgeWidthPx = EDGE_ZONE_WIDTH.toPx()
    val holdStepPx = HOLD_SPEED_STEP.toPx()

    // State that must survive across separate gestures (for double-tap correlation).
    var lastTapUpTimeMs = 0L
    var lastTapZone: InteractionZone? = null

    fun zoneFor(x: Float, width: Int): InteractionZone = when {
        x <= edgeWidthPx -> InteractionZone.LEFT_EDGE
        x >= width - edgeWidthPx -> InteractionZone.RIGHT_EDGE
        else -> InteractionZone.CENTER
    }

    awaitEachGesture {
        // awaitFirstDown() from androidx.compose.foundation.gestures is `internal`
        // and not accessible here, so the first-down wait is done manually: the
        // very first pointer event delivered inside a fresh awaitEachGesture
        // iteration is the down event that started this gesture.
        val firstEvent = awaitPointerEvent(PointerEventPass.Initial)
        val firstDown = firstEvent.changes.first { it.pressed }
        val downTimeMs = System.currentTimeMillis()
        val zone = zoneFor(firstDown.position.x, size.width)

        var maxPointerCount = 1
        var totalDrag = Offset.Zero
        var holdModeActive = false
        var holdAccumY = 0f
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

            // Left-edge hold-to-adjust-speed sub-gesture. Only engages with a single
            // finger held on the left edge without a competing swipe already underway.
            if (zone == InteractionZone.LEFT_EDGE && maxPointerCount == 1 &&
                !holdModeActive && elapsed > LONG_PRESS_MS && !moved
            ) {
                holdModeActive = true
                onGesture(ResonantGesture.HoldStart)
            }
            if (holdModeActive) {
                val rawDelta = primary?.positionChange()?.y ?: 0f
                holdAccumY += if (INVERT_VERTICAL_SWIPES) -rawDelta else rawDelta
                if (holdAccumY <= -holdStepPx) {
                    onGesture(ResonantGesture.HoldSpeedUp)
                    holdAccumY = 0f
                } else if (holdAccumY >= holdStepPx) {
                    onGesture(ResonantGesture.HoldSpeedDown)
                    holdAccumY = 0f
                }
            }

            // Long press for CENTER / RIGHT_EDGE fires as soon as the threshold is
            // crossed (standard long-press feel) rather than waiting for release.
            if (!holdModeActive && zone != InteractionZone.LEFT_EDGE && maxPointerCount == 1 &&
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
            holdModeActive -> onGesture(ResonantGesture.HoldEnd)

            longPressFired -> { /* already emitted during the gesture */ }

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
                val now = System.currentTimeMillis()
                if (lastTapZone == zone && (now - lastTapUpTimeMs) <= DOUBLE_TAP_MAX_INTERVAL_MS) {
                    onGesture(ResonantGesture.DoubleTap(zone))
                    lastTapUpTimeMs = 0L
                    lastTapZone = null
                } else {
                    onGesture(ResonantGesture.Tap(zone))
                    lastTapUpTimeMs = now
                    lastTapZone = zone
                }
            }

            else -> {
                // Long hold with drift, or a duration between tap and long-press
                // thresholds with no clean classification — intentionally ignored,
                // this is a deliberate "do nothing" outcome rather than a guess.
            }
        }
    }
}
