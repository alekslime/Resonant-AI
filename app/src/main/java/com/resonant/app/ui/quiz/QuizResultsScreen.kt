package com.resonant.app.ui.quiz

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resonant.app.content.SemanticUnit
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.gestures.InteractionZone
import com.resonant.app.gestures.ResonantGesture
import com.resonant.app.haptics.HapticPattern
import com.resonant.app.ui.components.GestureSurface
import com.resonant.app.ui.components.ResonantScaffold
import com.resonant.app.ui.theme.LocalResonantColors
import com.resonant.app.ui.theme.ScreenHorizontalPadding

@Composable
fun QuizResultsScreen(
    correct: Int,
    total: Int,
    // Item 2: empty when the score is perfect, or when this results screen is
    // itself showing the outcome of a retry round — either way there is
    // nothing left to offer a retry of.
    missedCount: Int,
    onRetryMissed: () -> Unit,
    onDone: () -> Unit
) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    val colors = LocalResonantColors.current
    val canRetry = missedCount > 0

    // Kept out of the spoken instructions when there's nothing to retry, so a
    // perfect score doesn't get a dangling "tap right edge" that does nothing.
    val instructions = if (canRetry) {
        "$missedCount missed. Tap the right edge to retry those, or tap center to return home."
    } else {
        "Tap to return home."
    }

    LaunchedEffect(correct, total, missedCount) {
        debug.setScreen("Quiz Results")
        haptics.play(HapticPattern.CONFIRM)
        audio.setQueue(
            listOf(SemanticUnit("results", "You scored $correct out of $total. $instructions")),
            startIndex = 0,
            autoAdvance = false
        )
    }

    ResonantScaffold(title = "Results", subtitle = "$correct of $total correct") {
        GestureSurface(onGesture = { gesture ->
            when (gesture) {
                is ResonantGesture.Tap -> when (gesture.zone) {
                    InteractionZone.CENTER -> { haptics.play(HapticPattern.CONFIRM); onDone() }
                    InteractionZone.LEFT_EDGE -> { audio.togglePause(); haptics.play(HapticPattern.CONFIRM) }
                    InteractionZone.RIGHT_EDGE -> if (canRetry) {
                        haptics.play(HapticPattern.SELECT)
                        audio.announce("Retrying $missedCount missed question${if (missedCount == 1) "" else "s"}.")
                        onRetryMissed()
                    }
                }
                is ResonantGesture.DoubleTap -> if (gesture.zone == InteractionZone.LEFT_EDGE) audio.repeatCurrent()
                is ResonantGesture.LongPress -> if (gesture.zone == InteractionZone.RIGHT_EDGE) {
                    haptics.play(HapticPattern.BACK)
                    audio.announce("Back to Home.")
                    onDone()
                }
                ResonantGesture.ThreeFingerTap -> audio.repeatCurrent()
                ResonantGesture.ThreeFingerHold -> audio.announce(
                    "Quiz Results. You scored $correct out of $total. $instructions"
                )
                ResonantGesture.HoldSpeedUp -> { if (audio.increaseSpeed()) haptics.play(HapticPattern.SPEED_UP) else haptics.play(HapticPattern.ERROR) }
                ResonantGesture.HoldSpeedDown -> { if (audio.decreaseSpeed()) haptics.play(HapticPattern.SPEED_DOWN) else haptics.play(HapticPattern.ERROR) }
                else -> {}
            }
        }) {
            Column(Modifier.fillMaxSize().padding(horizontal = ScreenHorizontalPadding, vertical = 40.dp)) {
                Text(
                    "$correct / $total",
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black),
                    color = colors.text
                )
                Text(
                    if (canRetry) "Tap center for home. Tap the right edge to retry the $missedCount missed."
                    else "Tap anywhere to return home.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.text,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
        }
    }
}
