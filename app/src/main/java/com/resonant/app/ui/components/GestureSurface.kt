package com.resonant.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.gestures.InteractionZone
import com.resonant.app.gestures.ResonantGesture
import com.resonant.app.gestures.SwipeDirection
import com.resonant.app.gestures.resonantGestureDetector
import com.resonant.app.haptics.HapticPattern

/**
 * Every screen that participates in the Resonant interaction model wraps its
 * content in this. It attaches the shared gesture detector and mirrors every
 * gesture into [com.resonant.app.core.DebugState] so the Debug screen always
 * reflects reality, without each screen having to remember to log it.
 *
 * It also exposes the same grammar as TalkBack custom actions. Raw touch
 * gestures cannot reach the detector while TalkBack is running (TalkBack owns
 * the touch stream), but the Actions menu can — and because every screen speaks
 * the identical [ResonantGesture] vocabulary, mapping the menu entries onto
 * synthetic gestures here gives every screen a screen-reader path for free.
 */
@Composable
fun GestureSurface(
    modifier: Modifier = Modifier,
    onGesture: (ResonantGesture) -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val debugState = LocalDebugState.current
    val haptics = LocalHapticManager.current

    // The detector's pointerInput block is started once and keeps running across
    // recompositions, so a plain `onGesture` captured inside it would go stale —
    // it would keep closing over the first composition's locals. Reading through
    // rememberUpdatedState always reaches the latest handler.
    val latestOnGesture by rememberUpdatedState(onGesture)

    val dispatch: (ResonantGesture) -> Unit = remember(debugState, haptics) {
        { gesture ->
            debugState.setGesture(describeGesture(gesture))
            zoneOf(gesture)?.let { debugState.setZone(it) }
            // Tactile confirmation that hold-to-adjust-speed is now live. Without
            // it there is no cue until the first speed step, so a user cannot tell
            // "still waiting for the hold" from "holding, ready to drag".
            if (gesture == ResonantGesture.HoldStart) haptics.play(HapticPattern.HOLD_ENGAGED)
            latestOnGesture(gesture)
        }
    }

    val actions = remember(dispatch) { talkBackActions(dispatch) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .resonantGestureDetector { gesture -> dispatch(gesture) }
            .semantics(mergeDescendants = true) { customActions = actions },
        content = content
    )
}

/**
 * The Actions-menu equivalents of the gesture grammar. Labels are short verbs —
 * TalkBack reads them aloud in a menu, so brevity matters more than grammar.
 * Directions are the semantic ones screens already switch on (UP = next item,
 * DOWN = previous), not physical finger directions.
 */
private fun talkBackActions(dispatch: (ResonantGesture) -> Unit): List<CustomAccessibilityAction> {
    fun action(label: String, gesture: ResonantGesture) =
        CustomAccessibilityAction(label) { dispatch(gesture); true }

    return listOf(
        action("Next", ResonantGesture.Swipe(InteractionZone.CENTER, SwipeDirection.UP)),
        action("Previous", ResonantGesture.Swipe(InteractionZone.CENTER, SwipeDirection.DOWN)),
        action("Select", ResonantGesture.Tap(InteractionZone.CENTER)),
        action("Continue", ResonantGesture.Swipe(InteractionZone.CENTER, SwipeDirection.RIGHT)),
        action("Back", ResonantGesture.Swipe(InteractionZone.CENTER, SwipeDirection.LEFT)),
        action("Leave screen", ResonantGesture.LongPress(InteractionZone.RIGHT_EDGE)),
        action("Pause or resume", ResonantGesture.Tap(InteractionZone.LEFT_EDGE)),
        action("Repeat", ResonantGesture.ThreeFingerTap),
        action("Where am I", ResonantGesture.ThreeFingerHold),
        action("Faster speech", ResonantGesture.HoldSpeedUp),
        action("Slower speech", ResonantGesture.HoldSpeedDown)
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
