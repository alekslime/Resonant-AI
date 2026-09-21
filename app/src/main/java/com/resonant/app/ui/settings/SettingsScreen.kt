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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.ResonantApp
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
import com.resonant.app.ui.theme.LocalResonantColors
import com.resonant.app.ui.theme.ScreenHorizontalPadding

private const val REPLAY_TUTORIAL = "Replay Tutorial"
private const val SOUND_CUES = "Sound cues"
private const val DEBUG_MODE = "Debug Mode"

private val settingsItems = listOf(REPLAY_TUTORIAL, SOUND_CUES, DEBUG_MODE)

/** What is shown and spoken for an item. The toggle carries its state, so it is never a guess. */
private fun labelFor(item: String, soundCuesOn: Boolean) =
    if (item == SOUND_CUES) "$SOUND_CUES: ${if (soundCuesOn) "on" else "off"}" else item

@Composable
fun SettingsScreen(
    onOpenDebug: () -> Unit,
    onReplayTutorial: () -> Unit,
    onBack: () -> Unit
) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    val colors = LocalResonantColors.current
    val soundCues = (LocalContext.current.applicationContext as ResonantApp).container.soundCues
    var soundCuesOn by remember { mutableStateOf(soundCues.enabled) }
    var index by remember { mutableIntStateOf(0) }
    val speedIndex by audio.speedIndex.collectAsState()
    val speed = AudioManager.SPEEDS[speedIndex]

    // Single effect: setQueue must land before collection starts, or the first
    // collected value could be the pre-queue default instead of the real start.
    LaunchedEffect(Unit) {
        debug.setScreen("Settings")
        audio.setQueue(
            settingsItems.mapIndexed { i, s -> SemanticUnit("settings_$i", labelFor(s, soundCuesOn)) },
            startIndex = 0,
            autoAdvance = false
        )
        audio.index.collect { idx ->
            index = idx.coerceIn(0, (settingsItems.size - 1).coerceAtLeast(0))
        }
    }

    // One place for "the user chose this item", shared by the tap gesture and the on-screen
    // touch target, so the two can never disagree.
    fun activate(item: String) {
        when (item) {
            REPLAY_TUTORIAL -> onReplayTutorial()
            SOUND_CUES -> {
                val now = !soundCuesOn
                soundCues.enabled = now
                soundCuesOn = now
                // Re-queueing at the same position speaks the new state ("Sound cues: off").
                audio.setQueue(
                    settingsItems.mapIndexed { i, s -> SemanticUnit("settings_$i", labelFor(s, now)) },
                    startIndex = settingsItems.indexOf(SOUND_CUES),
                    autoAdvance = false
                )
                // Turning them on: sound one, so the change is heard and not only announced.
                if (now) haptics.play(HapticPattern.CONFIRM)
            }
            DEBUG_MODE -> onOpenDebug()
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
                        activate(settingsItems[index])
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
                    "Settings. Speaking at ${audio.speedLabel(speed)}. Hold the right edge and drag up to speed up, down to slow down. Currently focused: ${labelFor(settingsItems[index], soundCuesOn)}."
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
                Text(
                    "Hold right edge, drag up\nto speed up, down to slow.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.text,
                    modifier = Modifier.padding(bottom = 44.dp)
                )
                settingsItems.forEachIndexed { i, item ->
                    Text(
                        labelFor(item, soundCuesOn),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.6).sp
                        ),
                        color = colors.text,
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .clickable {
                                audio.jumpTo(i)
                                haptics.play(HapticPattern.SELECT)
                                activate(item)
                            }
                    )
                }
            }
        }
    }
}