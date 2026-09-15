package com.resonant.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resonant.app.audio.AudioManager
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.gestures.InteractionZone
import com.resonant.app.gestures.ResonantGesture
import com.resonant.app.haptics.HapticPattern
import com.resonant.app.ui.components.GestureSurface
import com.resonant.app.ui.components.ResonantScaffold
import com.resonant.app.ui.theme.ResonantBlack
import com.resonant.app.ui.theme.ResonantWhite

@Composable
fun DebugScreen(onBack: () -> Unit) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current

    LaunchedEffect(Unit) { debug.setScreen("Debug") }

    val screen by debug.currentScreen.collectAsState()
    val zone by debug.currentZone.collectAsState()
    val lastGesture by debug.lastGesture.collectAsState()
    val selectedOption by debug.selectedOption.collectAsState()
    val lastHaptic by haptics.lastPattern.collectAsState()
    val currentUnit by audio.currentUnit.collectAsState()
    val isSpeaking by audio.isSpeaking.collectAsState()
    val isPaused by audio.isPaused.collectAsState()
    val speedIndex by audio.speedIndex.collectAsState()

    val speechState = when {
        isSpeaking -> "speaking"
        isPaused -> "paused"
        else -> "idle"
    }

    val rows = listOf(
        "Screen" to screen,
        "Semantic unit" to (currentUnit?.id ?: "—"),
        "Semantic text" to (currentUnit?.text ?: "—"),
        "Selected option" to selectedOption,
        "Speech state" to speechState,
        "Speech speed" to "${AudioManager.SPEEDS[speedIndex]}x",
        "Last gesture" to lastGesture,
        "Last haptic" to (lastHaptic?.name ?: "—"),
        "Current zone" to zone
    )

    ResonantScaffold(title = "Debug", subtitle = "Live interaction state") {
        GestureSurface(onGesture = { gesture ->
            when (gesture) {
                is ResonantGesture.LongPress -> if (gesture.zone == InteractionZone.RIGHT_EDGE) {
                    haptics.play(HapticPattern.BACK)
                    audio.announce("Back to Settings.")
                    onBack()
                }
                is ResonantGesture.Tap -> if (gesture.zone == InteractionZone.LEFT_EDGE) {
                    audio.togglePause(); haptics.play(HapticPattern.CONFIRM)
                }
                is ResonantGesture.DoubleTap -> if (gesture.zone == InteractionZone.LEFT_EDGE) audio.repeatCurrent()
                ResonantGesture.ThreeFingerTap -> audio.repeatCurrent()
                ResonantGesture.ThreeFingerHold -> audio.announce(
                    "Debug screen. Screen $screen. Last gesture $lastGesture. Zone $zone."
                )
                ResonantGesture.HoldSpeedUp -> { if (audio.increaseSpeed()) haptics.play(HapticPattern.SPEED_UP) else haptics.play(HapticPattern.ERROR) }
                ResonantGesture.HoldSpeedDown -> { if (audio.decreaseSpeed()) haptics.play(HapticPattern.SPEED_DOWN) else haptics.play(HapticPattern.ERROR) }
                else -> {}
            }
        }) {
            Column(Modifier.fillMaxSize().padding(start = 32.dp, end = 24.dp, top = 32.dp)) {
                rows.forEach { (label, value) ->
                    Column(Modifier.padding(bottom = 20.dp)) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = ResonantWhite
                        )
                        Text(
                            value,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = ResonantBlack
                        )
                    }
                }
            }
        }
    }
}
