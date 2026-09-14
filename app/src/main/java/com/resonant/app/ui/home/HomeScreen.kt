package com.resonant.app.ui.home

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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

private data class HomeItem(val label: String, val route: String)

private val homeItems = listOf(
    HomeItem("Lessons", "lessons"),
    HomeItem("Quiz", "quiz"),
    HomeItem("AI Chat", "chat"),
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

    // Keep visual index in sync with audio queue
    LaunchedEffect(Unit) {
        audio.index.collect { idx ->
            index = idx.coerceIn(0, (homeItems.size - 1).coerceAtLeast(0))
        }
    }

    ResonantScaffold(title = "Resonant", subtitle = "Swipe to browse. Tap to open.") {
        GestureSurface(onGesture = { gesture ->
            when (gesture) {
                is ResonantGesture.Swipe -> if (gesture.zone == InteractionZone.CENTER) {
                    when (gesture.direction) {
                        SwipeDirection.UP -> {
                            if (audio.next()) haptics.play(HapticPattern.NEXT)
                        }
                        SwipeDirection.DOWN -> {
                            if (audio.previous()) haptics.play(HapticPattern.PREVIOUS)
                        }
                        else -> {}
                    }
                }
                // CENTER tap = select/confirm
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
                    InteractionZone.RIGHT_EDGE -> { /* dead zone */ }
                }
                // Left edge double-tap = repeat current
                is ResonantGesture.DoubleTap -> if (gesture.zone == InteractionZone.LEFT_EDGE) {
                    audio.repeatCurrent()
                }
                // Right edge long press = contextual (no-op on home)
                is ResonantGesture.LongPress -> {}
                ResonantGesture.ThreeFingerTap -> audio.repeatCurrent()
                ResonantGesture.ThreeFingerHold -> audio.announce(
                    "You are on the Home screen. Currently focused: ${homeItems[index].label}."
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
                homeItems.forEachIndexed { i, item ->
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
                                audio.announce("Opening ${item.label}.")
                                onNavigate(item.route)
                            }
                            .padding(20.dp)
                            .semantics { contentDescription = item.label + if (focused) " focused" else "" }
                    ) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (focused) ResonantWhite else ResonantBlack
                        )
                    }
                }
            }
        }
    }
}
