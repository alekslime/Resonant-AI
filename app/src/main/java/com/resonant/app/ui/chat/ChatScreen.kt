package com.resonant.app.ui.chat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
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
import com.resonant.app.content.OfflineAnswers
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
import com.resonant.app.network.OllamaConfig
import com.resonant.app.network.checkOllamaConnection
import com.resonant.app.network.isServerUnavailable
import com.resonant.app.network.serverIsUsable
import com.resonant.app.network.serverNotice
import com.resonant.app.network.spokenErrorFor
import com.resonant.app.network.warmUpModel
import com.resonant.app.speech.SentenceChunker
import com.resonant.app.speech.SpeechInputManager
import com.resonant.app.speech.VoiceInputController
import com.resonant.app.ui.components.GestureSurface
import com.resonant.app.ui.components.ResonantScaffold
import com.resonant.app.ui.theme.LocalResonantColors
import com.resonant.app.ui.theme.ScreenHorizontalPadding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private data class FlatChatUnit(val unit: SemanticUnit, val exchangeIndex: Int, val userText: String)

/** Gap between the soft "still working" pulses while waiting for the model's first sentence. */
private const val THINKING_TICK_MS = 2_000L

/** Every this-many ms of waiting, say so out loud as well — a pulse alone doesn't say "cancel is possible". */
private const val THINKING_SPOKEN_EVERY_MS = 10_000L

/**
 * A look at a server we already think is down, taken when a question is asked. Short on
 * purpose: a dead server should cost a couple of seconds, not the full connect timeout.
 */
private const val RECHECK_TIMEOUT_MS = 2_000

/** How long the check made on entry waits for the greeting to finish before it stays quiet. */
private const val NOTICE_WAIT_MS = 20_000L

