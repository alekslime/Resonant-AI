package com.resonant.app.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.resonant.app.content.ChatData
import com.resonant.app.content.ChatExchange
import com.resonant.app.content.SemanticUnit
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.gestures.InteractionZone
import com.resonant.app.gestures.ResonantGesture
import com.resonant.app.gestures.SwipeDirection
import com.resonant.app.haptics.HapticPattern
import com.resonant.app.network.ChatMessage
import com.resonant.app.network.OllamaClient
import com.resonant.app.speech.SpeechInputManager
import com.resonant.app.ui.components.GestureSurface
import com.resonant.app.ui.components.ResonantScaffold
import com.resonant.app.ui.theme.ResonantBlack
import com.resonant.app.ui.theme.ResonantCaption
import kotlinx.coroutines.launch

private data class FlatChatUnit(val unit: SemanticUnit, val exchangeIndex: Int, val userText: String)

/** ". " / "? " / "! " boundaries — good enough to pace a short spoken answer. */
private fun splitIntoUnits(exchangeIndex: Int, text: String): List<SemanticUnit> =
    text.split(Regex("(?<=[.!?])\\s+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapIndexed { i, sentence -> SemanticUnit("e${exchangeIndex}u$i", sentence) }
        .ifEmpty { listOf(SemanticUnit("e${exchangeIndex}u0", text)) }

@Composable
fun ChatScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    val scope = rememberCoroutineScope()

    val speech = remember { SpeechInputManager(context) }
    DisposableEffect(Unit) { onDispose { speech.stopListening() } }

    var exchanges by remember { mutableStateOf<List<ChatExchange>>(emptyList()) }
    var flatIndex by remember { mutableIntStateOf(0) }
    var lastExchangeIndex by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) } // listening OR waiting on the model
    var statusText by remember { mutableStateOf("") }

    fun flatten(list: List<ChatExchange>): List<FlatChatUnit> =
        list.flatMapIndexed { ei, exchange ->
            exchange.assistantChunks.map { FlatChatUnit(it, ei, exchange.userText) }
        }

    fun speakLatestAnswer(list: List<ChatExchange>) {
        val flat = flatten(list)
        val newAnswerStart = flat.size - (list.lastOrNull()?.assistantChunks?.size ?: 0)
        audio.setQueue(flat.map { it.unit }, startIndex = newAnswerStart.coerceAtLeast(0), autoAdvance = true)
    }

    fun handleAnswer(userText: String, reply: String) {
        val exchangeIndex = exchanges.size
        val updated = exchanges + ChatExchange(userText, splitIntoUnits(exchangeIndex, reply))
        exchanges = updated
        speakLatestAnswer(updated)
        busy = false
        statusText = ""
    }

    fun handleError(message: String) {
        busy = false
        statusText = ""
        haptics.play(HapticPattern.ERROR)
        audio.announce(message)
    }

    fun askModel(userText: String) {
        statusText = "Thinking…"
        scope.launch {
            val history = buildList {
                add(ChatMessage("system", ChatData.systemPrompt))
                exchanges.forEach { ex ->
                    add(ChatMessage("user", ex.userText))
                    add(ChatMessage("assistant", ex.assistantChunks.joinToString(" ") { it.text }))
                }
                add(ChatMessage("user", userText))
            }
            OllamaClient.chat(history).fold(
                onSuccess = { reply -> handleAnswer(userText, reply) },
                onFailure = { err ->
                    handleError("I couldn't reach the AI. ${err.message ?: "Check your Ollama server is running and reachable."}")
                }
            )
        }
    }

    fun startListening() {
        busy = true
        statusText = "Listening…"
        haptics.play(HapticPattern.LISTENING)
        audio.announce("Listening.")
        speech.startListening { outcome ->
            when (outcome) {
                is SpeechInputManager.Outcome.Success -> {
                    haptics.play(HapticPattern.CONFIRM)
                    audio.announce("You said: ${outcome.text}")
                    askModel(outcome.text)
                }
                is SpeechInputManager.Outcome.Error -> handleError(outcome.message)
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening() else handleError("Microphone permission is needed to ask a question.")
    }

    fun onAskTapped() {
        if (busy) return
        val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (hasMic) startListening() else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(Unit) {
        debug.setScreen("AI Chat")
        audio.announce(ChatData.greeting)
        audio.index.collect { idx ->
            val flat = flatten(exchanges)
            flatIndex = idx.coerceIn(0, (flat.size - 1).coerceAtLeast(0))
            val exchange = flat.getOrNull(flatIndex)?.exchangeIndex ?: 0
            if (exchange != lastExchangeIndex) {
                haptics.play(HapticPattern.SECTION_CHANGE)
                lastExchangeIndex = exchange
            }
        }
    }

    val flat = flatten(exchanges)
    val current = flat.getOrNull(flatIndex)

    ResonantScaffold(
        title = ChatData.title,
        subtitle = if (exchanges.isEmpty()) "Tap center to ask a question" else "Exchange ${(current?.exchangeIndex ?: 0) + 1} of ${exchanges.size}"
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
                    InteractionZone.CENTER -> onAskTapped()
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
                    if (exchanges.isEmpty()) "AI Chat. Tap the center of the screen to ask a question."
                    else "AI Chat, exchange ${lastExchangeIndex + 1} of ${exchanges.size}. Tap center to ask another question."
                )
                ResonantGesture.HoldSpeedUp -> { if (audio.increaseSpeed()) haptics.play(HapticPattern.SPEED_UP) else haptics.play(HapticPattern.ERROR) }
                ResonantGesture.HoldSpeedDown -> { if (audio.decreaseSpeed()) haptics.play(HapticPattern.SPEED_DOWN) else haptics.play(HapticPattern.ERROR) }
                else -> {}
            }
        }) {
            Column(Modifier.fillMaxSize().padding(start = 32.dp, end = 24.dp, top = 40.dp, bottom = 24.dp)) {
                if (statusText.isNotEmpty()) {
                    Text(
                        statusText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ResonantCaption
                    )
                } else if (current != null) {
                    Text(
                        "You: ${current.userText}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ResonantCaption
                    )
                }
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
