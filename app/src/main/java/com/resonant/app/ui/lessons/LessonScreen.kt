package com.resonant.app.ui.lessons

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.resonant.app.ui.components.ResonantScaffold
import com.resonant.app.ui.theme.ResonantBlack

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
    }

    LaunchedEffect(Unit) {
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
        audio.jumpTo(idx)
        haptics.play(HapticPattern.SECTION_CHANGE)
        audio.announce(lesson.sections[target].title)
    }

    val current = flat.getOrNull(flatIndex)

    ResonantScaffold(
        title = lesson.title,
        subtitle = current?.let { "Section ${it.sectionIndex + 1} of ${lesson.sections.size}: ${it.sectionTitle}" }
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
                ResonantGesture.ThreeFingerHold -> {
                    val speedLabel = com.resonant.app.audio.AudioManager.SPEEDS[audio.speedIndex.value]
                    audio.announce(
                        "You are in ${lesson.title}, section ${lastSectionIndex + 1} of ${lesson.sections.size}. " +
                            "Audio is ${if (audio.isPaused.value) "paused" else "playing"} at ${speedLabel}x speed."
                    )
                }
                ResonantGesture.HoldSpeedUp -> { audio.increaseSpeed(); haptics.play(HapticPattern.SPEED_UP) }
                ResonantGesture.HoldSpeedDown -> { audio.decreaseSpeed(); haptics.play(HapticPattern.SPEED_DOWN) }
                else -> {}
            }
        }) {
            Column(Modifier.fillMaxSize().padding(start = 32.dp, end = 24.dp, top = 40.dp, bottom = 24.dp)) {
                Text(
                    current?.unit?.text ?: "",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = ResonantBlack
                )
                Text(
                    "Unit ${flatIndex + 1} of ${flat.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ResonantBlack.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}
