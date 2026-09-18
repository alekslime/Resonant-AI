package com.resonant.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.ui.components.TtsUnavailableBanner
import com.resonant.app.ui.nav.ResonantNavHost
import com.resonant.app.ui.nav.Routes
import com.resonant.app.ui.theme.ResonantTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as ResonantApp).container

        // First launch drops straight into the tutorial; after that, Home.
        val start = if (container.prefs.onboardingComplete) Routes.HOME else Routes.ONBOARDING

        setContent {
            ResonantTheme {
                CompositionLocalProvider(
                    LocalAudioManager provides container.audioManager,
                    LocalHapticManager provides container.hapticManager,
                    LocalDebugState provides container.debugState
                ) {
                    Box(Modifier.fillMaxSize()) {
                        ResonantNavHost(startDestination = start)
                        TtsUnavailableBanner(container.audioManager, container.hapticManager)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as ResonantApp).container.audioManager.restoreFromBackground()
    }

    /**
     * Speech must not continue once the app is off-screen. onStop (not onPause)
     * so a transient overlay — permission dialog, notification shade — doesn't
     * cut the user off mid-sentence.
     */
    override fun onStop() {
        super.onStop()
        (application as ResonantApp).container.audioManager.releaseForBackground()
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as ResonantApp).container.audioManager.shutdown()
    }
}
