package com.resonant.app.ui.lessons

import com.resonant.app.ui.theme.ResonantOnInk
import com.resonant.app.ui.theme.ResonantCard
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.ResonantApp
import com.resonant.app.content.Lesson
import com.resonant.app.content.SemanticUnit
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.gestures.InteractionZone
import com.resonant.app.gestures.ResonantGesture
import com.resonant.app.gestures.SwipeDirection
import com.resonant.app.haptics.HapticPattern
import com.resonant.app.ui.components.GestureSurface
import com.resonant.app.ui.components.SystemBarsColor
import com.resonant.app.ui.theme.BrandInk
import com.resonant.app.ui.theme.BrandOrange
import com.resonant.app.ui.theme.MetropolisBlack
import com.resonant.app.ui.theme.TextScale

private data class FlatUnit(val unit: SemanticUnit, val sectionIndex: Int, val sectionTitle: String)

private fun flatten(lesson: Lesson): List<FlatUnit> =
    lesson.sections.flatMapIndexed { si, section ->
        section.units.map { FlatUnit(it, si, section.title) }
    }

private fun firstIndexOfSection(flat: List<FlatUnit>, sectionIndex: Int): Int =
    flat.indexOfFirst { it.sectionIndex == sectionIndex }

@Composable
fun LessonScreen(lesson: Lesson, onExit: () -> Unit, autoAdvance: Boolean = true, onOpenSettings: (() -> Unit)? = null) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    val flat = remember(lesson) { flatten(lesson) }

    val prefs = (LocalContext.current.applicationContext as ResonantApp).container.prefs
    var textScale by remember { mutableStateOf(TextScale.clamp(prefs.textScale)) }
    val onPinch: (Float) -> Unit = { factor ->
        val old = textScale
        val new = TextScale.apply(old, factor)
        if (new != old) {
            textScale = new
            prefs.textScale = new
            val atLimit = new == TextScale.MAX || new == TextScale.MIN
            val before = TextScale.stepOf(old)
            val after = TextScale.stepOf(new)
            when {
                atLimit -> haptics.play(HapticPattern.EDGE)
                after > before -> haptics.play(HapticPattern.SPEED_UP)
                after < before -> haptics.play(HapticPattern.SPEED_DOWN)
            }
        }
    }

    // Saved across opening Settings from the gear, so coming back resumes at the same sentence.
    var flatIndex by rememberSaveable { mutableStateOf(0) }
    var lastSectionIndex by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(lesson.id, autoAdvance) {
        debug.setScreen("Lesson: ${lesson.title}")
        val resumeAt = flatIndex.coerceIn(0, (flat.size - 1).coerceAtLeast(0))
        // Say which mode this is before the first sentence (or that we are back, and resume
        // from the sentence we left on); the lesson waits for it.
        audio.announce(
            if (resumeAt > 0) "Back to the lesson."
            else if (autoAdvance) "Auto. The lesson will play on its own."
            else "Manual. Tap the center of the screen to hear the next sentence."
        )
        audio.setQueue(
            flat.map { it.unit },
            startIndex = resumeAt,
            autoAdvance = autoAdvance,
            queueBehindAnnouncement = true
        )
        audio.index.collect { newIndex ->
            flatIndex = newIndex.coerceIn(0, (flat.size - 1).coerceAtLeast(0))
            val newSection = flat.getOrNull(flatIndex)?.sectionIndex ?: 0
            if (newSection != lastSectionIndex) {
                haptics.play(HapticPattern.SECTION_CHANGE)
                lastSectionIndex = newSection
            }
        }
    }

    fun jumpToSection(delta: Int) {
        val target = (lastSectionIndex + delta).coerceIn(0, lesson.sections.size - 1)
        if (target == lastSectionIndex) return
        val idx = firstIndexOfSection(flat, target)
        haptics.play(HapticPattern.SECTION_CHANGE)
        audio.announce(lesson.sections[target].title)
        audio.jumpTo(idx, queueBehindAnnouncement = true)
    }

    val currentSection = lesson.sections.getOrNull(lastSectionIndex)

    SystemBarsColor(BrandOrange)

    Box(
        Modifier
            .fillMaxSize()
            .background(BrandOrange)
    ) {
        GestureSurface(
            twoFingerSwipe = true,
            hint = "Tap the left edge to pause, double-tap it to repeat, swipe right for the next section, pinch with two fingers to change the text size, or hold three fingers to hear where you are.",
            onPinch = onPinch,
            onGesture = { gesture ->
            when (gesture) {
                is ResonantGesture.Swipe -> if (gesture.zone == InteractionZone.CENTER) {
                    when (gesture.direction) {
                        SwipeDirection.UP -> { audio.next(); haptics.play(HapticPattern.NEXT) }
                        SwipeDirection.DOWN -> { audio.previous(); haptics.play(HapticPattern.PREVIOUS) }
                        SwipeDirection.RIGHT -> jumpToSection(+1)
                        SwipeDirection.LEFT -> jumpToSection(-1)
                    }
                }
                is ResonantGesture.Tap -> when (gesture.zone) {
                    InteractionZone.LEFT_EDGE -> { audio.togglePause(); haptics.play(HapticPattern.CONFIRM) }
                    // Manual: each tap in the center plays the next sentence.
                    InteractionZone.CENTER -> if (!autoAdvance) {
                        if (audio.next()) {
                            haptics.play(HapticPattern.NEXT)
                        } else {
                            haptics.play(HapticPattern.EDGE)
                            audio.announce("End of the lesson. Long press the right edge to go back.")
                        }
                    }
                    InteractionZone.RIGHT_EDGE -> {}
                }
                is ResonantGesture.DoubleTap -> if (gesture.zone == InteractionZone.LEFT_EDGE) audio.repeatCurrent()
                is ResonantGesture.LongPress -> if (gesture.zone == InteractionZone.RIGHT_EDGE) {
                    haptics.play(HapticPattern.BACK)
                    onExit()
                }
                ResonantGesture.ThreeFingerTap -> audio.repeatCurrent()
                ResonantGesture.ThreeFingerHold -> audio.announce(
                    "You are in ${lesson.title}, section ${lastSectionIndex + 1} of ${lesson.sections.size}. " +
                        "${if (autoAdvance) "Auto" else "Manual"} mode. Audio is ${if (audio.isPaused.value) "paused" else "playing"} at ${audio.speedLabel()}. Text size ${TextScale.percent(textScale)} percent."
                )
                ResonantGesture.HoldSpeedUp -> { if (audio.increaseSpeed()) haptics.play(HapticPattern.SPEED_UP) else haptics.play(HapticPattern.ERROR) }
                ResonantGesture.HoldSpeedDown -> { if (audio.decreaseSpeed()) haptics.play(HapticPattern.SPEED_DOWN) else haptics.play(HapticPattern.ERROR) }
                else -> {}
            }
        }) {
            Column(Modifier.fillMaxSize()) {
                LessonsTopBar(onBack = onExit, backAnnouncement = "Back to Lessons.", onSettings = onOpenSettings)

                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    // Title card
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(ResonantCard)
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        FittedTitle(lesson.title, maxSp = 50f, lineRatio = 1.2f)
                    }

                    val sec = currentSection
                    val hasSteps = sec?.units?.any { it.stepTitle != null } == true
                    // Lead line: for step sections the section title; otherwise the first unit.
                    val lead = when {
                        sec == null -> lesson.title
                        hasSteps -> sec.title
                        else -> sec.units.firstOrNull()?.text ?: sec.title
                    }

                    Text(
                        text = lead,
                        fontFamily = MetropolisBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = (32 * textScale).sp,
                        lineHeight = (42 * textScale).sp,
                        color = BrandInk,
                        modifier = Modifier.padding(top = 36.dp)
                    )

                    Spacer(Modifier.height(28.dp))

                    if (sec != null) {
                        if (hasSteps) {
                            sec.units.forEachIndexed { i, unit ->
                                StepRow(number = i + 1, title = unit.stepTitle ?: unit.text, scale = textScale)
                                if (i < sec.units.size - 1) Spacer(Modifier.height(36.dp))
                            }
                        } else {
                            sec.units.drop(1).forEach { unit ->
                                Text(
                                    text = unit.text,
                                    fontFamily = MetropolisBlack,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = (24 * textScale).sp,
                                    lineHeight = (32 * textScale).sp,
                                    color = BrandInk,
                                    modifier = Modifier.padding(bottom = 20.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun StepRow(number: Int, title: String, scale: Float) {
    Row(verticalAlignment = Alignment.Top) {
        // Numbered circle, centered on the first line of the title
        Box(
            modifier = Modifier
                .padding(top = (6 * scale).dp)
                .size((24 * scale).dp)
                .clip(CircleShape)
                .background(BrandInk),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                fontFamily = MetropolisBlack,
                fontWeight = FontWeight.Black,
                fontSize = (14 * scale).sp,
                color = ResonantOnInk
            )
        }
        Spacer(Modifier.width(20.dp))
        Text(
            text = title.uppercase(),
            fontFamily = MetropolisBlack,
            fontWeight = FontWeight.SemiBold,
            fontSize = (28 * scale).sp,
            lineHeight = (36 * scale).sp,
            color = BrandInk,
            modifier = Modifier.weight(1f)
        )
    }
}
