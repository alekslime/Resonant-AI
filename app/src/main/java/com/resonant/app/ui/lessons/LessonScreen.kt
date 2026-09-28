package com.resonant.app.ui.lessons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.content.Lesson
import com.resonant.app.content.LessonSection
import com.resonant.app.content.SemanticUnit
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.gestures.InteractionZone
import com.resonant.app.gestures.ResonantGesture
import com.resonant.app.gestures.SwipeDirection
import com.resonant.app.haptics.HapticPattern
import com.resonant.app.ui.components.GestureSurface
import com.resonant.app.ui.home.SettingsPlaceholderIcon
import com.resonant.app.ui.theme.BrandInk
import com.resonant.app.ui.theme.BrandOrange
import com.resonant.app.ui.theme.MetropolisBlack

private data class FlatUnit(val unit: SemanticUnit, val sectionIndex: Int, val sectionTitle: String)

private fun flatten(lesson: Lesson): List<FlatUnit> =
    lesson.sections.flatMapIndexed { si, section ->
        section.units.map { FlatUnit(it, si, section.title) }
    }

private fun firstIndexOfSection(flat: List<FlatUnit>, sectionIndex: Int): Int =
    flat.indexOfFirst { it.sectionIndex == sectionIndex }

@Composable
fun LessonScreen(lesson: Lesson, onExit: () -> Unit) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    val flat = remember(lesson) { flatten(lesson) }

    var flatIndex by remember { mutableIntStateOf(0) }
    var lastSectionIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(lesson.id) {
        debug.setScreen("Lesson: ${lesson.title}")
        audio.setQueue(flat.map { it.unit }, startIndex = 0, autoAdvance = true)
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

    Box(
        Modifier
            .fillMaxSize()
            .background(BrandOrange)
    ) {
        GestureSurface(onGesture = { gesture ->
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
                    InteractionZone.CENTER -> {}
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
                        "Audio is ${if (audio.isPaused.value) "paused" else "playing"} at ${audio.speedLabel()}."
                )
                ResonantGesture.HoldSpeedUp -> { if (audio.increaseSpeed()) haptics.play(HapticPattern.SPEED_UP) else haptics.play(HapticPattern.ERROR) }
                ResonantGesture.HoldSpeedDown -> { if (audio.decreaseSpeed()) haptics.play(HapticPattern.SPEED_DOWN) else haptics.play(HapticPattern.ERROR) }
                else -> {}
            }
        }) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color(0xFF0A0A0A))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "←",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Lessons",
                        fontFamily = MetropolisBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color.White
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        SettingsPlaceholderIcon(tint = BrandInk, modifier = Modifier.size(18.dp))
                    }
                }

                // Lesson title — large, directly on orange
                Text(
                    text = lesson.title,
                    fontFamily = MetropolisBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 42.sp,
                    lineHeight = 48.sp,
                    color = BrandInk,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )

                // White content card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .padding(24.dp)
                ) {
                    // Section summary — first unit of the section if no stepTitle, else a fixed line
                    val summaryText = currentSection?.let { sec ->
                        if (sec.units.any { it.stepTitle != null }) {
                            // Section has steps — use section title as summary prompt
                            sec.title
                        } else {
                            sec.units.firstOrNull()?.text ?: sec.title
                        }
                    } ?: lesson.title

                    Text(
                        text = summaryText,
                        fontFamily = MetropolisBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        lineHeight = 28.sp,
                        color = BrandInk,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Steps or plain units
                    currentSection?.let { sec ->
                        val hasSteps = sec.units.any { it.stepTitle != null }
                        if (hasSteps) {
                            sec.units.forEachIndexed { i, unit ->
                                StepRow(number = i + 1, title = unit.stepTitle ?: "", body = unit.text)
                                if (i < sec.units.size - 1) Spacer(Modifier.height(20.dp))
                            }
                        } else {
                            // Plain units — show from second onward (first is the summary)
                            sec.units.drop(1).forEach { unit ->
                                Text(
                                    text = unit.text,
                                    fontFamily = MetropolisBlack,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    color = BrandInk,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StepRow(number: Int, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        // Numbered circle
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(BrandInk),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                fontFamily = MetropolisBlack,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = Color.White
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontFamily = MetropolisBlack,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = BrandInk
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                fontFamily = MetropolisBlack,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFF444444)
            )
        }
    }
}
