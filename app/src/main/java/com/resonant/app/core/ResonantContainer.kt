package com.resonant.app.core

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.resonant.app.audio.AudioManager
import com.resonant.app.haptics.HapticManager
import com.resonant.app.network.OllamaConfig
import com.resonant.app.sound.SoundCueManager

/**
 * Resonant deliberately has no backend, database, or DI framework — this
 * container just holds the app's few singleton managers and hands them down
 * through composition, so every screen shares the same AudioManager /
 * HapticManager / DebugState instance instead of creating its own.
 */
class ResonantContainer(context: Context) {
    // prefs first: the managers below read their saved state from it.
    val prefs = ResonantPrefs(context)

    // Speech speed survives a restart. Restoring happens before the TTS engine
    // finishes starting, so the very first utterance already uses the saved rate.
    val audioManager = AudioManager(context).also { audio ->
        audio.restoreSpeedIndex(prefs.speedIndex)
        audio.onSpeedIndexChanged = { index -> prefs.speedIndex = index }
    }
    val soundCues = SoundCueManager(prefs)

    // Every haptic pattern also sounds its cue. See HapticManager.onPlay.
    val hapticManager = HapticManager(context).also { it.onPlay = soundCues::play }
    val debugState = DebugState()

    init {
        OllamaConfig.applyOverrides(prefs.ollamaBaseUrl, prefs.ollamaModel)
    }
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
