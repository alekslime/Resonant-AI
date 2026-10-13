package com.resonant.app.livekit

sealed interface LiveKitState {
    data object Disconnected : LiveKitState
    data object Connecting : LiveKitState
    data object Connected : LiveKitState
    /** [spoken] and [short] say what went wrong and what to check; see [LiveErrors]. */
    data class Error(
        val message: String,
        val spoken: String = LiveErrors.GENERIC_SPOKEN,
        val short: String = LiveErrors.GENERIC_SHORT
    ) : LiveKitState
}
