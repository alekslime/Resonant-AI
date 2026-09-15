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
import androidx.compose.ui.text.font.FontWeight
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
import com.resonant.app.ui.components.GestureSurface
import com.resonant.app.ui.components.ResonantScaffold
import com.resonant.app.ui.theme.ResonantBlack

private data class FlatChatUnit(val unit: SemanticUnit, val exchangeIndex: Int, val userText: String)

@Composable
fun ChatScreen(onBack: () -> Unit) {
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
                        SwipeDirection.UP -> { audio.next(); haptics.play(HapticPattern.NEXT) }
                        SwipeDirection.DOWN -> { audio.previous(); haptics.play(HapticPattern.PREVIOUS) }
                        else -> {}
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
                    audio.announce("Back to Home.")
                    onBack()
                }
                ResonantGesture.ThreeFingerTap -> audio.repeatCurrent()
                ResonantGesture.ThreeFingerHold -> audio.announce(
                    "AI Chat, exchange ${lastExchangeIndex + 1} of ${script.exchanges.size}."
                )
                ResonantGesture.HoldSpeedUp -> { audio.increaseSpeed(); haptics.play(HapticPattern.SPEED_UP) }
                ResonantGesture.HoldSpeedDown -> { audio.decreaseSpeed(); haptics.play(HapticPattern.SPEED_DOWN) }
                else -> {}
            }
        }) {
            Column(Modifier.fillMaxSize().padding(start = 32.dp, end = 24.dp, top = 40.dp, bottom = 24.dp)) {
                Text(
                    "You: ${current?.userText ?: ""}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ResonantBlack.copy(alpha = 0.4f)
                )
                Text(
                    current?.unit?.text ?: "",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = ResonantBlack,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}
