package com.resonant.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.audio.AudioManager
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
import com.resonant.app.ui.theme.ResonantCaption
import com.resonant.app.ui.theme.ResonantUnfocused

private const val REPLAY_TUTORIAL = "Replay Tutorial"
private const val DEBUG_MODE = "Debug Mode"

private val settingsItems = listOf(REPLAY_TUTORIAL, DEBUG_MODE)

@Composable
fun SettingsScreen(
    onOpenDebug: () -> Unit,
    onReplayTutorial: () -> Unit,
    onBack: () -> Unit
) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    var index by remember { mutableIntStateOf(0) }
    val speedIndex by audio.speedIndex.collectAsState()
    val speed = AudioManager.SPEEDS[speedIndex]

    // Single effect: setQueue must land before collection starts, or the first
    // collected value could be the pre-queue default instead of the real start.
    LaunchedEffect(Unit) {
        debug.setScreen("Settings")
        audio.setQueue(
            settingsItems.mapIndexed { i, s -> SemanticUnit("settings_$i", s) },
            startIndex = 0,
            autoAdvance = false
        )
        audio.index.collect { idx ->
            index = idx.coerceIn(0, (settingsItems.size - 1).coerceAtLeast(0))
        }
    }

    ResonantScaffold(title = "Settings", subtitle = "Speech speed: ${speed}x") {
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
                        when (settingsItems[index]) {
                            REPLAY_TUTORIAL -> onReplayTutorial()
                            DEBUG_MODE -> onOpenDebug()
                        }
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
                    "Settings. Speech speed is ${speed}x. Hold the right edge and drag up to speed up, down to slow down. Currently focused: ${settingsItems[index]}."
                )
                ResonantGesture.HoldSpeedUp -> { if (audio.increaseSpeed()) haptics.play(HapticPattern.SPEED_UP) else haptics.play(HapticPattern.ERROR) }
                ResonantGesture.HoldSpeedDown -> { if (audio.decreaseSpeed()) haptics.play(HapticPattern.SPEED_DOWN) else haptics.play(HapticPattern.ERROR) }
                else -> {}
            }
        }) {
            Column(
                Modifier.fillMaxSize().padding(start = 32.dp, end = 24.dp, top = 40.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Hold right edge, drag up\nto speed up, down to slow.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ResonantCaption,
                    modifier = Modifier.padding(bottom = 40.dp)
                )
                settingsItems.forEachIndexed { i, label ->
                    val focused = i == index
                    Text(
                        label,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.6).sp
                        ),
                        color = if (focused) ResonantBlack else ResonantUnfocused,
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .clickable {
                                audio.jumpTo(i)
                                haptics.play(HapticPattern.SELECT)
                                when (label) {
                                    REPLAY_TUTORIAL -> onReplayTutorial()
                                    DEBUG_MODE -> onOpenDebug()
                                }
                            }
                    )
                }
            }
        }
    }
}
