package com.resonant.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.ui.components.TtsUnavailableBanner
import com.resonant.app.ui.nav.ResonantNavHost
import com.resonant.app.ui.nav.Routes
import com.resonant.app.ui.talkback.TalkBackNoticeScreen
import com.resonant.app.ui.theme.ResonantTheme

/**
 * Resonant's whole interaction model is its own custom gesture detector
 * (see GestureSurface) reading raw touches directly — there is no standard
 * Compose click/semantics layer under it. If TalkBack (or any screen reader
 * that turns on touch exploration) is already running, Android intercepts
 * touches before Resonant's detector ever sees them: taps become "select",
 * double-tap becomes "activate", swipes move screen-reader focus instead of
 * triggering Resonant's gestures. The two systems are not compatible, so we
 * check for it explicitly rather than let a blind user — the actual target
 * user — hit a silently broken gesture surface.
 */
private fun isTouchExplorationOn(am: AccessibilityManager?) = am?.isTouchExplorationEnabled == true

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as ResonantApp).container
        val accessibilityManager = getSystemService(AccessibilityManager::class.java)

        // First launch drops straight into the tutorial; after that, Home.
        val start = if (container.prefs.onboardingComplete) Routes.HOME else Routes.ONBOARDING

        setContent {
            ResonantTheme {
                val context = LocalContext.current
                var touchExplorationOn by remember {
                    mutableStateOf(isTouchExplorationOn(accessibilityManager))
                }
                // Reacts live if TalkBack is toggled from Quick Settings while
                // Resonant is in the foreground, not just at cold start.
                DisposableEffect(accessibilityManager) {
                    if (accessibilityManager == null) return@DisposableEffect onDispose {}
                    val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled ->
                        touchExplorationOn = enabled
                    }
                    accessibilityManager.addTouchExplorationStateChangeListener(listener)
                    onDispose { accessibilityManager.removeTouchExplorationStateChangeListener(listener) }
                }
                var bypassWarning by remember { mutableStateOf(false) }

                CompositionLocalProvider(
                    LocalAudioManager provides container.audioManager,
                    LocalHapticManager provides container.hapticManager,
                    LocalDebugState provides container.debugState
                ) {
                    if (touchExplorationOn && !bypassWarning) {
                        TalkBackNoticeScreen(
                            onOpenAccessibilitySettings = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            onRecheck = {
                                touchExplorationOn = isTouchExplorationOn(accessibilityManager)
                            },
                            onContinueAnyway = { bypassWarning = true }
                        )
                    } else {
                        Box(Modifier.fillMaxSize()) {
                            ResonantNavHost(startDestination = start)
                            TtsUnavailableBanner(container.audioManager, container.hapticManager, lifecycle)
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val container = (application as ResonantApp).container
        container.audioManager.restoreFromBackground()
        container.soundCues.inForeground = true
    }

    /**
     * Speech must not continue once the app is off-screen. onStop (not onPause)
     * so a transient overlay — permission dialog, notification shade — doesn't
     * cut the user off mid-sentence.
     */
    override fun onStop() {
        super.onStop()
        val container = (application as ResonantApp).container
        container.audioManager.releaseForBackground()
        // Chat's thinking tick keeps running off-screen; it must not keep sounding.
        container.soundCues.inForeground = false
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as ResonantApp).container.audioManager.shutdown()
    }
}
