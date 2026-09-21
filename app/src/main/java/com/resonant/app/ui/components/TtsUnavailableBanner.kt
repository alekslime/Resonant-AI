package com.resonant.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.resonant.app.audio.AudioManager
import com.resonant.app.haptics.HapticManager
import com.resonant.app.haptics.HapticPattern
import kotlinx.coroutines.delay

/**
 * This app is built around speech being available — every screen assumes TTS
 * works and fails silently (a no-op speak call) if it doesn't. That's fine
 * for a sighted developer who can see the "Thinking…" text that never gets
 * spoken, but for the actual target user it looks like the app has frozen.
 *
 * This surfaces that one failure mode: if the engine hasn't become ready a
 * few seconds after launch, something is genuinely wrong (no TTS engine
 * installed/enabled on the device) rather than still warming up, so we show
 * it and buzz once, instead of leaving the person guessing why nothing talks.
 *
 * The engine is released on purpose whenever the app is backgrounded, so "not ready"
 * while stopped is expected, not a fault. The grace period therefore only runs while
 * [lifecycle] is STARTED; otherwise the error buzz would fire from the background.
 */
@Composable
fun TtsUnavailableBanner(audio: AudioManager, haptics: HapticManager, lifecycle: Lifecycle) {
    val isReady by audio.isReady.collectAsState()
    var showBanner by remember { mutableStateOf(false) }

    LaunchedEffect(isReady) {
        if (isReady) {
            showBanner = false
            return@LaunchedEffect
        }
        // repeatOnLifecycle cancels the wait when the app stops and starts it over on
        // return, and reading the flow fresh means an engine that came back in the
        // meantime is not reported as missing. The buzz plays once, when the banner first
        // appears, not on every return to the foreground.
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            delay(4000) // grace period — normal init takes well under a second
            if (!audio.isReady.value && !showBanner) {
                showBanner = true
                haptics.play(HapticPattern.ERROR)
            }
        }
    }

    AnimatedVisibility(visible = showBanner, enter = fadeIn(), exit = fadeOut()) {
        Text(
            "Voice is unavailable. Check that a text-to-speech engine is installed and enabled in your device settings.",
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2A0000))
                .padding(16.dp),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