@Composable
fun ChatScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    val colors = LocalResonantColors.current
    val scope = rememberCoroutineScope()

    val speech = remember { VoiceInputController(context) }
    var alive by remember { mutableStateOf(true) }
    var requestJob by remember { mutableStateOf<Job?>(null) }
    // Bumped every time a request is cancelled or replaced. A cancelled request's
    // `finally` still runs later; comparing ids stops it from resetting the state of
    // the request (or the listening session) that superseded it.
    var requestId by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        onDispose {
            alive = false
            speech.stopListening()
            speech.release() // frees the native Whisper model — nothing does this automatically
            requestJob?.cancel()
            audio.endStream()
        }
    }

    var exchanges by remember { mutableStateOf<List<ChatExchange>>(emptyList()) }
    var flatIndex by remember { mutableIntStateOf(0) }
    var lastExchangeIndex by remember { mutableIntStateOf(0) }
    var listening by remember { mutableStateOf(false) }  // microphone open
    var thinking by remember { mutableStateOf(false) }   // question sent, no sentence back yet
    var statusText by remember { mutableStateOf("") }
    var micPermanentlyDenied by remember { mutableStateOf(false) }
    // What we last learned about the AI server: null = not known yet, false = unusable, so
    // questions go to the lessons (after a quick look), true = fine.
    var serverUp by remember { mutableStateOf<Boolean?>(null) }

    fun flatten(list: List<ChatExchange>): List<FlatChatUnit> =
        list.flatMapIndexed { ei, exchange ->
            exchange.assistantChunks.map { FlatChatUnit(it, ei, exchange.userText) }
        }

    fun resetBusy() {
        listening = false
        thinking = false
        statusText = ""
    }

    fun handleError(message: String) {
        resetBusy()
        haptics.play(HapticPattern.ERROR)
        audio.announce(message)
    }

    /** Stop waiting for (or streaming) an answer. Safe to call when nothing is in flight. */
    fun cancelRequest() {
        requestId++
        requestJob?.cancel()
        requestJob = null
        audio.endStream()
        thinking = false
        statusText = ""
    }

    fun askModel(userText: String) {
        cancelRequest()
        val myId = requestId
        thinking = true
        statusText = "Thinking…"

        requestJob = scope.launch {
            // Silence reads as a frozen app to someone who can't see "Thinking…". Pulse
            // softly, and every so often say so — including that a tap will cancel.
            val ticker = launch {
                var waited = 0L
                while (true) {
                    delay(THINKING_TICK_MS)
                    waited += THINKING_TICK_MS
                    if (waited % THINKING_SPOKEN_EVERY_MS == 0L) {
                        audio.announce("Still thinking. Tap the center to cancel.")
                    } else {
                        haptics.play(HapticPattern.THINKING)
                    }
                }
            }

            val history = buildList {
                add(ChatMessage("system", ChatData.systemPrompt))
                exchanges.forEach { ex ->
                    add(ChatMessage("user", ex.userText))
                    add(ChatMessage("assistant", ex.assistantChunks.joinToString(" ") { it.text }))
                }
                add(ChatMessage("user", userText))
            }

            val chunker = SentenceChunker()
            var sentenceCount = 0

            // Speak the first sentence as soon as it exists; append the rest as they land.
            fun onSentence(sentence: String) {
                if (sentenceCount == 0) {
                    ticker.cancel()
                    thinking = false
                    statusText = ""
                    val exchangeIndex = exchanges.size
                    val unit = SemanticUnit("e${exchangeIndex}u0", sentence)
                    exchanges = exchanges + ChatExchange(userText, listOf(unit))
                    val flat = flatten(exchanges)
                    // Queued behind "You said … Thinking." rather than cutting it off.
                    audio.setQueue(
                        flat.map { it.unit },
                        startIndex = flat.size - 1,
                        autoAdvance = true,
                        queueBehindAnnouncement = true
                    )
                } else {
                    val last = exchanges.last()
                    val unit = SemanticUnit("e${exchanges.lastIndex}u$sentenceCount", sentence)
                    exchanges = exchanges.dropLast(1) + last.copy(assistantChunks = last.assistantChunks + unit)
                    audio.appendUnits(listOf(unit))
                }
                sentenceCount++
            }

            // When the AI server can't answer, the lesson text does: a degraded demo, not a dead one.
            fun answerFromLessons() {
                OfflineAnswers.answer(userText).sentences.forEach { onSentence(it) }
            }

            suspend fun serverCameBack(): Boolean {
                val check = checkOllamaConnection(OllamaConfig.BASE_URL, OllamaConfig.MODEL, RECHECK_TIMEOUT_MS)
                val usable = serverIsUsable(check)
                if (usable) serverUp = true
                return usable
            }

            audio.beginStream()
            try {
                // We already believe the server is down (the check made when Chat opened, or an
                // earlier failure). Take a quick look instead of waiting out a long connect
                // timeout for an answer that is probably not coming.
                val skipNetwork = serverUp == false && !serverCameBack()
                if (skipNetwork) {
                    answerFromLessons()
                } else {
                    try {
                        OllamaClient.chatStream(history).collect { delta ->
                            chunker.feed(delta).forEach { onSentence(it) }
                        }
                        chunker.flush()?.let { onSentence(it) }
                        if (sentenceCount == 0) handleError("The AI didn't reply. Tap the center to try again.")
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // Nothing has been said yet and the server can't serve us: answer from the
                        // lessons instead of only reporting the failure.
                        if (sentenceCount == 0 && isServerUnavailable(e) && requestId == myId) {
                            Log.w("ResonantChat", "AI server unavailable, answering from the lessons", e)
                            serverUp = false
                            answerFromLessons()
                        } else {
                            throw e
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (requestId == myId) {
                    // The exception's own text can be an HTTP body or a raw socket error:
                    // log that, and speak something a person can act on.
                    Log.w("ResonantChat", "Chat request failed", e)
                    // It broke part-way through an answer. Next question looks before it leaps.
                    if (isServerUnavailable(e)) serverUp = false
                    handleError(spokenErrorFor(e))
                }
            } finally {
                ticker.cancel()
                if (requestId == myId) {
                    audio.endStream()
                    thinking = false
                    statusText = ""
                }
            }
        }
    }

    fun startListening() {
        // Barge-in: a new question interrupts any answer still streaming or speaking.
        cancelRequest()
        audio.stop()
        listening = true
        statusText = "Listening…"
        haptics.play(HapticPattern.LISTENING)
        // The microphone opens only AFTER "Listening." has finished. Started together,
        // the recognizer can pick up the app's own voice as the user's question.
        audio.announce("Listening.") {
            if (alive && listening) {
                speech.startListening { outcome ->
                    listening = false
                    when (outcome) {
                        is SpeechInputManager.Outcome.Success -> {
                            haptics.play(HapticPattern.CONFIRM)
                            // One announcement, not two: a second announce() would flush this one,
                            // and the reply's first sentence is queued behind it (see askModel).
                            audio.announce("You said: ${outcome.text}. Thinking.")
                            askModel(outcome.text)
                        }
                        is SpeechInputManager.Outcome.Error -> handleError(outcome.message)
                    }
                }
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startListening()
        } else {
            val activity = context as? Activity
            val canAskAgain = activity == null ||
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
            if (canAskAgain) {
                handleError("Microphone permission is needed to ask a question.")
            } else {
                // "Don't ask again" was chosen (or the system otherwise won't
                // show the dialog anymore) — re-requesting from here on would
                // silently no-op forever with no way out. Route future taps to
                // the app's Settings page instead of repeating a dead end.
                micPermanentlyDenied = true
                handleError("Microphone permission was denied. Tap center again to open Settings and allow it.")
            }
        }
    }

    fun onAskTapped() {
        // A tap while waiting for the model cancels the wait — otherwise the only
        // way out of a slow or hung request would be sitting through the timeout.
        if (thinking) {
            cancelRequest()
            haptics.play(HapticPattern.BACK)
            audio.announce("Cancelled. Tap the center to ask again.")
            return
        }
        if (listening) return
        if (micPermanentlyDenied) {
            audio.announce("Opening settings. Turn on the microphone permission for Resonant.")
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
            return
        }
        val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (hasMic) startListening() else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(Unit) {
        debug.setScreen("AI Chat")
        // Start from an empty queue. AudioManager is shared by every screen, so without
        // this next/previous/repeat here would walk the previous screen's items (Home's
        // menu) until the first reply arrives.
        audio.setQueue(emptyList(), autoAdvance = true)
        val greetingDone = CompletableDeferred<Unit>()
        audio.announce(ChatData.greeting) { greetingDone.complete(Unit) }

        // Find out now, not after someone has asked a question and waited, whether the server is
        // usable. If it is, load the model in the background so the first answer isn't the slow
        // one; if not, say so once and answer from the lessons instead.
        launch {
            val check = checkOllamaConnection(OllamaConfig.BASE_URL, OllamaConfig.MODEL)
            if (serverIsUsable(check)) {
                serverUp = true
                warmUpModel(OllamaConfig.BASE_URL, OllamaConfig.MODEL)
            } else {
                serverUp = false
                // Never talk over the greeting, and stay quiet if the person has already begun.
                withTimeoutOrNull(NOTICE_WAIT_MS) { greetingDone.await() }
                if (alive && exchanges.isEmpty() && !listening && !thinking) {
                    audio.announce(serverNotice(check))
                }
            }
        }

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
        subtitle = when {
            thinking -> "Tap center to cancel"
            exchanges.isEmpty() -> "Tap center to ask a question"
            else -> "Exchange ${(current?.exchangeIndex ?: 0) + 1} of ${exchanges.size}"
        }
    ) {
        GestureSurface(onGesture = { gesture ->
            when (gesture) {
                is ResonantGesture.Swipe -> if (gesture.zone == InteractionZone.CENTER) {
                    when (gesture.direction) {
                        SwipeDirection.UP -> haptics.play(if (audio.next()) HapticPattern.NEXT else HapticPattern.EDGE)
                        SwipeDirection.DOWN -> haptics.play(if (audio.previous()) HapticPattern.PREVIOUS else HapticPattern.EDGE)
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
            Column(Modifier.fillMaxSize().padding(horizontal = ScreenHorizontalPadding, vertical = 32.dp)) {
                if (statusText.isNotEmpty()) {
                    Text(
                        statusText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.text
                    )
                } else if (current != null) {
                    Text(
                        "You: ${current.userText}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.text
                    )
                }
                Text(
                    current?.unit?.text ?: "",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = colors.text,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
        }
    }
}
