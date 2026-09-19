package com.resonant.app.gestures

import android.os.SystemClock
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import com.resonant.app.ui.theme.EdgeZoneWidth

/**
 * RESONANT GESTURE GRAMMAR
 *
 * LEFT EDGE  — tap = pause/resume, double-tap = repeat current
 * CENTER     — swipe ↑↓←→ = navigate, tap = select/confirm
 * RIGHT EDGE — hold + drag ↑ = faster / ↓ = slower, hold without dragging = back
 * ANYWHERE   — three-finger tap = repeat last, three-finger hold = orientation
 *
 * No double-tap in CENTER. No hold on LEFT EDGE.
 * Right edge tap is a dead zone (no accidental triggers).
 *
 * TIMING: holds are decided by a real timeout, not by waiting for the next
 * pointer event. A perfectly still finger produces few or no events, so an
 * event-driven check would only notice the 500 ms mark on release — which is
 * too late for a user who is relying on the tactile "hold engaged" cue
 * (see HapticPattern.HOLD_ENGAGED, played by GestureSurface on HoldStart).
 * Time comes from [SystemClock.uptimeMillis], the same clock as
 * PointerInputChange.uptimeMillis.
 */

private const val LONG_PRESS_MS = 500L
private const val DOUBLE_TAP_MAX_INTERVAL_MS = 300L
private const val TAP_MAX_DURATION_MS = 300L

private const val INVERT_VERTICAL_SWIPES = true
private const val INVERT_HORIZONTAL_SWIPES = false

/**
 * Speed drag is deliberately NOT tied to [INVERT_VERTICAL_SWIPES]. Swipe
 * inversion is about list navigation feel; the speed drag is a physical
 * metaphor — the finger is a slider. Drag UP = faster, drag DOWN = slower.
 * Flip this only if user testing says the slider itself should invert.
 */
private const val INVERT_SPEED_DRAG = false

// Matches ScreenHorizontalPadding's visual margin, and wider than the old
// 48.dp — more forgiving to hit reliably without precise aim.
private val LEFT_EDGE_WIDTH = EdgeZoneWidth
private val RIGHT_EDGE_WIDTH = EdgeZoneWidth
private val HOLD_SPEED_STEP = 56.dp

fun Modifier.resonantGestureDetector(
    onGesture: (ResonantGesture) -> Unit
): Modifier = this.pointerInput(Unit) {

    val leftEdgePx = LEFT_EDGE_WIDTH.toPx()
    val rightEdgePx = RIGHT_EDGE_WIDTH.toPx()
    val holdStepPx = HOLD_SPEED_STEP.toPx()

    var lastTapUpTimeMs = 0L
    var lastTapZone: InteractionZone? = null

    awaitEachGesture {
        val firstEvent = awaitPointerEvent(PointerEventPass.Initial)
        val firstDown = firstEvent.changes.first { it.pressed }
        val downTimeMs = firstDown.uptimeMillis
        val zone = GestureClassifier.zoneFor(firstDown.position.x, size.width, leftEdgePx, rightEdgePx)

        var maxPointerCount = 1
        var pressedNow = 1
        var totalDrag = Offset.Zero
        var thresholdHandled = false
        var holdModeActive = false
        var holdAccumY = 0f
        var holdDidStep = false
        var longPressFired = false
        var threeFingerHoldFired = false

        // Runs exactly once, when the finger(s) have been down for LONG_PRESS_MS —
        // either because a pointer event arrived after the mark or, for a
        // perfectly still finger, because the timeout below expired.
        fun handleHoldThreshold() {
            thresholdHandled = true
            val movedNow = GestureClassifier.hasMoved(totalDrag.x, totalDrag.y)
            when {
                pressedNow >= 3 -> {
                    threeFingerHoldFired = true
                    onGesture(ResonantGesture.ThreeFingerHold)
                }
                maxPointerCount > 1 || movedNow -> { /* not a hold */ }
                zone == InteractionZone.RIGHT_EDGE -> {
                    // Speed mode. Activation requires the finger to be still at the
                    // threshold; once active, drag is the whole point.
                    holdModeActive = true
                    onGesture(ResonantGesture.HoldStart)
                }
                else -> {
                    longPressFired = true
                    onGesture(ResonantGesture.LongPress(zone))
                }
            }
        }

        while (true) {
            val event = if (thresholdHandled) {
                awaitPointerEvent()
            } else {
                val wait = downTimeMs + LONG_PRESS_MS - SystemClock.uptimeMillis()
                if (wait <= 0L) null else withTimeoutOrNull(wait) { awaitPointerEvent() }
            }

            if (event == null) {
                // Timed out with no pointer event: the finger is holding still.
                handleHoldThreshold()
                continue
            }

            val changes = event.changes
            pressedNow = changes.count { it.pressed }
            if (pressedNow > maxPointerCount) maxPointerCount = pressedNow

            val primary = changes.firstOrNull { it.id == firstDown.id }
            if (primary != null && primary.pressed) {
                totalDrag += primary.positionChange()
                primary.consume()
            }

            // Speed drag. Re-read from position - previousPosition rather than
            // positionChange(), which was already consumed above.
            if (holdModeActive) {
                val rawDeltaY = primary?.let { it.position.y - it.previousPosition.y } ?: 0f
                holdAccumY += if (INVERT_SPEED_DRAG) -rawDeltaY else rawDeltaY
                when (GestureClassifier.holdStep(holdAccumY, holdStepPx)) {
                    GestureClassifier.HoldStep.UP -> {
                        onGesture(ResonantGesture.HoldSpeedUp)
                        holdAccumY = 0f
                        holdDidStep = true
                    }
                    GestureClassifier.HoldStep.DOWN -> {
                        onGesture(ResonantGesture.HoldSpeedDown)
                        holdAccumY = 0f
                        holdDidStep = true
                    }
                    GestureClassifier.HoldStep.NONE -> {}
                }
            }

            if (!thresholdHandled && SystemClock.uptimeMillis() - downTimeMs >= LONG_PRESS_MS) {
                handleHoldThreshold()
            }

            if (changes.all { !it.pressed }) break
        }

        val durationMs = SystemClock.uptimeMillis() - downTimeMs
        val moved = GestureClassifier.hasMoved(totalDrag.x, totalDrag.y)

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

            longPressFired || threeFingerHoldFired -> { /* already emitted */ }

            maxPointerCount >= 3 -> {
                if (durationMs > LONG_PRESS_MS) {
                    onGesture(ResonantGesture.ThreeFingerHold)
                } else {
                    onGesture(ResonantGesture.ThreeFingerTap)
                }
            }

            moved -> {
                val direction = GestureClassifier.swipeDirection(
                    totalDrag.x, totalDrag.y,
                    invertVertical = INVERT_VERTICAL_SWIPES,
                    invertHorizontal = INVERT_HORIZONTAL_SWIPES
                )
                if (GestureClassifier.isSwipe(totalDrag.x, totalDrag.y)) {
                    onGesture(ResonantGesture.Swipe(zone, direction))
                }
            }

            durationMs <= TAP_MAX_DURATION_MS -> {
                // Right edge tap is intentionally dead — no accidental triggers
                if (zone == InteractionZone.RIGHT_EDGE) return@awaitEachGesture

                val now = SystemClock.uptimeMillis()
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
