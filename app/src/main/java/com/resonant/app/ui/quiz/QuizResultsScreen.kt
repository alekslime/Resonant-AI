package com.resonant.app.ui.quiz

import com.resonant.app.ui.theme.CappedTextScale
import com.resonant.app.ui.theme.cardShape
import com.resonant.app.ui.theme.cardBorder
import com.resonant.app.ui.theme.ResonantCard
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.resonant.app.ui.components.ResonantButton
import com.resonant.app.ui.components.ResonantScaffold
import com.resonant.app.ui.components.enterStagger
import com.resonant.app.ui.theme.BrandInk
import com.resonant.app.ui.theme.MetropolisBlack

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

    val percent = if (total > 0) correct * 100 / total else 0

    // A perfect score gets a second, brighter tap just after the first. Nothing else changes, and
    // the spoken result is never delayed.
    LaunchedEffect(correct, total) {
        if (total > 0 && percent == 100) {
            delay(650)
            haptics.play(HapticPattern.CORRECT)
        }
    }
    val resultLabel = when {
        percent == 100 -> "Perfect score!"
        percent >= 80 -> "Great work."
        percent >= 60 -> "Good effort."
        else -> "Keep practicing."
    }
    fun retry() {
        haptics.play(HapticPattern.SELECT)
        audio.announce("Retrying $missedCount missed question${if (missedCount == 1) "" else "s"}.")
        onRetryMissed()
    }

    ResonantScaffold(
        title = "Results",
        subtitle = "$correct of $total correct",
        onBack = onDone,
        backAnnouncement = "Back to quizzes.",
        // Real buttons for sighted users. They sit outside the GestureSurface on purpose (the
        // gesture detector consumes touches); the tap gestures below still do the same things.
        bottomBar = {
            Column(
                Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (canRetry) {
                    ResonantButton("Retry $missedCount missed", filled = true, onClick = { retry() })
                }
                ResonantButton("Back to quizzes", filled = !canRetry, onClick = {
                    haptics.play(HapticPattern.CONFIRM)
                    onDone()
                })
            }
        }
    ) {
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
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // The score sits in one of the white cards used on every other screen.
                Column(
                    Modifier
                        .enterStagger(0)
                        .fillMaxWidth()
                        .clip(cardShape())
                        .background(ResonantCard)
                        .cardBorder(cardShape())
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // The big score keeps its size: at a larger text size it would no longer fit the card.
                    CappedTextScale(1f) {
                        Text(
                            text = "$correct / $total",
                            style = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.Black, fontSize = 72.sp, lineHeight = 78.sp),
                            color = BrandInk,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.clearAndSetSemantics { contentDescription = "$correct / $total" }
                        )
                    }
                    Text(
                        text = resultLabel,
                        fontFamily = MetropolisBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        color = BrandInk,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Text(
                    text = if (canRetry)
                        "Tap the right edge to retry the $missedCount missed, or the center to go back."
                    else
                        "Tap anywhere to go back.",
                    style = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
                    color = BrandInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.enterStagger(2).padding(top = 24.dp)
                )
            }
        }
    }
}
