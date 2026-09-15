package com.resonant.app.core

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.resonant.app.audio.AudioManager
import com.resonant.app.haptics.HapticManager

/**
 * Resonant deliberately has no backend, database, or DI framework — this
 * container just holds the app's few singleton managers and hands them down
 * through composition, so every screen shares the same AudioManager /
 * HapticManager / DebugState instance instead of creating its own.
 */
class ResonantContainer(context: Context) {
    val audioManager = AudioManager(context)
    val hapticManager = HapticManager(context)
    val debugState = DebugState()
    val prefs = ResonantPrefs(context)
}

val LocalAudioManager = staticCompositionLocalOf<AudioManager> {
    error("AudioManager not provided")
}
val LocalHapticManager = staticCompositionLocalOf<HapticManager> {
    error("HapticManager not provided")
}
val LocalDebugState = staticCompositionLocalOf<DebugState> {
    error("DebugState not provided")
}
