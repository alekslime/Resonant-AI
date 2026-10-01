package com.resonant.app.livekit

sealed interface LiveKitState {
    data object Disconnected : LiveKitState
    data object Connecting : LiveKitState
    data object Connected : LiveKitState
    data class Error(val message: String) : LiveKitState
}
