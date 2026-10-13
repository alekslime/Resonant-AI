package com.resonant.app.ui.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.ResonantApp
import com.resonant.app.audio.AudioManager
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.gestures.InteractionZone
import com.resonant.app.gestures.ResonantGesture
import com.resonant.app.gestures.SwipeDirection
import com.resonant.app.haptics.HapticPattern
import com.resonant.app.ui.components.GestureSurface
import com.resonant.app.ui.components.ResonantButton as ResonantActionButton
import com.resonant.app.ui.components.ResonantScaffold
import com.resonant.app.ui.components.ResonantTextField
import com.resonant.app.ui.settings.OptionPicker
import com.resonant.app.ui.theme.ActiveFont
import com.resonant.app.ui.theme.ActiveMotion
import com.resonant.app.ui.theme.ActivePalette
import com.resonant.app.ui.theme.AppFonts
import com.resonant.app.ui.theme.BrandInk
import com.resonant.app.ui.theme.MetropolisBlack
import com.resonant.app.ui.theme.Palettes
import com.resonant.app.ui.theme.TextScale
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlinx.coroutines.suspendCancellableCoroutine

/** The things the person sets up, in order. */
internal enum class SetupStep { THEME, FONT, TEXT_SIZE, SPEED, NAME }

/** The pure parts of the setup: what each step is called, how it is read out, how far along it is. Unit tested. */
internal object SetupFlow {
    val STEPS: List<SetupStep> = SetupStep.values().toList()

    /** Sizes offered for reading text. Same range as pinching on a reading screen. */
    val TEXT_SCALES: List<Float> = listOf(1f, 1.25f, 1.5f, 2f, 2.5f)

    fun title(step: SetupStep): String = when (step) {
        SetupStep.THEME -> "Color theme"
        SetupStep.FONT -> "Font"
        SetupStep.TEXT_SIZE -> "Text size"
        SetupStep.SPEED -> "Speaking speed"
        SetupStep.NAME -> "Your name"
    }

    /** What is said when a step opens, before the option that is showing. */
    fun prompt(step: SetupStep): String = when (step) {
        SetupStep.THEME -> "Color theme. Swipe left or right with two fingers to look through them. Tap the middle to keep one."
        SetupStep.FONT -> "Font. Swipe left or right with two fingers to try each one. Tap the middle to keep it."
        SetupStep.TEXT_SIZE -> "Text size for reading. Swipe left or right with two fingers. Tap the middle to keep it. You can still pinch on any reading screen later."
        SetupStep.SPEED -> "Speaking speed. Swipe right with two fingers to speak faster, left to slow down. Each change is spoken at the new speed. Tap the middle to keep it."
        SetupStep.NAME -> "Last one. What should I call you? Tap the middle to type your first name, or swipe left or right to skip."
    }

    /** Index into [TEXT_SCALES] closest to a saved scale, so an old value still lands on an option. */
    fun scaleIndexOf(scale: Float): Int =
        TEXT_SCALES.indices.minByOrNull { abs(TEXT_SCALES[it] - scale) } ?: 0

    fun scaleLabel(scale: Float): String = "${TextScale.percent(scale)} percent"

    /** How full the position bar is for option [index] of [count]. Empty list = empty bar. */
    fun position(index: Int, count: Int): Float =
        if (count <= 0) 0f else ((index + 1).toFloat() / count).coerceIn(0f, 1f)

    /** Whether a step has a list of options to swipe through (the name step does not). */
    fun hasOptions(step: SetupStep): Boolean = step != SetupStep.NAME
}

private const val SETUP_INTRO =
    "Welcome to Resonant. First, make it yours: a color theme, a font, a text size, how fast I speak, and your name. " +
        "Each choice is applied as you try it. Swipe left or right with two fingers to try options, and tap the middle to keep one. " +
        "To skip all of this, press and hold the left edge of the screen."

private const val SAMPLE = "The quick brown fox jumps over the lazy dog."

