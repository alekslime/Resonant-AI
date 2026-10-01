package com.resonant.app.livekit

import android.content.Context
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import io.livekit.android.token.TokenRequestOptions
import io.livekit.android.token.TokenSource
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

    private val room: Room = LiveKit.create(appContext)

    private val tokenSource =
        TokenSource.fromDevelopmentTokenServer(TOKEN_SERVER_ID)

    suspend fun connect(
        participantName: String = "resonant-android"
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

                room.localParticipant.setMicrophoneEnabled(true)

                _state.value = LiveKitState.Connected
            } catch (e: Exception) {
                _state.value = LiveKitState.Error(
                    e.message ?: "Unknown LiveKit connection error"
                )
            }
        }
    }

    suspend fun disconnect() {
        mutex.withLock {
            room.disconnect()
            _state.value = LiveKitState.Disconnected
        }
    }
}
