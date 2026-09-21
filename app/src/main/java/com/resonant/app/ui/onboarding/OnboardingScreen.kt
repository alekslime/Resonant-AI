package com.resonant.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.resonant.app.ui.components.ResonantScaffold
import com.resonant.app.ui.theme.LocalResonantColors
import com.resonant.app.ui.theme.ScreenHorizontalPadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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
        label = "Swipe down",
        spoken = "First, moving through a list. Swipe down anywhere in the middle of the screen to go to the next item.",
        hint = "Swipe down in the middle of the screen.",
        matches = { it is ResonantGesture.Swipe && it.zone == InteractionZone.CENTER && it.direction == SwipeDirection.UP }
    ),
    GestureLesson(
        label = "Swipe up",
        spoken = "Now the other way. Swipe up in the middle to go back to the previous item.",
        hint = "Swipe up in the middle of the screen.",
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
    val colors = LocalResonantColors.current

    var step by remember { mutableIntStateOf(-1) }   // -1 = intro still playing
    var misses by remember { mutableIntStateOf(0) }
    var completing by remember { mutableStateOf(false) }
    // True once the current step's spoken prompt has finished (or been cut off), so the
    // "still there?" re-prompt is timed from silence rather than from when the step began.
    var promptSettled by remember { mutableStateOf(false) }
    // Set the moment we decide to leave, so a late speech callback can't restart the
    // tutorial and a second exit path can't navigate twice.
    var leaving by remember { mutableStateOf(false) }
    val onFinishedNow by rememberUpdatedState(onFinished)

    fun finish() {
        if (leaving) return
        leaving = true
        onFinishedNow()
    }

    // Intro plays once, then the first lesson starts. Waits for the speech itself rather
    // than guessing a duration: how long the intro takes depends on the engine, its voice
    // and the saved speech speed, and a guess that runs short cuts off the line that
    // explains how to skip. The callback also fires if the intro is interrupted (the skip
    // gesture does that), hence the `leaving` check.
    LaunchedEffect(Unit) {
        debug.setScreen("Onboarding")
        suspendCancellableCoroutine<Unit> { cont ->
            audio.announce(INTRO) { if (cont.isActive) cont.resume(Unit) }
        }
        if (leaving) return@LaunchedEffect
        step = 0
        promptSettled = false
        audio.announce(lessons[0].spoken) { if (step == 0) promptSettled = true }
    }

    // Re-prompt with the short hint if the user has been silent or wrong a while —
    // counted from when the prompt stopped speaking, not from when it began.
    LaunchedEffect(step, misses, completing, promptSettled) {
        if (step < 0 || step >= lessons.size || completing || !promptSettled) return@LaunchedEffect
        delay(12_000)
        audio.announce(lessons[step].hint)
    }

    fun advance() {
        val next = step + 1
        if (next >= lessons.size) {
            haptics.play(HapticPattern.CORRECT)
            step = lessons.size
            completing = true
            // Home's first queue flushes whatever is speaking, so leave only once the
            // closing line has been heard.
            audio.announce(
                "That's all of them. You can hear this tutorial again any time from Settings. Taking you to the home screen."
            ) { finish() }
        } else {
            haptics.play(HapticPattern.CONFIRM)
            promptSettled = false
            step = next
            misses = 0
            // Ignore the callback of a prompt that a later step has already replaced.
            audio.announce("Good. " + lessons[next].spoken) { if (step == next) promptSettled = true }
        }
    }

    val current = lessons.getOrNull(step)

    // Same header every other screen uses — this used to hand-roll its own copy
    // of the rule/title/subtitle motif, which meant two independent
    // implementations of the same visual element that could silently drift.
    ResonantScaffold(
        title = "Learn the gestures",
        subtitle = if (step < 0) "Listen" else "${(step + 1).coerceAtMost(lessons.size)} of ${lessons.size}"
    ) {
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
                    finish()
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
                        .padding(horizontal = ScreenHorizontalPadding, vertical = 40.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = current?.label ?: "Welcome",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                        color = colors.text,
                        modifier = Modifier.semantics {
                            contentDescription = current?.spoken ?: INTRO
                        }
                    )
                    Box(Modifier.height(20.dp))
                    Text(
                        text = current?.hint ?: "Turn your volume up.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.text
                    )
                    Box(Modifier.height(36.dp))
                    Text(
                        text = "Hold the left edge to skip.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.text
                    )
                }
            }
        }
    }

