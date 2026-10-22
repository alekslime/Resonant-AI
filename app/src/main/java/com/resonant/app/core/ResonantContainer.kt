package com.resonant.app.core

import com.resonant.app.ui.theme.ActiveTextScale
import com.resonant.app.content.ActivityStore
import com.resonant.app.ui.theme.TextSpacings
import com.resonant.app.ui.theme.ActiveSpacing
import com.resonant.app.ui.theme.AppFonts
import com.resonant.app.ui.theme.ActiveFont
import com.resonant.app.ui.theme.ActiveMotion
import com.resonant.app.ui.theme.ActivePalette
import com.resonant.app.ui.theme.Palettes
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.staticCompositionLocalOf
import com.resonant.app.audio.AudioManager
import com.resonant.app.haptics.HapticManager
import com.resonant.app.network.OllamaConfig
import com.resonant.app.sound.HeadphoneDetector
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

    // The color theme is applied before the first screen draws.
    init {
        ActivePalette.current = Palettes.byId(prefs.colorTheme)
        ActiveFont.current = AppFonts.byId(prefs.font)
        ActiveSpacing.current = TextSpacings.byId(prefs.textSpacing)
        // Setting them above marks them as chosen; only a saved pick counts. With none, the theme's own type applies.
        ActiveFont.chosen = prefs.font != null
        ActiveSpacing.chosen = prefs.textSpacing != null
        ActiveMotion.userReduced = prefs.reduceMotion
        ActiveTextScale.current = prefs.textScale
        ActiveMotion.systemReduced =
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    // Speech speed survives a restart. Restoring happens before the TTS engine
    // finishes starting, so the very first utterance already uses the saved rate.
    val audioManager = AudioManager(context, prefs.ttsEngine).also { audio ->
        audio.onEngineChanged = { engine -> prefs.ttsEngine = engine }
        audio.restoreSpeedIndex(prefs.speedIndex)
        audio.onSpeedIndexChanged = { index -> prefs.speedIndex = index }
    }
    val soundCues = SoundCueManager(prefs, HeadphoneDetector(context))

    // Every haptic pattern also sounds its cue. See HapticManager.onPlay.
    val hapticManager = HapticManager(context).also { it.onPlay = soundCues::play }
    val debugState = DebugState()

    // Streaks, listening time and finished lessons, for the Profile screen.
    val activity = ActivityStore(context)

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
