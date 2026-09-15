package com.resonant.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resonant.app.content.SemanticUnit
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.gestures.InteractionZone
import com.resonant.app.gestures.ResonantGesture
import com.resonant.app.gestures.SwipeDirection
import com.resonant.app.haptics.HapticPattern
import com.resonant.app.ui.components.GestureSurface
import com.resonant.app.ui.theme.ResonantBlack
import com.resonant.app.ui.theme.ResonantYellow

private data class HomeItem(val label: String, val route: String)

private val homeItems = listOf(
    HomeItem("Lessons", "lessons"),
    HomeItem("Quiz", "quiz"),
    HomeItem("Chat", "chat"),
    HomeItem("Settings", "settings")
)

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        debug.setScreen("Home")
        audio.setQueue(
            homeItems.mapIndexed { i, item -> SemanticUnit("home_$i", item.label) },
            startIndex = 0,
            autoAdvance = false
        )
    }

    LaunchedEffect(Unit) {
        audio.index.collect { idx ->
            index = idx.coerceIn(0, (homeItems.size - 1).coerceAtLeast(0))
        }
    }

    Box(Modifier.fillMaxSize().background(ResonantYellow)) {

        // Decorative black left strip
        Box(
            Modifier
                .fillMaxHeight()
                .width(10.dp)
                .background(ResonantBlack)
        )

        Column(Modifier.fillMaxSize()) {
            // Header
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(ResonantBlack)
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp)
            ) {
                Column {
                    Text(
                        "Resonant",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ResonantYellow
                    )
                    Text(
                        "Swipe to browse. Tap to open.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ResonantYellow.copy(alpha = 0.6f)
                    )
                }
            }

            // Gesture surface
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
                            audio.announce("Opening ${homeItems[index].label}.")
                            onNavigate(homeItems[index].route)
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
                        audio.announce("You are already on the Home screen.")
                    }
                    ResonantGesture.ThreeFingerTap -> audio.repeatCurrent()
                    ResonantGesture.ThreeFingerHold -> audio.announce(
                        "You are on the Home screen. Currently focused: ${homeItems[index].label}."
                    )
                    ResonantGesture.HoldSpeedUp -> { if (audio.increaseSpeed()) haptics.play(HapticPattern.SPEED_UP) else haptics.play(HapticPattern.ERROR) }
                    ResonantGesture.HoldSpeedDown -> { if (audio.decreaseSpeed()) haptics.play(HapticPattern.SPEED_DOWN) else haptics.play(HapticPattern.ERROR) }
                    else -> {}
                }
            }) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(start = 32.dp, end = 24.dp, top = 40.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    homeItems.forEachIndexed { i, item ->
                        val focused = i == index
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = if (focused) FontWeight.Black else FontWeight.Normal
                            ),
                            color = if (focused) ResonantBlack else ResonantBlack.copy(alpha = 0.3f),
                            modifier = Modifier
                                .padding(vertical = 6.dp)
                                .clickable {
                                    audio.jumpTo(i)
                                    haptics.play(HapticPattern.SELECT)
                                    audio.announce("Opening ${item.label}.")
                                    onNavigate(item.route)
                                }
                                .semantics {
                                    contentDescription = item.label + if (focused) " focused" else ""
                                }
                        )
                    }
                }
            }
        }
    }
}
