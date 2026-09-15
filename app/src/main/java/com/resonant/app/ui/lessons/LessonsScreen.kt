package com.resonant.app.ui.lessons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import com.resonant.app.content.LessonData
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

@Composable
fun LessonsScreen(onOpenLesson: (String) -> Unit, onBack: () -> Unit) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    val lessons = LessonData.allLessons
    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        debug.setScreen("Lessons")
        audio.setQueue(
            lessons.mapIndexed { i, l -> SemanticUnit("lesson_list_$i", l.title) },
            startIndex = 0,
            autoAdvance = false
        )
    }

    LaunchedEffect(Unit) {
        audio.index.collect { idx ->
            index = idx.coerceIn(0, (lessons.size - 1).coerceAtLeast(0))
        }
    }

    ResonantScaffold(title = "Lessons", subtitle = "Swipe to browse. Tap to start.") {
        GestureSurface(onGesture = { gesture ->
            when (gesture) {
                is ResonantGesture.Swipe -> if (gesture.zone == InteractionZone.CENTER) {
                    when (gesture.direction) {
                        SwipeDirection.UP -> if (audio.next()) haptics.play(HapticPattern.NEXT)
                        SwipeDirection.DOWN -> if (audio.previous()) haptics.play(HapticPattern.PREVIOUS)
                        else -> {}
                    }
                }
                is ResonantGesture.Tap -> when (gesture.zone) {
                    InteractionZone.CENTER -> {
                        haptics.play(HapticPattern.SELECT)
                        audio.announce("Starting ${lessons[index].title}.")
                        onOpenLesson(lessons[index].id)
                    }
                    InteractionZone.LEFT_EDGE -> { audio.togglePause(); haptics.play(HapticPattern.CONFIRM) }
                    InteractionZone.RIGHT_EDGE -> {}
                }
                is ResonantGesture.DoubleTap -> if (gesture.zone == InteractionZone.LEFT_EDGE) audio.repeatCurrent()
                is ResonantGesture.LongPress -> if (gesture.zone == InteractionZone.RIGHT_EDGE) {
                    haptics.play(HapticPattern.BACK)
                    audio.announce("Back to Home.")
                    onBack()
                }
                ResonantGesture.ThreeFingerTap -> audio.repeatCurrent()
                ResonantGesture.ThreeFingerHold -> audio.announce(
                    "Lessons list. Currently focused: ${lessons[index].title}."
                )
                ResonantGesture.HoldSpeedUp -> { audio.increaseSpeed(); haptics.play(HapticPattern.SPEED_UP) }
                ResonantGesture.HoldSpeedDown -> { audio.decreaseSpeed(); haptics.play(HapticPattern.SPEED_DOWN) }
                else -> {}
            }
        }) {
            Column(
                Modifier.fillMaxSize().padding(start = 32.dp, end = 24.dp, top = 40.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.Center
            ) {
                lessons.forEachIndexed { i, lesson ->
                    val focused = i == index
                    Column(
                        Modifier
                            .padding(vertical = 6.dp)
                            .clickable {
                                audio.jumpTo(i)
                                haptics.play(HapticPattern.SELECT)
                                audio.announce("Starting ${lesson.title}.")
                                onOpenLesson(lesson.id)
                            }
                    ) {
                        Text(
                            lesson.title,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = if (focused) FontWeight.Black else FontWeight.Normal
                            ),
                            color = if (focused) ResonantBlack else ResonantBlack.copy(alpha = 0.3f)
                        )
                        if (focused) {
                            Text(
                                "${lesson.sections.size} sections",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ResonantBlack.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