@Composable
fun SetupScreen(onFinished: () -> Unit) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    val context = LocalContext.current
    val prefs = remember { (context.applicationContext as ResonantApp).container.prefs }
    val steps = SetupFlow.STEPS

    var step by remember { mutableIntStateOf(-1) }          // -1 = intro still playing, steps.size = finished
    var index by remember { mutableIntStateOf(0) }          // option showing in the current step
    var start by remember { mutableIntStateOf(0) }          // option in use when the step opened, for reverting
    var scale by remember { mutableFloatStateOf(TextScale.clamp(prefs.textScale)) }
    var typing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(prefs.userName.orEmpty()) }
    var leaving by remember { mutableStateOf(false) }
    val speedAtStart = remember { audio.speedIndex.value }
    val onFinishedNow by rememberUpdatedState(onFinished)

    fun stepOrNull(): SetupStep? = steps.getOrNull(step)

    fun count(s: SetupStep): Int = when (s) {
        SetupStep.THEME -> Palettes.ALL.size
        SetupStep.FONT -> AppFonts.ALL.size
        SetupStep.TEXT_SIZE -> SetupFlow.TEXT_SCALES.size
        SetupStep.SPEED -> AudioManager.SPEEDS.size
        SetupStep.NAME -> 0
    }

    fun label(s: SetupStep, i: Int): String = when (s) {
        SetupStep.THEME -> Palettes.ALL[i].label
        SetupStep.FONT -> AppFonts.ALL[i].label
        SetupStep.TEXT_SIZE -> SetupFlow.scaleLabel(SetupFlow.TEXT_SCALES[i])
        SetupStep.SPEED -> audio.speedLabel(AudioManager.SPEEDS[i])
        SetupStep.NAME -> ""
    }

    fun currentIndex(s: SetupStep): Int = when (s) {
        SetupStep.THEME -> Palettes.ALL.indexOf(ActivePalette.current)
        SetupStep.FONT -> AppFonts.ALL.indexOf(ActiveFont.current)
        SetupStep.TEXT_SIZE -> SetupFlow.scaleIndexOf(scale)
        SetupStep.SPEED -> audio.speedIndex.value
        SetupStep.NAME -> 0
    }.coerceAtLeast(0)

    /** Shows an option without saving it. Speed is the exception: it is applied (and saved) by the audio manager itself. */
    fun show(s: SetupStep, i: Int) {
        when (s) {
            SetupStep.THEME -> ActivePalette.current = Palettes.ALL[i]
            SetupStep.FONT -> ActiveFont.current = AppFonts.ALL[i]
            SetupStep.TEXT_SIZE -> scale = SetupFlow.TEXT_SCALES[i]
            else -> Unit
        }
    }

    fun save(s: SetupStep, i: Int) {
        when (s) {
            SetupStep.THEME -> prefs.colorTheme = Palettes.ALL[i].id
            SetupStep.FONT -> prefs.font = AppFonts.ALL[i].id
            SetupStep.TEXT_SIZE -> prefs.textScale = SetupFlow.TEXT_SCALES[i]
            else -> Unit   // speed already saved on every change
        }
    }

    /** Puts back whatever the open step was showing before it was tried. Nothing to do once a step is kept. */
    fun revertPending() {
        val s = stepOrNull() ?: return
        when (s) {
            SetupStep.SPEED -> {
                audio.restoreSpeedIndex(speedAtStart)
                prefs.speedIndex = speedAtStart
            }
            SetupStep.NAME -> Unit
            else -> show(s, start)
        }
    }

    // Leaving mid-preview (system back, process change) must not leave an unsaved option on screen.
    DisposableEffect(Unit) { onDispose { revertPending() } }

    fun finish(message: String) {
        if (leaving) return
        leaving = true
        step = steps.size
        typing = false
        haptics.play(HapticPattern.CORRECT)
        audio.announce(message) { onFinishedNow() }
    }

    fun enter(next: Int, prefix: String = "") {
        if (next >= steps.size) {
            finish(if (name.isBlank()) "All set. Now the gestures." else "Nice to meet you, ${name.trim()}. Now the gestures.")
            return
        }
        step = next
        val s = steps[next]
        start = currentIndex(s)
        index = start
        haptics.play(HapticPattern.SECTION_CHANGE)
        val option = if (SetupFlow.hasOptions(s)) " " + OptionPicker.describe(label(s, index), index, count(s)) else ""
        audio.announce(prefix + SetupFlow.prompt(s) + option)
    }

    fun skipAll() {
        if (leaving) return
        revertPending()
        haptics.play(HapticPattern.BACK)
        finish("Skipping setup. Now the gestures.")
    }

    fun move(delta: Int) {
        val s = stepOrNull() ?: return
        if (!SetupFlow.hasOptions(s)) return
        if (s == SetupStep.SPEED) {
            // The audio manager speaks the new rate itself, at that rate, so the number is also a sample.
            val moved = if (delta > 0) audio.increaseSpeed() else audio.decreaseSpeed()
            haptics.play(if (!moved) HapticPattern.EDGE else if (delta > 0) HapticPattern.SPEED_UP else HapticPattern.SPEED_DOWN)
            index = audio.speedIndex.value
            return
        }
        val to = OptionPicker.move(index, delta, count(s))
        if (to == null) {
            haptics.play(HapticPattern.EDGE)
            audio.announce((if (delta > 0) "Last. " else "First. ") + OptionPicker.describe(label(s, index), index, count(s)))
            return
        }
        show(s, to)
        index = to
        haptics.play(if (delta > 0) HapticPattern.NEXT else HapticPattern.PREVIOUS)
        audio.announce(OptionPicker.describe(label(s, to), to, count(s)))
    }

    fun keep() {
        val s = stepOrNull() ?: return
        if (s == SetupStep.NAME) {
            typing = true
            audio.announce("Type your first name, then press Done.")
            return
        }
        save(s, index)
        haptics.play(HapticPattern.CONFIRM)
        enter(step + 1, "Kept. ")
    }

    fun back() {
        if (step <= 0) return
        revertPending()
        haptics.play(HapticPattern.BACK)
        enter(step - 1, "Going back. ")
    }

    // The intro plays once, then the first step opens. Waits for the speech itself, as the tutorial does,
    // so a slow engine or a fast speech speed can't cut off the line about skipping.
    LaunchedEffect(Unit) {
        debug.setScreen("Setup")
        suspendCancellableCoroutine<Unit> { cont ->
            audio.announce(SETUP_INTRO) { if (cont.isActive) cont.resume(Unit) }
        }
        if (leaving) return@LaunchedEffect
        enter(0)
    }

    val current = stepOrNull()
    val optionText = current?.takeIf { SetupFlow.hasOptions(it) }?.let { label(it, index) }
        ?: if (current == SetupStep.NAME) (name.trim().ifBlank { "No name yet" }) else "Listen"

    ResonantScaffold(
        title = "Make it yours",
        subtitle = if (current != null) "${step + 1} of ${steps.size}" else "Listen"
    ) {
        if (typing && current == SetupStep.NAME) {
            // Typing is the one place a keyboard is needed, so the gesture layer steps aside for it.
            Column(
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                ResonantTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    label = "First name",
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        prefs.userName = name
                        enter(steps.size)
                    })
                )
                Spacer(Modifier.height(20.dp))
                ResonantActionButton(text = "Save and continue", onClick = {
                    prefs.userName = name
                    enter(steps.size)
                })
                Spacer(Modifier.height(12.dp))
                ResonantActionButton(text = "Skip", filled = false, onClick = {
                    name = prefs.userName.orEmpty()
                    enter(steps.size)
                })
            }
        } else {
            GestureSurface(
                twoFingerSwipe = true,
                hint = "Swipe left or right with two fingers to try options, tap the middle to keep, hold the right edge to go back a step, or hold the left edge to skip setup.",
                onGesture = { gesture ->
                    if (gesture == ResonantGesture.HoldStart || gesture == ResonantGesture.HoldEnd) return@GestureSurface
                    if (leaving) return@GestureSurface
                    // Escape hatch, spoken in the intro: nobody is trapped in setup.
                    if (gesture is ResonantGesture.LongPress && gesture.zone == InteractionZone.LEFT_EDGE) {
                        skipAll()
                        return@GestureSurface
                    }
                    val s = stepOrNull() ?: return@GestureSurface
                    when {
                        gesture is ResonantGesture.Swipe && gesture.zone == InteractionZone.CENTER &&
                            (gesture.direction == SwipeDirection.LEFT || gesture.direction == SwipeDirection.RIGHT) -> {
                            if (s == SetupStep.NAME) {
                                haptics.play(HapticPattern.SELECT)
                                enter(steps.size)
                            } else {
                                move(if (gesture.direction == SwipeDirection.RIGHT) +1 else -1)
                            }
                        }
                        gesture is ResonantGesture.Tap && gesture.zone == InteractionZone.CENTER -> {
                            haptics.play(HapticPattern.SELECT)
                            keep()
                        }
                        gesture is ResonantGesture.LongPress && gesture.zone == InteractionZone.RIGHT_EDGE -> back()
                        gesture == ResonantGesture.ThreeFingerHold -> audio.announce(
                            "Setup, step ${step + 1} of ${steps.size}: ${SetupFlow.title(s)}. " +
                                if (SetupFlow.hasOptions(s)) OptionPicker.describe(label(s, index), index, count(s)) + "." else "Tap the middle to type your name."
                        )
                        else -> Unit
                    }
                }
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Step track: filled up to the step you are on.
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Step ${(step + 1).coerceAtLeast(0)} of ${steps.size}" },
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        steps.forEachIndexed { i, _ ->
                            val fill by animateColorAsState(
                                targetValue = if (i <= step) BrandInk else BrandInk.copy(alpha = 0.2f),
                                animationSpec = tween(ActiveMotion.ms(250)),
                                label = "setupSegment"
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

                    // No card: the choice sits straight on the screen, big, so the theme and font are the design.
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = (current?.let { SetupFlow.title(it) } ?: "Welcome").uppercase(),
                            style = TextStyle(
                                fontFamily = MetropolisBlack,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                letterSpacing = 1.5.sp
                            ),
                            color = BrandInk.copy(alpha = 0.7f),
                            modifier = Modifier.semantics {
                                contentDescription = current?.let { SetupFlow.prompt(it) } ?: SETUP_INTRO
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                        Crossfade(
                            targetState = optionText,
                            animationSpec = tween(ActiveMotion.ms(150)),
                            label = "setupOption"
                        ) { shown ->
                            Text(
                                text = shown,
                                style = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.Black, fontSize = 40.sp, lineHeight = 46.sp),
                                color = BrandInk
                            )
                        }
                        if (current != SetupStep.NAME) {
                            Spacer(Modifier.height(20.dp))
                            val sampleScale = if (current == SetupStep.TEXT_SIZE) scale else 1f
                            Text(
                                text = SAMPLE,
                                style = TextStyle(
                                    fontFamily = MetropolisBlack,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = (20 * sampleScale).sp,
                                    lineHeight = (28 * sampleScale).sp
                                ),
                                color = BrandInk.copy(alpha = 0.85f)
                            )
                        }
                        if (current != null && SetupFlow.hasOptions(current)) {
                            Spacer(Modifier.height(32.dp))
                            // One dot per option. The one showing is bigger. For themes each dot wears its own colors.
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 0 until count(current)) {
                                    val selected = i == index
                                    val size by animateDpAsState(
                                        targetValue = if (selected) 18.dp else 10.dp,
                                        animationSpec = tween(ActiveMotion.ms(150)),
                                        label = "setupDot"
                                    )
                                    val palette = if (current == SetupStep.THEME) Palettes.ALL[i] else null
                                    Box(
                                        Modifier
                                            .size(size)
                                            .clip(CircleShape)
                                            .background(palette?.background ?: if (selected) BrandInk else BrandInk.copy(alpha = 0.2f))
                                            .then(
                                                if (palette != null) Modifier.border(if (selected) 3.dp else 1.5.dp, palette.ink, CircleShape)
                                                else Modifier
                                            )
                                    )
                                }
                            }
                        }
                    }

                    // One quiet line instead of a button: the gestures are the controls.
                    Text(
                        text = when {
                            current == null -> "Hold the left edge to skip"
                            current == SetupStep.NAME -> "Tap to type, or swipe to skip"
                            else -> "Swipe to try, tap to keep"
                        },
                        style = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                        color = BrandInk.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}
