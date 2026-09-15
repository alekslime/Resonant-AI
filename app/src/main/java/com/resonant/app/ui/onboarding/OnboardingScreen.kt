package com.resonant.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.gestures.InteractionZone
import com.resonant.app.gestures.ResonantGesture
import com.resonant.app.gestures.SwipeDirection
import com.resonant.app.haptics.HapticPattern
import com.resonant.app.ui.components.GestureSurface
import com.resonant.app.ui.components.ResonantSurface
import com.resonant.app.ui.theme.ResonantBlack
import com.resonant.app.ui.theme.ResonantWhite
import kotlinx.coroutines.delay

/**
 * One step of the tutorial. [matches] decides whether a gesture completes the
 * step; everything else is treated as a miss and re-prompted, so the user can
 * explore without getting stuck.
 */
private data class GestureLesson(
    val label: String,
    val spoken: String,
    val hint: String,
    val matches: (ResonantGesture) -> Boolean
)

private val lessons = listOf(
    GestureLesson(
        label = "Swipe up",
        spoken = "First, moving through a list. Swipe up anywhere in the middle of the screen to go to the next item.",
        hint = "Swipe up in the middle of the screen.",
        matches = { it is ResonantGesture.Swipe && it.zone == InteractionZone.CENTER && it.direction == SwipeDirection.UP }
    ),
    GestureLesson(
        label = "Swipe down",
        spoken = "Now the other way. Swipe down in the middle to go back to the previous item.",
        hint = "Swipe down in the middle of the screen.",
        matches = { it is ResonantGesture.Swipe && it.zone == InteractionZone.CENTER && it.direction == SwipeDirection.DOWN }
    ),
    GestureLesson(
        label = "Tap the middle",
        spoken = "To open whatever you are on, tap once in the middle of the screen.",
        hint = "Tap once in the middle of the screen.",
        matches = { it is ResonantGesture.Tap && it.zone == InteractionZone.CENTER }
    ),
    GestureLesson(
        label = "Tap the left edge",
        spoken = "The left edge controls speech. Tap the left edge of the screen to pause or resume.",
        hint = "Tap the strip along the left edge.",
        matches = { it is ResonantGesture.Tap && it.zone == InteractionZone.LEFT_EDGE }
    ),
    GestureLesson(
        label = "Double-tap the left edge",
        spoken = "Missed something? Double-tap the left edge to hear it again.",
        hint = "Tap the left edge twice, quickly.",
        matches = { it is ResonantGesture.DoubleTap && it.zone == InteractionZone.LEFT_EDGE }
    ),
    GestureLesson(
        label = "Drag up on the right edge",
        spoken = "The right edge is a speed slider. Press and hold the right edge, then drag up to speak faster. Drag down to slow back down.",
        hint = "Hold the right edge, then drag your finger upward.",
        matches = { it == ResonantGesture.HoldSpeedUp }
    ),
    GestureLesson(
        label = "Hold the right edge",
        spoken = "To go back, press and hold the right edge without dragging.",
        hint = "Press and hold the right edge, and keep still.",
        matches = { it is ResonantGesture.LongPress && it.zone == InteractionZone.RIGHT_EDGE }
    ),
    GestureLesson(
        label = "Three-finger hold",
        spoken = "Last one. Lost your place? Hold three fingers anywhere on the screen and Resonant will tell you where you are.",
        hint = "Press and hold with three fingers anywhere.",
        matches = { it == ResonantGesture.ThreeFingerHold }
    )
)

private const val INTRO =
    "Welcome to Resonant. Resonant is used entirely by touch and sound — there is nothing you need to see. " +
        "I will teach you eight gestures, one at a time. Try each one and I will confirm it. " +
        "To skip this tutorial, press and hold the left edge of the screen."

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current

    var step by remember { mutableIntStateOf(-1) }   // -1 = intro still playing
    var misses by remember { mutableIntStateOf(0) }
    var completing by remember { mutableStateOf(false) }
    val onFinishedNow by rememberUpdatedState(onFinished)

    // Intro plays once, then the first lesson starts. The delay is a deliberate
    // simplification: the tutorial is scripted speech, not queue playback, so it
    // doesn't need the utterance-completion plumbing the rest of the app uses.
    LaunchedEffect(Unit) {
        debug.setScreen("Onboarding")
        audio.announce(INTRO)
        delay(13_000)
        step = 0
        audio.announce(lessons[0].spoken)
    }

    // Re-prompt with the short hint if the user has been silent or wrong a while.
    LaunchedEffect(step, misses, completing) {
        if (step < 0 || step >= lessons.size || completing) return@LaunchedEffect
        delay(12_000)
        audio.announce(lessons[step].hint)
    }

    // Let the closing line actually finish before Home flushes the TTS queue.
    LaunchedEffect(completing) {
        if (!completing) return@LaunchedEffect
        delay(6_000)
        onFinishedNow()
    }

    fun advance() {
        val next = step + 1
        if (next >= lessons.size) {
            haptics.play(HapticPattern.CORRECT)
            audio.announce(
                "That's all of them. You can hear this tutorial again any time from Settings. Taking you to the home screen."
            )
            step = lessons.size
            completing = true
        } else {
            haptics.play(HapticPattern.CONFIRM)
            step = next
            misses = 0
            audio.announce("Good. " + lessons[next].spoken)
        }
    }

    val current = lessons.getOrNull(step)

    ResonantSurface {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .height(IntrinsicSize.Min)
                    .padding(start = 52.dp, end = 24.dp, top = 56.dp, bottom = 28.dp)
            ) {
                Spacer(Modifier.fillMaxHeight().width(5.dp).background(ResonantBlack))
                Column(Modifier.padding(start = 22.dp)) {
                    Text(
                        "Learn the\ngestures",
                        style = MaterialTheme.typography.displayLarge,
                        color = ResonantBlack
                    )
                    Text(
                        if (step < 0) "Listen" else "${(step + 1).coerceAtMost(lessons.size)} of ${lessons.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ResonantWhite
                    )
                }
            }

            GestureSurface(onGesture = { gesture ->
                // Hold-start / hold-end are mechanical, never a lesson answer.
                if (gesture == ResonantGesture.HoldStart || gesture == ResonantGesture.HoldEnd) {
                    return@GestureSurface
                }
                // Escape hatch, announced in the intro: a blind user must never be
                // trapped in a tutorial by a gesture they cannot perform.
                if (gesture is ResonantGesture.LongPress && gesture.zone == InteractionZone.LEFT_EDGE) {
                    haptics.play(HapticPattern.BACK)
                    audio.announce("Skipping the tutorial. Going to the home screen.")
                    onFinishedNow()
                    return@GestureSurface
                }
                val lesson = lessons.getOrNull(step) ?: return@GestureSurface
                if (lesson.matches(gesture)) {
                    advance()
                } else {
                    misses += 1
                    haptics.play(HapticPattern.ERROR)
                    if (misses >= 2) {
                        misses = 0
                        audio.announce("Not quite. " + lesson.hint)
                    }
                }
            }) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(start = 32.dp, end = 24.dp, top = 40.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = current?.label ?: "Welcome",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                        color = ResonantBlack,
                        modifier = Modifier.semantics {
                            contentDescription = current?.spoken ?: INTRO
                        }
                    )
                    Box(Modifier.height(16.dp))
                    Text(
                        text = current?.hint ?: "Turn your volume up.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ResonantWhite
                    )
                    Box(Modifier.height(32.dp))
                    Text(
                        text = "Hold the left edge to skip.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ResonantWhite.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}
