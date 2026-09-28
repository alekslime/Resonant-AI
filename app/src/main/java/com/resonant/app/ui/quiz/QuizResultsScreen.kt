package com.resonant.app.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.content.SemanticUnit
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.gestures.InteractionZone
import com.resonant.app.gestures.ResonantGesture
import com.resonant.app.haptics.HapticPattern
import com.resonant.app.ui.components.GestureSurface
import com.resonant.app.ui.components.ResonantScaffold
import com.resonant.app.ui.theme.BrandInk
import com.resonant.app.ui.theme.MetropolisBlack
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
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = ScreenHorizontalPadding, vertical = 16.dp)
            ) {
                // Score card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1A1A1A))
                        .padding(32.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        // Score
                        Text(
                            text = "$correct / $total",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Black
                            ),
                            color = Color.White
                        )

                        Spacer(Modifier.height(12.dp))

                        // Result label
                        val percent = if (total > 0) correct * 100 / total else 0
                        val resultLabel = when {
                            percent == 100 -> "Perfect score!"
                            percent >= 80 -> "Great work."
                            percent >= 60 -> "Good effort."
                            else -> "Keep practicing."
                        }
                        Text(
                            text = resultLabel,
                            fontFamily = MetropolisBlack,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = Color(0xFFFFAE00)
                        )

                        Spacer(Modifier.height(32.dp))

                        // Instructions
                        Text(
                            text = if (canRetry)
                                "Tap center to go back.\nTap right edge to retry the $missedCount missed."
                            else
                                "Tap anywhere to go back.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFAAAAAA)
                        )
                    }
                }

                // Bottom action pill
                if (canRetry) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(BrandInk)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Retry $missedCount missed →",
                            fontFamily = MetropolisBlack,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color(0xFFFFAE00)
                        )
                    }
                }
            }
        }
    }
}
