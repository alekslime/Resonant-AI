package com.resonant.app.ui.lessons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.resonant.app.ui.theme.LocalResonantColors
import com.resonant.app.ui.theme.ScreenHorizontalPadding

@Composable
fun LessonsScreen(onOpenLesson: (String) -> Unit, onBack: () -> Unit) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    val lessons = LessonData.allLessons
    val colors = LocalResonantColors.current
    var index by remember { mutableIntStateOf(0) }

    // Single effect: setQueue must land before collection starts, or the first
    // collected value could be the pre-queue default instead of the real start.
    LaunchedEffect(Unit) {
        debug.setScreen("Lessons")
        audio.setQueue(
            lessons.mapIndexed { i, l -> SemanticUnit("lesson_list_$i", l.title) },
            startIndex = 0,
            autoAdvance = false
        )
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
                ResonantGesture.HoldSpeedUp -> { if (audio.increaseSpeed()) haptics.play(HapticPattern.SPEED_UP) else haptics.play(HapticPattern.ERROR) }
                ResonantGesture.HoldSpeedDown -> { if (audio.decreaseSpeed()) haptics.play(HapticPattern.SPEED_DOWN) else haptics.play(HapticPattern.ERROR) }
                else -> {}
            }
        }) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = ScreenHorizontalPadding, vertical = 40.dp),
                verticalArrangement = Arrangement.Center
            ) {
                lessons.forEachIndexed { i, lesson ->
                    val focused = i == index
                    Column(
                        Modifier
                            .padding(vertical = 10.dp)
                            .clickable {
                                audio.jumpTo(i)
                                haptics.play(HapticPattern.SELECT)
                                audio.announce("Starting ${lesson.title}.")
                                onOpenLesson(lesson.id)
                            }
                    ) {
                        if (focused) {
                            // Solid filled chip — see Color.kt: a color swap on the text
                            // measured as low as 1.3:1 depending on gradient position; a
                            // solid fill is always ~19.8:1 regardless of where it sits.
                            Box(
                                Modifier
                                    .background(colors.focusedFill, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    lesson.title,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.6).sp
                                    ),
                                    color = colors.focusedText
                                )
                            }
                            Text(
                                "${lesson.sections.size} sections",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.text,
                                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                            )
                        } else {
                            Text(
                                lesson.title,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Normal,
                                    letterSpacing = (-0.6).sp
                                ),
                                color = colors.text
                            )
                        }
                    }
                }
            }
        }
    }
}