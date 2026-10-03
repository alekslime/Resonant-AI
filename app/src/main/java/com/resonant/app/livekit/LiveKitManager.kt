package com.resonant.app.livekit

import android.content.Context
import com.resonant.app.core.ResonantPrefs
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import io.livekit.android.token.TokenRequestOptions
import io.livekit.android.token.TokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

/** One line of live captions from the PC agent. role: "user", "assistant" (one sentence) or "done". */
data class LiveCaption(val role: String, val text: String)

class LiveKitManager(
    context: Context
) {
    companion object {
        private const val TOKEN_SERVER_ID = "resonant-1dvkl5"
    }

    private val appContext = context.applicationContext
    private val mutex = Mutex()

    private val _state = MutableStateFlow<LiveKitState>(LiveKitState.Disconnected)
    val state: StateFlow<LiveKitState> = _state.asStateFlow()

    private val _micEnabled = MutableStateFlow(false)
    /** True while the microphone is published to the room (i.e. not muted). */
    val micEnabled: StateFlow<Boolean> = _micEnabled.asStateFlow()

    private val _agentState = MutableStateFlow("")
    /**
     * What the PC agent says it is doing: "listening", "transcribing", "thinking" or
     * "speaking". Empty when unknown (not connected, or no agent in the room).
     */
    val agentState: StateFlow<String> = _agentState.asStateFlow()

    private val _captions = MutableSharedFlow<LiveCaption>(extraBufferCapacity = 64)
    /** Captions from the PC agent, in order, each delivered once. */
    val captions: SharedFlow<LiveCaption> = _captions.asSharedFlow()
    private var lastCaptionN = 0L
    private var lastCaptionRaw: String? = null

    private val _agentPresent = MutableStateFlow(false)
    /** True while anyone else (the PC agent) is in the room. */
    val agentPresent: StateFlow<Boolean> = _agentPresent.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollJob: Job? = null
    private var interruptCount = 0

    private val room: Room = LiveKit.create(appContext)

    // The agent publishes its state as a participant attribute. Polling the attribute is a few
    // map reads every 100 ms, and avoids depending on the SDK's event API.
    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                try {
                    _agentPresent.value = room.remoteParticipants.isNotEmpty()
                    _agentState.value =
                        room.remoteParticipants.values.firstNotNullOfOrNull { it.attributes["state"] } ?: ""
                    readCaptions(room.remoteParticipants.values.firstNotNullOfOrNull { it.attributes["captions"] })
                } catch (e: Exception) {
                    _agentState.value = ""
                }
                delay(100)
            }
        }
    }

    // The agent publishes the last few captions as one JSON array, each numbered ("n"). We emit
    // every item newer than the last one we saw, so nothing is lost or repeated between polls.
    private fun readCaptions(raw: String?) {
        if (raw == null || raw == lastCaptionRaw) return
        lastCaptionRaw = raw
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val n = item.optLong("n", 0L)
                if (n > lastCaptionN) {
                    lastCaptionN = n
                    _captions.tryEmit(LiveCaption(item.optString("role"), item.optString("text")))
                }
            }
        } catch (e: Exception) {
            // Malformed caption: skip it, the next one will come through.
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
        _agentState.value = ""
        _agentPresent.value = false
        lastCaptionN = 0L
        lastCaptionRaw = null
    }

    /** Tell the PC agent to stop thinking / talking now. No-op unless connected. */
    fun interruptAgent() {
        if (_state.value !is LiveKitState.Connected) return
        interruptCount += 1
        val n = interruptCount
        // Two routes, because token permissions differ: attributes need "update own metadata",
        // data packets need "publish data". The agent handles whichever arrives (it is idempotent).
        scope.launch {
            try {
                room.localParticipant.updateAttributes(mapOf("interrupt" to n.toString()))
            } catch (e: Exception) {
            }
            try {
                room.localParticipant.publishData("interrupt".toByteArray(), topic = "interrupt")
            } catch (e: Exception) {
            }
        }
    }

    /**
     * Send the Live voice and speed chosen in Settings to the PC agent. Two routes, like
     * [interruptAgent]: attributes stay on the participant (so an agent that joins later still
     * reads them), the data packet reaches an agent that is already in the room.
     */
    private fun sendVoiceSettings() {
        val prefs = ResonantPrefs(appContext)
        val voice = prefs.liveVoice
        val speed = prefs.liveSpeed
        scope.launch {
            try {
                room.localParticipant.updateAttributes(mapOf("voice" to voice, "speed" to speed.toString()))
            } catch (e: Exception) {
            }
            try {
                val json = JSONObject().put("voice", voice).put("speed", speed.toDouble()).toString()
                room.localParticipant.publishData(json.toByteArray(), topic = "voice")
            } catch (e: Exception) {
            }
        }
    }

    private val tokenSource =
        TokenSource.fromDevelopmentTokenServer(TOKEN_SERVER_ID)

    suspend fun connect(
        participantName: String = "resonant-android",
        enableMicrophone: Boolean = true
    ) {
        mutex.withLock {
            if (_state.value is LiveKitState.Connecting ||
                _state.value is LiveKitState.Connected
            ) {
                return
            }

            _state.value = LiveKitState.Connecting

            try {
                val credentials = tokenSource.fetch(
                    TokenRequestOptions(
                        participantName = participantName
                    )
                ).getOrThrow()

                room.connect(
                    credentials.serverUrl,
                    credentials.participantToken
                )

                if (enableMicrophone) {
                    room.localParticipant.setMicrophoneEnabled(true)
                    _micEnabled.value = true
                }

                _state.value = LiveKitState.Connected
                startPolling()
                sendVoiceSettings()
            } catch (e: CancellationException) {
                stopPolling()
                room.disconnect()
                _state.value = LiveKitState.Disconnected
                throw e
            } catch (e: Exception) {
                stopPolling()
                room.disconnect() // don't leave a half-open room behind
                _state.value = LiveKitState.Error(
                    e.message ?: "Unknown LiveKit connection error"
                )
            }
        }
    }

    /** Mute / unmute the microphone without leaving the room. No-op unless connected. */
    suspend fun setMicrophoneEnabled(enabled: Boolean) {
        mutex.withLock {
            if (_state.value !is LiveKitState.Connected) return
            try {
                room.localParticipant.setMicrophoneEnabled(enabled)
                _micEnabled.value = enabled
            } catch (e: Exception) {
                // Leave the flag as it was; the UI keeps showing the real state.
            }
        }
    }

    suspend fun disconnect() {
        mutex.withLock {
            stopPolling()
            room.disconnect()
            _micEnabled.value = false
            _state.value = LiveKitState.Disconnected
        }
    }
}
