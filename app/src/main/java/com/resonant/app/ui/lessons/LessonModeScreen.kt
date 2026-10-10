package com.resonant.app.ui.lessons

import com.resonant.app.ui.theme.ResonantCard
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private data class ModeItem(val label: String, val auto: Boolean, val spoken: String)

private val modes = listOf(
    ModeItem("MANUAL", false, "Manual. You tap the center of the screen to hear each sentence."),
    ModeItem("AUTO", true, "Auto. The lesson plays straight through on its own.")
)

/**
 * Figma "lessons" frame 2: after picking a lesson, choose Manual (tap for each next sentence)
 * or Auto (plays through). Same gestures as Home: swipe up/down to move between the two,
 * tap the center to choose; tapping a card chooses it directly.
 */
@Composable
fun LessonModeScreen(onPick: (auto: Boolean) -> Unit, onBack: () -> Unit, onOpenSettings: (() -> Unit)? = null) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        debug.setScreen("Lesson mode")
        audio.announce("How do you want to go through this lesson? Swipe up or down to choose, then tap the center.")
        audio.setQueue(
            modes.mapIndexed { i, m -> SemanticUnit("lesson_mode_$i", m.spoken) },
            startIndex = 0,
            autoAdvance = false,
            queueBehindAnnouncement = true
        )
        audio.index.collect { idx -> index = idx.coerceIn(0, modes.lastIndex) }
    }

    fun pick(i: Int) {
        haptics.play(HapticPattern.SELECT)
        onPick(modes[i].auto)
    }

    SystemBarsColor(BrandOrange)

    Box(Modifier.fillMaxSize().background(BrandOrange)) {
        GestureSurface(ignoreChildTaps = true, onGesture = { gesture ->
            when (gesture) {
                is ResonantGesture.Swipe -> if (gesture.zone == InteractionZone.CENTER) {
                    when (gesture.direction) {
                        SwipeDirection.UP -> if (audio.next()) haptics.play(HapticPattern.NEXT) else haptics.play(HapticPattern.EDGE)
                        SwipeDirection.DOWN -> if (audio.previous()) haptics.play(HapticPattern.PREVIOUS) else haptics.play(HapticPattern.EDGE)
                        else -> {}
                    }
                }
                is ResonantGesture.Tap -> when (gesture.zone) {
                    InteractionZone.CENTER -> pick(index)
                    InteractionZone.LEFT_EDGE -> { audio.togglePause(); haptics.play(HapticPattern.CONFIRM) }
                    InteractionZone.RIGHT_EDGE -> {}
                }
                is ResonantGesture.DoubleTap -> if (gesture.zone == InteractionZone.LEFT_EDGE) audio.repeatCurrent()
                is ResonantGesture.LongPress -> if (gesture.zone == InteractionZone.RIGHT_EDGE) {
                    haptics.play(HapticPattern.BACK)
                    audio.announce("Back to Lessons.")
                    onBack()
                }
                ResonantGesture.ThreeFingerTap -> audio.repeatCurrent()
                ResonantGesture.ThreeFingerHold -> audio.announce(
                    "Choose Manual or Auto. Currently focused: ${modes[index].label.lowercase()}."
                )
                ResonantGesture.HoldSpeedUp -> { if (audio.increaseSpeed()) haptics.play(HapticPattern.SPEED_UP) else haptics.play(HapticPattern.ERROR) }
                ResonantGesture.HoldSpeedDown -> { if (audio.decreaseSpeed()) haptics.play(HapticPattern.SPEED_DOWN) else haptics.play(HapticPattern.ERROR) }
                else -> {}
            }
        }) {
            Column(Modifier.fillMaxSize()) {
                LessonsTopBar(onBack = onBack, backAnnouncement = "Back to Lessons.", onSettings = onOpenSettings)

                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    modes.forEachIndexed { i, m ->
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(32.dp))
                                .background(ResonantCard)
                                .clickable { audio.jumpTo(i); pick(i) }
                                .semantics {
                                    contentDescription = m.spoken + if (i == index) " Focused." else ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = m.label,
                                fontFamily = MetropolisBlack,
                                fontWeight = FontWeight.Black,
                                fontSize = 54.sp,
                                color = BrandInk
                            )
                        }
                    }
                }
            }
        }
    }
}
