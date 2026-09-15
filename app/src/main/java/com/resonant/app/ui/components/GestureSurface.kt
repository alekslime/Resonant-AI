package com.resonant.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.resonant.app.core.LocalDebugState
import com.resonant.app.gestures.ResonantGesture
import com.resonant.app.gestures.resonantGestureDetector

/**
 * Every screen that participates in the Resonant interaction model wraps its
 * content in this. It attaches the shared gesture detector and mirrors every
 * gesture into [com.resonant.app.core.DebugState] so the Debug screen always
 * reflects reality, without each screen having to remember to log it.
 */
@Composable
fun GestureSurface(
    modifier: Modifier = Modifier,
    onGesture: (ResonantGesture) -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val debugState = LocalDebugState.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .resonantGestureDetector { gesture ->
                debugState.setGesture(describeGesture(gesture))
                zoneOf(gesture)?.let { debugState.setZone(it) }
                onGesture(gesture)
            },
        content = content
    )
}

private fun zoneOf(gesture: ResonantGesture): String? = when (gesture) {
    is ResonantGesture.Tap -> gesture.zone.name
    is ResonantGesture.DoubleTap -> gesture.zone.name
    is ResonantGesture.LongPress -> gesture.zone.name
    is ResonantGesture.Swipe -> gesture.zone.name
    // Hold gestures only ever originate from the right edge — see
    // GestureManager: holdModeActive is gated on zone == RIGHT_EDGE.
    ResonantGesture.HoldStart, ResonantGesture.HoldSpeedUp,
    ResonantGesture.HoldSpeedDown, ResonantGesture.HoldEnd -> "RIGHT_EDGE"
    ResonantGesture.ThreeFingerTap, ResonantGesture.ThreeFingerHold -> "GLOBAL"
}

private fun describeGesture(gesture: ResonantGesture): String = when (gesture) {
    is ResonantGesture.Tap -> "TAP(${gesture.zone})"
    is ResonantGesture.DoubleTap -> "DOUBLE_TAP(${gesture.zone})"
    is ResonantGesture.LongPress -> "LONG_PRESS(${gesture.zone})"
    is ResonantGesture.Swipe -> "SWIPE_${gesture.direction}(${gesture.zone})"
    ResonantGesture.HoldStart -> "HOLD_START"
    ResonantGesture.HoldSpeedUp -> "HOLD_SPEED_UP"
    ResonantGesture.HoldSpeedDown -> "HOLD_SPEED_DOWN"
    ResonantGesture.HoldEnd -> "HOLD_END"
    ResonantGesture.ThreeFingerTap -> "THREE_FINGER_TAP"
    ResonantGesture.ThreeFingerHold -> "THREE_FINGER_HOLD"
}
