package com.resonant.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.ui.nav.ResonantNavHost
import com.resonant.app.ui.theme.ResonantTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as ResonantApp).container

        setContent {
            ResonantTheme {
                CompositionLocalProvider(
                    LocalAudioManager provides container.audioManager,
                    LocalHapticManager provides container.hapticManager,
                    LocalDebugState provides container.debugState
                ) {
                    ResonantNavHost()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as ResonantApp).container.audioManager.shutdown()
    }
}
