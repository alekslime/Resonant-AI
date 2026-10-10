package com.resonant.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.resonant.app.audio.AudioManager
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.gestures.HintEngine
import com.resonant.app.gestures.HintText
import com.resonant.app.gestures.InteractionZone
import com.resonant.app.gestures.ResonantGesture
import com.resonant.app.gestures.SwipeDirection
import com.resonant.app.gestures.resonantGestureDetector
import com.resonant.app.haptics.HapticPattern
import kotlinx.coroutines.delay

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
 *
 * It also speaks short hints when the user seems stuck (see [HintEngine]): a long quiet
 * pause, or several gestures in a row that did nothing. A gesture "did nothing" when its
 * handler played no haptic cue (every real action does), or only an error or edge bump.
 * [hint] is the sentence to speak; the default fits a menu. [hints] = false turns it off
 * for screens that speak for themselves (Chat has a live microphone, the tutorial has its
 * own prompts).
 */
@Composable
fun GestureSurface(
    modifier: Modifier = Modifier,
    onGesture: (ResonantGesture) -> Unit,
    twoFingerSwipe: Boolean = false,
    ignoreChildTaps: Boolean = false,
    hints: Boolean = true,
    hint: String? = null,
    onPinch: ((Float) -> Unit)? = null,
    rewind: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val debugState = LocalDebugState.current
    val haptics = LocalHapticManager.current
    val audio = LocalAudioManager.current
    val engine = remember { HintEngine(SystemClock.uptimeMillis()) }
    val hintText by rememberUpdatedState(hint ?: HintText.general(twoFingerSwipe))

    // The detector's pointerInput block is started once and keeps running across
    // recompositions, so a plain `onGesture` captured inside it would go stale —
    // it would keep closing over the first composition's locals. Reading through
    // rememberUpdatedState always reaches the latest handler.
    val latestOnGesture by rememberUpdatedState(onGesture)

    val latestOnPinch by rememberUpdatedState(onPinch)
    val pinchHandler: ((Float) -> Unit)? = remember(engine, onPinch != null) {
        if (onPinch == null) null else { factor: Float ->
            engine.noteGesture(SystemClock.uptimeMillis())
            latestOnPinch?.invoke(factor)
        }
    }

    // Rewind (four fingers dragged left, or the TalkBack action): a tick per 5 s step while the
    // fingers move, and the audio jumps back when they lift. Nothing to go back to = edge bump.
    val rewindHandler: ((Int, Boolean) -> Unit)? = remember(audio, haptics, engine, rewind) {
        if (!rewind) null else { steps: Int, release: Boolean ->
            if (!release) {
                haptics.play(HapticPattern.PREVIOUS)
            } else {
                engine.noteGesture(SystemClock.uptimeMillis())
                if (!audio.rewind(steps * AudioManager.REWIND_STEP_SECONDS)) haptics.play(HapticPattern.EDGE)
            }
        }
    }

    val dispatch: (ResonantGesture) -> Unit = remember(debugState, haptics) {
        { gesture ->
            debugState.setGesture(describeGesture(gesture))
            zoneOf(gesture)?.let { debugState.setZone(it) }
            // Tactile confirmation that hold-to-adjust-speed is now live. Without
            // it there is no cue until the first speed step, so a user cannot tell
            // "still waiting for the hold" from "holding, ready to drag".
            if (gesture == ResonantGesture.HoldStart) haptics.play(HapticPattern.HOLD_ENGAGED)
            val before = haptics.playCount
            latestOnGesture(gesture)
            val judged = gesture is ResonantGesture.Swipe || gesture is ResonantGesture.Tap ||
                gesture is ResonantGesture.LongPress
            val last = haptics.lastPattern.value
            val didNothing = haptics.playCount == before ||
                last == HapticPattern.ERROR || last == HapticPattern.EDGE
            val now = SystemClock.uptimeMillis()
            if (judged && didNothing) engine.noteMiss(now) else engine.noteGesture(now)
        }
    }

    if (hints) {
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(engine, lifecycle) {
            // Only while the app is on screen, so a hint is never spoken from the background.
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                engine.resetClock(SystemClock.uptimeMillis())
                while (true) {
                    delay(1_000)
                    val busy = audio.isSpeaking.value || audio.isAnnouncing.value || audio.isPaused.value
                    val reason = engine.check(SystemClock.uptimeMillis(), silent = !busy) ?: continue
                    audio.announce(HintText.say(reason, hintText))
                }
            }
        }
    }

    val actions = remember(dispatch, pinchHandler, rewindHandler) { talkBackActions(dispatch, pinchHandler, rewindHandler) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .resonantGestureDetector(twoFingerSwipe, ignoreChildTaps, pinchHandler, rewindHandler) { gesture -> dispatch(gesture) }
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
private fun talkBackActions(
    dispatch: (ResonantGesture) -> Unit,
    pinch: ((Float) -> Unit)?,
    rewind: ((Int, Boolean) -> Unit)?
): List<CustomAccessibilityAction> {
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
    ) + (if (rewind == null) emptyList() else listOf(
        CustomAccessibilityAction("Rewind 10 seconds") { rewind(2, true); true }
    )) + if (pinch == null) emptyList() else listOf(
        CustomAccessibilityAction("Larger text") { pinch(1.25f); true },
        CustomAccessibilityAction("Smaller text") { pinch(0.8f); true }
    )
}

private fun zoneOf(gesture: ResonantGesture): String? = when (gesture) {
    is ResonantGesture.Tap -> gesture.zone.name
    is ResonantGesture.DoubleTap -> gesture.zone.name
    is ResonantGesture.LongPress -> gesture.zone.name
    is ResonantGesture.LongPressEnd -> gesture.zone.name
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
    is ResonantGesture.LongPressEnd -> "LONG_PRESS_END(${gesture.zone})"
    is ResonantGesture.Swipe -> "SWIPE_${gesture.direction}(${gesture.zone})"
    ResonantGesture.HoldStart -> "HOLD_START"
    ResonantGesture.HoldSpeedUp -> "HOLD_SPEED_UP"
    ResonantGesture.HoldSpeedDown -> "HOLD_SPEED_DOWN"
    ResonantGesture.HoldEnd -> "HOLD_END"
    ResonantGesture.ThreeFingerTap -> "THREE_FINGER_TAP"
    ResonantGesture.ThreeFingerHold -> "THREE_FINGER_HOLD"
}
