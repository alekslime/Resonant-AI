package com.resonant.app.livekit

import android.content.Context
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
                } catch (e: Exception) {
                    _agentState.value = ""
                }
                delay(100)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
        _agentState.value = ""
        _agentPresent.value = false
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
