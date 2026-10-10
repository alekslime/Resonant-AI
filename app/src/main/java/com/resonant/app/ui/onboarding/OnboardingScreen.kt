package com.resonant.app.ui.onboarding

import com.resonant.app.ui.theme.ResonantOnInk
import com.resonant.app.ui.theme.ResonantCard
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.gestures.GestureExplanation
import com.resonant.app.gestures.InteractionZone
import com.resonant.app.gestures.ResonantGesture
import com.resonant.app.gestures.SwipeDirection
import com.resonant.app.gestures.explain
import com.resonant.app.haptics.HapticPattern
import com.resonant.app.ui.components.GestureSurface
import com.resonant.app.ui.components.ResonantScaffold
import com.resonant.app.ui.theme.BrandInk
import com.resonant.app.ui.theme.MetropolisBlack
import kotlin.coroutines.resume
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine

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
        label = "Two-finger swipe down",
        spoken = "First, moving through a menu or list. Swipe down with two fingers, anywhere in the middle of the screen, to go to the next item. One finger scrolls; two fingers move between items.",
        hint = "Swipe down with two fingers in the middle of the screen.",
        matches = { it is ResonantGesture.Swipe && it.zone == InteractionZone.CENTER && it.direction == SwipeDirection.UP }
    ),
    GestureLesson(
        label = "Two-finger swipe up",
        spoken = "Now the other way. Swipe up with two fingers in the middle to go back to the previous item.",
        hint = "Swipe up with two fingers in the middle of the screen.",
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
        "Then you can practise freely. To skip the lessons, press and hold the left edge of the screen."

private const val SANDBOX_INTRO =
    "That's all eight. Now you can practise. Nothing you do here changes anything. " +
        "Try any gesture and I will tell you what it means. " +
        "When you are ready, swipe right twice to start. To leave right away, press and hold the left edge."

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    var step by remember { mutableIntStateOf(-1) }   // -1 = intro still playing
    var misses by remember { mutableIntStateOf(0) }
    var completing by remember { mutableStateOf(false) }
    // True once the current step's spoken prompt has finished (or been cut off), so the
    // "still there?" re-prompt is timed from silence rather than from when the step began.
    var promptSettled by remember { mutableStateOf(false) }
    // Set the moment we decide to leave, so a late speech callback can't restart the
    // tutorial and a second exit path can't navigate twice.
    var leaving by remember { mutableStateOf(false) }
    // Practice area after the lessons: every gesture is answered out loud, nothing is acted on.
    var sandbox by remember { mutableStateOf(false) }
    var sandboxLast by remember { mutableStateOf<GestureExplanation?>(null) }
    // True right after one swipe right, so the second one finishes. One swipe is only practice.
    var exitArmed by remember { mutableStateOf(false) }
    var quietTick by remember { mutableIntStateOf(0) }
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
        if (leaving || sandbox) return@LaunchedEffect
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

    fun startSandbox(intro: String) {
        sandbox = true
        completing = true
        step = lessons.size
        haptics.play(HapticPattern.CORRECT)
        audio.announce(intro) { quietTick += 1 }
    }

    // Still practising? Remind once per silence how to carry on.
    LaunchedEffect(sandbox, quietTick) {
        // quietTick only moves once something has finished being said, so the wait starts from silence.
        if (!sandbox || leaving || quietTick == 0) return@LaunchedEffect
        delay(20_000)
        audio.announce("Still practising? Try any gesture, or swipe right twice to start.")
    }

    fun practise(gesture: ResonantGesture) {
        val swipedRight = gesture is ResonantGesture.Swipe &&
            gesture.zone == InteractionZone.CENTER && gesture.direction == SwipeDirection.RIGHT
        if (swipedRight && exitArmed) {
            haptics.play(HapticPattern.CORRECT)
            audio.announce("Good. Taking you to the home screen.") { finish() }
            return
        }
        val answer = explain(gesture) ?: return
        answer.haptic?.let { haptics.play(it) }
        sandboxLast = answer
        exitArmed = swipedRight
        audio.announce(if (swipedRight) answer.spoken + " Swipe right again to start." else answer.spoken) { quietTick += 1 }
    }

    fun advance() {
        val next = step + 1
        if (next >= lessons.size) {
            startSandbox(SANDBOX_INTRO)
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
        subtitle = if (sandbox) "Practice" else if (step < 0) "Listen" else "${(step + 1).coerceAtMost(lessons.size)} of ${lessons.size}"
    ) {
            GestureSurface(twoFingerSwipe = true, hints = false, onGesture = { gesture ->
                // Hold-start / hold-end are mechanical, never a lesson answer.
                if (gesture == ResonantGesture.HoldStart || gesture == ResonantGesture.HoldEnd) {
                    return@GestureSurface
                }
                // Escape hatch, announced in the intro: a blind user must never be
                // trapped in a tutorial by a gesture they cannot perform.
                if (gesture is ResonantGesture.LongPress && gesture.zone == InteractionZone.LEFT_EDGE) {
                    haptics.play(HapticPattern.BACK)
                    if (sandbox) {
                        audio.announce("Leaving practice. Going to the home screen.")
                        finish()
                    } else {
                        // The practice area is part of the first launch, so skipping the
                        // lessons lands there instead of on Home.
                        startSandbox("Skipping the lessons. " + SANDBOX_INTRO)
                    }
                    return@GestureSurface
                }
                if (sandbox) {
                    practise(gesture)
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
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Step counter pill
                    if (step >= 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(BrandInk)
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = if (sandbox) "Practice" else "${(step + 1).coerceAtMost(lessons.size)} of ${lessons.size}",
                                fontFamily = MetropolisBlack,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = ResonantOnInk
                            )
                        }
                    } else {
                        Spacer(Modifier.height(36.dp))
                    }

                    // Main card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 16.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(ResonantCard)
                            .padding(24.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Text(
                                text = current?.label ?: if (sandbox) (sandboxLast?.title ?: "Practice") else "Welcome",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                color = BrandInk,
                                modifier = Modifier.semantics {
                                    contentDescription = current?.spoken
                                        ?: if (sandbox) (sandboxLast?.spoken ?: SANDBOX_INTRO) else INTRO
                                }
                            )
                            Spacer(Modifier.height(20.dp))
                            Text(
                                text = current?.hint ?: if (sandbox) {
                                    sandboxLast?.let { "Means: ${it.meaning}" } ?: "Try any gesture. I will tell you what it means."
                                } else "Turn your volume up.",
                                style = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
                                color = BrandInk
                            )
                        }
                    }

                    // Skip hint pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(BrandInk)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (sandbox) "Swipe right twice to start" else "Hold left edge to skip lessons",
                            fontFamily = MetropolisBlack,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = ResonantOnInk
                        )
                    }
                }
            }
        }
    }

