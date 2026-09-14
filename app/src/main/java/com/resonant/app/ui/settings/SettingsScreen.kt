package com.resonant.app.ui.settings

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.resonant.app.audio.AudioManager
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

private val settingsItems = listOf("Debug Mode")

@Composable
fun SettingsScreen(onOpenDebug: () -> Unit) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    var index by remember { mutableIntStateOf(0) }
    val speedIndex by audio.speedIndex.collectAsState()
    val speed = AudioManager.SPEEDS[speedIndex]

    LaunchedEffect(Unit) {
        debug.setScreen("Settings")
        audio.setQueue(
            settingsItems.mapIndexed { i, s -> SemanticUnit("settings_$i", s) },
            startIndex = 0,
            autoAdvance = false
        )
    }

    LaunchedEffect(Unit) {
        audio.index.collect { idx ->
            index = idx.coerceIn(0, (settingsItems.size - 1).coerceAtLeast(0))
        }
    }

    ResonantScaffold(title = "Settings", subtitle = "Swipe to browse. Tap to open.") {
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
                        if (settingsItems[index] == "Debug Mode") onOpenDebug()
                    }
                    InteractionZone.LEFT_EDGE -> {
                        audio.togglePause()
                        haptics.play(HapticPattern.CONFIRM)
                    }
                    InteractionZone.RIGHT_EDGE -> {}
                }
                is ResonantGesture.DoubleTap -> if (gesture.zone == InteractionZone.LEFT_EDGE) {
                    audio.repeatCurrent()
                }
                is ResonantGesture.LongPress -> if (gesture.zone == InteractionZone.RIGHT_EDGE) {
                    haptics.play(HapticPattern.BACK)
                }
                ResonantGesture.ThreeFingerTap -> audio.repeatCurrent()
                ResonantGesture.ThreeFingerHold -> audio.announce(
                    "You are in Settings. Speech speed is ${speed}x. Currently focused: ${settingsItems[index]}."
                )
                ResonantGesture.HoldSpeedUp -> { audio.increaseSpeed(); haptics.play(HapticPattern.SPEED_UP) }
                ResonantGesture.HoldSpeedDown -> { audio.decreaseSpeed(); haptics.play(HapticPattern.SPEED_DOWN) }
                else -> {}
            }
        }) {
            EdgeHints()
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                Text(
                    "Speech speed: ${speed}x",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ResonantBlack,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Text(
                    "Hold right edge, drag up/down to change speed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ResonantBlack,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                settingsItems.forEachIndexed { i, label ->
                    val focused = i == index
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (focused) ResonantOrange else ResonantWhite)
                            .clickable {
                                audio.jumpTo(i)
                                haptics.play(HapticPattern.SELECT)
                                if (label == "Debug Mode") onOpenDebug()
                            }
                            .padding(20.dp)
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (focused) ResonantWhite else ResonantBlack
                        )
                    }
                }
            }
        }
    }
}
