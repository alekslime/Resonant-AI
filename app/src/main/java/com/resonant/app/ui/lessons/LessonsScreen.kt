package com.resonant.app.ui.lessons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.resonant.app.ui.components.EdgeHints
import com.resonant.app.ui.components.GestureSurface
import com.resonant.app.ui.components.ResonantScaffold
import com.resonant.app.ui.theme.ResonantBlack
import com.resonant.app.ui.theme.ResonantOrange
import com.resonant.app.ui.theme.ResonantWhite

@Composable
fun LessonsScreen(onOpenLesson: (String) -> Unit) {
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

    ResonantScaffold(title = "Lessons", subtitle = "Swipe to browse. Double-tap to start.") {
        GestureSurface(onGesture = { gesture ->
            when (gesture) {
                is ResonantGesture.Swipe -> if (gesture.zone == InteractionZone.CENTER) {
                    when (gesture.direction) {
                        SwipeDirection.UP -> if (audio.next()) { index = audio.index.value; haptics.play(HapticPattern.NEXT) }
                        SwipeDirection.DOWN -> if (audio.previous()) { index = audio.index.value; haptics.play(HapticPattern.PREVIOUS) }
                        else -> {}
                    }
                }
                is ResonantGesture.DoubleTap -> if (gesture.zone == InteractionZone.CENTER) {
                    haptics.play(HapticPattern.SELECT)
                    audio.announce("Starting ${lessons[index].title}.")
                    onOpenLesson(lessons[index].id)
                }
                is ResonantGesture.Tap -> if (gesture.zone == InteractionZone.LEFT_EDGE) {
                    audio.togglePause(); haptics.play(HapticPattern.CONFIRM)
                }
                ResonantGesture.ThreeFingerTap -> audio.repeatCurrent()
                ResonantGesture.ThreeFingerHold -> audio.announce(
                    "You are in the Lessons list. Currently focused: ${lessons[index].title}."
                )
                ResonantGesture.HoldSpeedUp -> { audio.increaseSpeed(); haptics.play(HapticPattern.SPEED_UP) }
                ResonantGesture.HoldSpeedDown -> { audio.decreaseSpeed(); haptics.play(HapticPattern.SPEED_DOWN) }
                else -> {}
            }
        }) {
            EdgeHints()
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                lessons.forEachIndexed { i, lesson ->
                    val focused = i == index
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (focused) ResonantOrange else ResonantWhite)
                            .clickable { index = i; audio.jumpTo(i) }
                            .padding(20.dp)
                    ) {
                        Text(
                            lesson.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (focused) ResonantWhite else ResonantBlack
                        )
                        Text(
                            "${lesson.sections.size} sections",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (focused) ResonantWhite else ResonantBlack
                        )
                    }
                }
            }
        }
    }
}
