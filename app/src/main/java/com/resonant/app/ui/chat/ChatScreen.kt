package com.resonant.app.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resonant.app.content.ChatData
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
import com.resonant.app.ui.theme.ResonantGray
import com.resonant.app.ui.theme.ResonantOrange

private data class FlatChatUnit(val unit: SemanticUnit, val exchangeIndex: Int, val userText: String)

@Composable
fun ChatScreen() {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    val script = ChatData.sampleScript

    val flat = remember {
        script.exchanges.flatMapIndexed { ei, exchange ->
            exchange.assistantChunks.map { FlatChatUnit(it, ei, exchange.userText) }
        }
    }

    var flatIndex by remember { mutableIntStateOf(0) }
    var lastExchangeIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        debug.setScreen("AI Chat")
        audio.setQueue(flat.map { it.unit }, startIndex = 0, autoAdvance = true)
    }

    LaunchedEffect(Unit) {
        audio.index.collect { idx ->
            flatIndex = idx.coerceIn(0, (flat.size - 1).coerceAtLeast(0))
            val exchange = flat.getOrNull(flatIndex)?.exchangeIndex ?: 0
            if (exchange != lastExchangeIndex) {
                haptics.play(HapticPattern.SECTION_CHANGE)
                lastExchangeIndex = exchange
            }
        }
    }

    val current = flat.getOrNull(flatIndex)

    ResonantScaffold(
        title = script.title,
        subtitle = current?.let { "Exchange ${it.exchangeIndex + 1} of ${script.exchanges.size}" }
    ) {
        GestureSurface(onGesture = { gesture ->
            when (gesture) {
                is ResonantGesture.Swipe -> if (gesture.zone == InteractionZone.CENTER) {
                    when (gesture.direction) {
                        SwipeDirection.UP -> audio.next()
                        SwipeDirection.DOWN -> audio.previous()
                        else -> {}
                    }
                }
                is ResonantGesture.Tap -> if (gesture.zone == InteractionZone.LEFT_EDGE) {
                    audio.togglePause(); haptics.play(HapticPattern.CONFIRM)
                }
                ResonantGesture.ThreeFingerTap -> audio.repeatCurrent()
                ResonantGesture.ThreeFingerHold -> audio.announce(
                    "You are in AI Chat, exchange ${lastExchangeIndex + 1} of ${script.exchanges.size}."
                )
                ResonantGesture.HoldSpeedUp -> { audio.increaseSpeed(); haptics.play(HapticPattern.SPEED_UP) }
                ResonantGesture.HoldSpeedDown -> { audio.decreaseSpeed(); haptics.play(HapticPattern.SPEED_DOWN) }
                else -> {}
            }
        }) {
            EdgeHints()
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                Text(
                    "You: ${current?.userText ?: ""}",
                    style = MaterialTheme.typography.titleLarge,
                    color = ResonantGray
                )
                Text(
                    current?.unit?.text ?: "",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ResonantOrange,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    "Swipe up/down to move through the reply",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ResonantBlack,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }
}
