package com.resonant.app.livekit

import android.content.Context
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import io.livekit.android.token.TokenRequestOptions
import io.livekit.android.token.TokenSource
import kotlinx.coroutines.CancellationException
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

    private val room: Room = LiveKit.create(appContext)

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
            } catch (e: CancellationException) {
                room.disconnect()
                _state.value = LiveKitState.Disconnected
                throw e
            } catch (e: Exception) {
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
            room.disconnect()
            _micEnabled.value = false
            _state.value = LiveKitState.Disconnected
        }
    }
}
