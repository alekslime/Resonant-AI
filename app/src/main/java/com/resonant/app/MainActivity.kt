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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.resonant.app.content.MaterialImporter
import com.resonant.app.haptics.HapticPattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.ui.components.TtsUnavailableBanner
import com.resonant.app.ui.nav.ResonantNavHost
import com.resonant.app.ui.nav.Routes
import com.resonant.app.ui.splash.SplashScreen
import com.resonant.app.ui.talkback.TalkBackNoticeScreen
import com.resonant.app.ui.theme.BrandOrange
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

        // Speech is prepared under the splash but only played once it starts to fade.
        container.audioManager.holdPlayback()

        // First launch: setup, then the tutorial; after that, Home. A person who quit between the two
        // does not get the setup again.
        val start = when {
            container.prefs.onboardingComplete -> Routes.HOME
            container.prefs.setupComplete -> Routes.ONBOARDING
            else -> Routes.SETUP
        }

        importShared(intent)

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
                // The splash plays on top of whichever screen is actually ready
                // underneath (TalkBack notice or the real nav host), so its exit
                // animation uncovers live content instead of handing off to a
                // blank frame while something else loads.
                var showSplash by remember { mutableStateOf(true) }

                CompositionLocalProvider(
                    LocalAudioManager provides container.audioManager,
                    LocalHapticManager provides container.hapticManager,
                    LocalDebugState provides container.debugState
                ) {
                    // The chosen theme's screen color sits behind everything, so a fade between
                    // screens never lets the window's own orange show through.
                    Box(Modifier.fillMaxSize().drawBehind { drawRect(BrandOrange) }) {
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

                        if (showSplash) {
                            SplashScreen(
                                soundCues = container.soundCues,
                                onExitStart = { container.audioManager.releasePlayback() },
                                onFinished = { showSplash = false }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        importShared(intent)
    }

    /**
     * "Share to Resonant": saves the shared text or file, then says what happened. While the
     * live mic is open it only vibrates, so the PC agent never hears the app talk.
     */
    private fun importShared(intent: Intent) {
        if (intent.action != Intent.ACTION_SEND) return
        // Handled once: a rotation or recreate must not import the same share again.
        val shared = Intent(intent)
        intent.action = null
        val app = application as ResonantApp
        lifecycleScope.launch {
            if (!app.liveKitManager.micEnabled.value) {
                MaterialImporter.startMessage(shared.type)?.let { app.container.audioManager.announce(it) }
            }
            val result = withContext(Dispatchers.IO) { MaterialImporter.fromIntent(app, shared) } ?: return@launch
            val micOpen = app.liveKitManager.micEnabled.value
            val saved = result is MaterialImporter.Result.Saved
            app.container.hapticManager.play(
                if (saved) HapticPattern.CONFIRM else HapticPattern.ERROR,
                withSound = !micOpen
            )
            if (!micOpen) app.container.audioManager.announce(MaterialImporter.message(result))
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
        // Listening is counted in memory a second at a time; make sure none of it is lost.
        container.activity.flush()
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as ResonantApp).container.audioManager.shutdown()
    }
}
