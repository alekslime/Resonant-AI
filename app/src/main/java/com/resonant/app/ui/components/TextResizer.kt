package com.resonant.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.resonant.app.ResonantApp
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.haptics.HapticPattern
import com.resonant.app.ui.theme.ActiveTextScale
import com.resonant.app.ui.theme.SizeCue
import com.resonant.app.ui.theme.TextScale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * What a pinch does on a screen that does not handle it itself: resize the text of the whole app, save the size,
 * tick at each quarter step, bump at the limits, and say the size once the fingers have been still for a moment.
 */
@Composable
fun rememberTextResizer(): (Float) -> Unit {
    val context = LocalContext.current
    val prefs = remember(context) { (context.applicationContext as ResonantApp).container.prefs }
    val haptics = LocalHapticManager.current
    val audio = LocalAudioManager.current
    val scope = rememberCoroutineScope()
    return remember(prefs, haptics, audio, scope) {
        var settle: Job? = null
        val resize: (Float) -> Unit = { factor ->
            val outcome = TextScale.pinch(ActiveTextScale.current, factor)
            if (outcome.scale != ActiveTextScale.current) {
                ActiveTextScale.current = outcome.scale
                prefs.textScale = outcome.scale
                when (outcome.cue) {
                    SizeCue.LARGER -> haptics.play(HapticPattern.SPEED_UP)
                    SizeCue.SMALLER -> haptics.play(HapticPattern.SPEED_DOWN)
                    SizeCue.EDGE -> haptics.play(HapticPattern.EDGE)
                    SizeCue.NONE -> Unit
                }
                // A pinch has no end event: say the size when the fingers have gone quiet.
                settle?.cancel()
                settle = scope.launch {
                    delay(700)
                    audio.announce("Text size ${TextScale.percent(outcome.scale)} percent.")
                }
            }
        }
        resize
    }
}
