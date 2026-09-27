package com.resonant.app.ui.lessons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import com.resonant.app.ui.theme.BrandInk
import com.resonant.app.ui.theme.MetropolisBlack
import com.resonant.app.ui.theme.ScreenHorizontalPadding

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

    // Single effect keyed on lesson.id: setQueue lands before collection starts,
    // and if lesson.id ever changes, both setup and the collector restart together
    // instead of the collector being left subscribed under a stale Unit key.
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
        // Title first, then the section's first unit queued behind it. The other order
        // has the title flush the unit the instant it starts, and nothing resumes it.
        audio.announce(lesson.sections[target].title)
        audio.jumpTo(idx, queueBehindAnnouncement = true)
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
                    .padding(horizontal = ScreenHorizontalPadding, vertical = 16.dp)
            ) {
                // Section progress bar
                val sectionFraction = if (lesson.sections.isEmpty()) 0f
                    else (lastSectionIndex + 1).toFloat() / lesson.sections.size
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF3A3A3A))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(sectionFraction)
                                .height(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(BrandInk)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Section ${lastSectionIndex + 1}/${lesson.sections.size}",
                        fontFamily = MetropolisBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BrandInk
                    )
                }

                // Main content card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1A1A1A))
                        .padding(24.dp)
                ) {
                    Column {
                        // Section title
                        current?.sectionTitle?.let { title ->
                            Text(
                                text = title,
                                fontFamily = MetropolisBlack,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color(0xFFFFAE00),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        // Unit text
                        Text(
                            text = current?.unit?.text ?: "",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black
                            ),
                            color = Color.White
                        )

                        Spacer(Modifier.height(24.dp))

                        // Unit counter
                        Text(
                            text = "Unit ${flatIndex + 1} of ${flat.size}",
                            fontFamily = MetropolisBlack,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = Color(0xFFAAAAAA)
                        )
                    }
                }

                // Bottom hint pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color(0xFF1A1A1A))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Swipe up · next   |   Swipe right · next section",
                        fontFamily = MetropolisBlack,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color(0xFF888888)
                    )
                }
            }
        }
    }
}
