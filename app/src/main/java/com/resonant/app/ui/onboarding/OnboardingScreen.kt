package com.resonant.app.ui.onboarding

import com.resonant.app.ui.theme.EdgeZoneWidth
import com.resonant.app.ui.theme.ActiveMotion
import com.resonant.app.ui.components.ResonantDots
import com.resonant.app.ui.components.DotsState
import com.resonant.app.gestures.GestureClassifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.Crossfade
import com.resonant.app.ui.theme.cardShape
import com.resonant.app.ui.theme.controlShape
import com.resonant.app.ui.theme.ResonantOnButton
import com.resonant.app.ui.theme.ResonantButton
import com.resonant.app.ui.theme.controlBorder
import com.resonant.app.ui.theme.cardBorder
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
import com.resonant.app.audio.AudioManager
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.gestures.GestureExplanation
import com.resonant.app.gestures.InteractionZone
import com.resonant.app.gestures.ResonantGesture
import com.resonant.app.gestures.SwipeDirection
import com.resonant.app.gestures.explain
import com.resonant.app.gestures.explainPinch
import com.resonant.app.gestures.explainRewind
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
    val matches: (ResonantGesture) -> Boolean,
    val tool: Tool = Tool.GESTURE
)

/** Pinch and four-finger rewind do not come in as gestures, so those steps are finished by their own callbacks. */
private enum class Tool { GESTURE, PINCH, REWIND }

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
        label = "Four fingers left",
        spoken = "Missed more than a sentence? Put four fingers on the screen and drag them left. Every step goes back five seconds, and you feel a tick for each one. Lift your fingers to jump back.",
        hint = "Put four fingers down, drag them left, then lift.",
        matches = { false },
        tool = Tool.REWIND
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
        label = "Pinch",
        spoken = "On any screen you can change the text size. Put two fingers on the screen and spread them apart for larger text, or bring them together for smaller text.",
        hint = "Put two fingers down and spread them apart.",
        matches = { false },
        tool = Tool.PINCH
    ),
    GestureLesson(
        label = "Three-finger hold",
        spoken = "Last one. Lost your place? Hold three fingers anywhere on the screen and Resonant will tell you where you are.",
        hint = "Press and hold with three fingers anywhere.",
        matches = { it == ResonantGesture.ThreeFingerHold }
    )
)

private const val INTRO =
    "Now the gestures. Resonant is used entirely by touch and sound — there is nothing you need to see. " +
        "I will teach you ten gestures, one at a time. Try each one and I will confirm it. " +
        "Then you can practise freely. To skip the lessons, press and hold the left edge of the screen."

private const val SANDBOX_INTRO =
    "That's all ten. Now you can practise. Nothing you do here changes anything. " +
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
    // Counts wrong gestures (unlike [misses], which resets), so each one can trigger its own cue.
    var missTick by remember { mutableIntStateOf(0) }
    // A pinch has no end event, so its movements add up and it is judged once the fingers go quiet.
    var pinchTotal by remember { mutableStateOf(1f) }
    var pinchTick by remember { mutableIntStateOf(0) }
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

    fun showAnswer(answer: GestureExplanation) {
        answer.haptic?.let { haptics.play(it) }
        sandboxLast = answer
        exitArmed = false
        audio.announce(answer.spoken) { quietTick += 1 }
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

    fun settlePinch() {
        val total = pinchTotal
        pinchTotal = 1f
        if (sandbox) {
            showAnswer(explainPinch(total))
        } else if (lessons.getOrNull(step)?.tool == Tool.PINCH) {
            advance()
        }
    }

    LaunchedEffect(pinchTick) {
        if (pinchTick == 0) return@LaunchedEffect
        delay(450)
        settlePinch()
    }

    val onPinch: (Float) -> Unit = { factor ->
        pinchTotal *= factor
        pinchTick += 1
    }

    val onRewind: (Int, Boolean) -> Unit = { steps, release ->
        if (!release) {
            haptics.play(HapticPattern.PREVIOUS)
        } else if (sandbox) {
            showAnswer(explainRewind(steps * AudioManager.REWIND_STEP_SECONDS.toInt()))
        } else if (lessons.getOrNull(step)?.tool == Tool.REWIND) {
            advance()
        }
    }

    val current = lessons.getOrNull(step)
    // Only live while they can mean something: a pinch that crept into a swipe lesson would eat the swipe.
    val pinchOn = sandbox || current?.tool == Tool.PINCH
    val rewindOn = sandbox || current?.tool == Tool.REWIND

    // The mark answers what is happening: it talks while a prompt plays, bounces when a gesture is right
    // and dips for a moment when one is wrong. Reduce motion makes all of that instant.
    val dotsState = when {
        step < 0 -> DotsState.Speaking
        !sandbox && step < lessons.size && !promptSettled -> DotsState.Speaking
        else -> DotsState.Idle
    }
    val pop = remember { Animatable(1f) }
    val dip = remember { Animatable(1f) }
    LaunchedEffect(step) {
        if (step <= 0 || ActiveMotion.reduce) pop.snapTo(1f)
        else { pop.snapTo(0.88f); pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 380f)) }
    }
    LaunchedEffect(missTick) {
        if (missTick == 0 || ActiveMotion.reduce) dip.snapTo(1f)
        else { dip.snapTo(0.4f); dip.animateTo(1f, tween(350)) }
    }
    val progressText = if (sandbox) "Practice" else if (step < 0) "Listen" else "${(step + 1).coerceAtMost(lessons.size)} / ${lessons.size}"
    val title = current?.label ?: if (sandbox) (sandboxLast?.title ?: "Practice") else "Welcome"
    val detail = current?.hint ?: if (sandbox) {
        sandboxLast?.let { "Means: ${it.meaning}" } ?: "Try any gesture. I will tell you what it means."
    } else "Turn your volume up."

    // Same header every other screen uses — this used to hand-roll its own copy
    // of the rule/title/subtitle motif, which meant two independent
    // implementations of the same visual element that could silently drift.
    ResonantScaffold(
        title = "Learn the gestures",
        subtitle = if (sandbox) "Practice" else if (step < 0) "Listen" else "${(step + 1).coerceAtMost(lessons.size)} of ${lessons.size}"
    ) {
            TouchLayer {
            GestureSurface(
                twoFingerSwipe = true,
                hints = false,
                pinchResize = false,
                rewind = false,
                onPinch = if (pinchOn) onPinch else null,
                onRewind = if (rewindOn) onRewind else null,
                onGesture = { gesture ->
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
                    missTick += 1
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
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Progress: one segment per lesson, filled in as each is passed.
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = progressText },
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        lessons.indices.forEach { i ->
                            val fill by animateColorAsState(
                                targetValue = if (sandbox || i <= step) BrandInk else BrandInk.copy(alpha = 0.2f),
                                animationSpec = tween(ActiveMotion.ms(250)),
                                label = "lessonSegment"
                            )
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(fill)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (step >= 0 || sandbox) progressText else "",
                        style = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                        color = BrandInk.copy(alpha = 0.7f)
                    )

                    // The brand mark fills the top half.
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        ResonantDots(
                            state = dotsState,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = pop.value
                                    scaleY = pop.value
                                    alpha = dip.value
                                }
                        )
                    }

                    Crossfade(
                        targetState = title,
                        animationSpec = tween(ActiveMotion.ms(150)),
                        label = "lessonTitle"
                    ) { shown ->
                        Text(
                            text = shown,
                            style = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.Black, fontSize = 32.sp, lineHeight = 38.sp, textAlign = TextAlign.Center),
                            color = BrandInk,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription = current?.spoken
                                        ?: if (sandbox) (sandboxLast?.spoken ?: SANDBOX_INTRO) else INTRO
                                }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = detail,
                        style = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp, textAlign = TextAlign.Center),
                        color = BrandInk.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = if (sandbox) "Swipe right twice to start" else "Hold the left edge to skip the lessons",
                        style = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, textAlign = TextAlign.Center),
                        color = BrandInk.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            }
        }
    }

/**
 * Shows where the fingers are, so the three zones can be seen as well as felt: a faint line at each edge zone,
 * the zone being touched glows, a ring sits under each finger, and the first finger leaves a short fading trail.
 * It only watches (before the gesture detector, consuming nothing), so it can never change what a gesture does.
 * With Reduce motion on there is no trail, only the rings and the glow.
 */
@Composable
private fun TouchLayer(content: @Composable () -> Unit) {
    val ink = BrandInk
    val edgePx = with(LocalDensity.current) { EdgeZoneWidth.toPx() }
    val reduce = ActiveMotion.reduce
    var width by remember { mutableIntStateOf(0) }
    val fingers = remember { mutableStateListOf<Offset>() }
    val trail = remember { mutableStateListOf<Offset>() }

    LaunchedEffect(fingers.isEmpty()) {
        if (fingers.isEmpty()) {
            delay(250)
            trail.clear()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { width = it.width }
            .pointerInput(reduce) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val down = event.changes.filter { it.pressed }
                        fingers.clear()
                        down.forEach { fingers.add(it.position) }
                        if (!reduce && down.isNotEmpty()) {
                            trail.add(down.first().position)
                            while (trail.size > 14) trail.removeAt(0)
                        }
                    }
                }
            }
    ) {
        content()
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val line = 2.dp.toPx()
            drawLine(ink.copy(alpha = 0.12f), Offset(edgePx, 0f), Offset(edgePx, h), strokeWidth = line)
            drawLine(ink.copy(alpha = 0.12f), Offset(w - edgePx, 0f), Offset(w - edgePx, h), strokeWidth = line)

            fingers.map { GestureClassifier.zoneFor(it.x, width, edgePx, edgePx) }.toSet().forEach { zone ->
                when (zone) {
                    InteractionZone.LEFT_EDGE -> drawRect(ink.copy(alpha = 0.14f), Offset.Zero, Size(edgePx, h))
                    InteractionZone.RIGHT_EDGE -> drawRect(ink.copy(alpha = 0.14f), Offset(w - edgePx, 0f), Size(edgePx, h))
                    InteractionZone.CENTER -> drawRect(ink.copy(alpha = 0.07f), Offset(edgePx, 0f), Size(w - 2 * edgePx, h))
                }
            }

            trail.forEachIndexed { i, point ->
                val f = (i + 1f) / trail.size
                drawCircle(ink.copy(alpha = 0.3f * f), radius = 12.dp.toPx() * f, center = point)
            }
            fingers.forEach { point ->
                drawCircle(ink.copy(alpha = 0.15f), radius = 34.dp.toPx(), center = point)
                drawCircle(ink.copy(alpha = 0.6f), radius = 34.dp.toPx(), center = point, style = Stroke(width = 3.dp.toPx()))
            }
        }
    }
}
