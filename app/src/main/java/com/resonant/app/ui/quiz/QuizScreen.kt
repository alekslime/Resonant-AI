package com.resonant.app.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.content.QuizQuestion
import com.resonant.app.content.QuizSet
import com.resonant.app.content.SemanticUnit
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.gestures.InteractionZone
import com.resonant.app.gestures.ResonantGesture
import com.resonant.app.gestures.SwipeDirection
import com.resonant.app.haptics.HapticPattern
import com.resonant.app.ui.components.GestureSurface
import com.resonant.app.ui.components.ResonantScaffold
import com.resonant.app.ui.theme.LocalResonantColors
import com.resonant.app.ui.theme.MetropolisBlack
import com.resonant.app.ui.theme.ScreenHorizontalPadding

@Composable
fun QuizScreen(
    quiz: QuizSet,
    // Item 2: the results screen offers a retry of just the ones missed, so
    // finishing has to report WHICH questions those were, not just the count.
    onFinished: (correct: Int, total: Int, missed: List<QuizQuestion>) -> Unit,
    onBack: () -> Unit
) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    val colors = LocalResonantColors.current

    var questionIndex by remember { mutableIntStateOf(0) }
    var queueIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var submitted by remember { mutableStateOf(false) }
    var lastAnswerCorrect by remember { mutableStateOf<Boolean?>(null) }
    var correctCount by remember { mutableIntStateOf(0) }
    // Keyed on quiz.id so a fresh quiz (e.g. starting a retry-missed round)
    // never inherits misses from a previous round still sitting in memory.
    val missed = remember(quiz.id) { mutableListOf<QuizQuestion>() }

    val question = quiz.questions[questionIndex]

    fun loadQuestion(i: Int) {
        questionIndex = i
        selectedOption = null
        submitted = false
        lastAnswerCorrect = null
        debug.setSelectedOption("—")
        val q = quiz.questions[i]
        val units = listOf(q.prompt) + q.options.mapIndexed { oi, opt ->
            SemanticUnit("${q.id}_opt_$oi", "Option ${opt.letter}. ${opt.text}.")
        }
        audio.setQueue(units, startIndex = 0, autoAdvance = false)
    }

    // Single effect keyed on quiz.id: loadQuestion(0) must land before collection
    // starts, and if quiz.id ever changes, both setup and the collector restart
    // together instead of the collector being left subscribed under a stale key.
    LaunchedEffect(quiz.id) {
        debug.setScreen("Quiz")
        loadQuestion(0)
        audio.index.collect { idx ->
            queueIndex = idx
            if (idx >= 1) haptics.playOption(idx - 1)
        }
    }

    fun submit() {
        val chosen = selectedOption ?: run {
            audio.announce("No option selected. Swipe up or down to browse, then tap to select.")
            haptics.play(HapticPattern.ERROR)
            return
        }
        submitted = true
        val correct = chosen == question.correctIndex
        lastAnswerCorrect = correct
        if (correct) {
            correctCount += 1
            haptics.play(HapticPattern.CORRECT)
            audio.announce("Correct. ${question.explanation}")
        } else {
            missed.add(question)
            haptics.play(HapticPattern.INCORRECT)
            audio.announce("Incorrect. ${question.explanation}")
        }
    }

    fun advance() {
        haptics.play(HapticPattern.NEXT)
        if (questionIndex + 1 < quiz.questions.size) {
            loadQuestion(questionIndex + 1)
        } else {
            onFinished(correctCount, quiz.questions.size, missed.toList())
        }
    }

    ResonantScaffold(
        title = quiz.title,
        subtitle = "Question ${questionIndex + 1} of ${quiz.questions.size}"
    ) {
        GestureSurface(onGesture = { gesture ->
            when (gesture) {
                is ResonantGesture.Swipe -> if (gesture.zone == InteractionZone.CENTER) {
                    when (gesture.direction) {
                        // While browsing, the option's own tactile identifier (played by the
                        // index collector above) IS the feedback for a successful move —
                        // playing NEXT/PREVIOUS on top of it just replaced one buzz with
                        // another. Only the edges need an explicit cue here.
                        SwipeDirection.UP -> if (submitted) advance() else if (!audio.next()) haptics.play(HapticPattern.EDGE)
                        SwipeDirection.DOWN -> if (!submitted) {
                            if (!audio.previous()) {
                                haptics.play(HapticPattern.EDGE)
                            } else if (audio.index.value == 0) {
                                // Back on the question itself — no option identifier for that.
                                haptics.play(HapticPattern.PREVIOUS)
                            }
                        }
                        SwipeDirection.RIGHT -> if (!submitted) submit()
                        SwipeDirection.LEFT -> {}
                    }
                }
                is ResonantGesture.Tap -> when (gesture.zone) {
                    InteractionZone.CENTER -> if (!submitted && queueIndex >= 1) {
                        val optIndex = queueIndex - 1
                        selectedOption = optIndex
                        haptics.play(HapticPattern.SELECT)
                        val letter = question.options[optIndex].letter
                        debug.setSelectedOption(letter.toString())
                        audio.announce("Option $letter selected. Swipe right to submit.")
                    }
                    InteractionZone.LEFT_EDGE -> { audio.togglePause(); haptics.play(HapticPattern.CONFIRM) }
                    InteractionZone.RIGHT_EDGE -> {}
                }
                is ResonantGesture.DoubleTap -> if (gesture.zone == InteractionZone.LEFT_EDGE) audio.repeatCurrent()
                is ResonantGesture.LongPress -> if (gesture.zone == InteractionZone.RIGHT_EDGE) {
                    haptics.play(HapticPattern.BACK)
                    audio.announce("Leaving the quiz. Back to Lessons.")
                    onBack()
                }
                ResonantGesture.ThreeFingerTap -> audio.repeatCurrent()
                ResonantGesture.ThreeFingerHold -> {
                    val status = when {
                        submitted -> "Submitted. ${if (lastAnswerCorrect == true) "Correct." else "Incorrect."} Swipe down to continue."
                        selectedOption != null -> "Option ${question.options[selectedOption!!].letter} selected. Swipe right to submit."
                        else -> "No option selected. Swipe up or down to browse, tap to select, swipe right to submit."
                    }
                    audio.announce("Quiz, question ${questionIndex + 1} of ${quiz.questions.size}. $status")
                }
                ResonantGesture.HoldSpeedUp -> { if (audio.increaseSpeed()) haptics.play(HapticPattern.SPEED_UP) else haptics.play(HapticPattern.ERROR) }
                ResonantGesture.HoldSpeedDown -> { if (audio.decreaseSpeed()) haptics.play(HapticPattern.SPEED_DOWN) else haptics.play(HapticPattern.ERROR) }
                else -> {}
            }
        }) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenHorizontalPadding, vertical = 24.dp)
            ) {
                // Question prompt
                Text(
                    question.prompt.text,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = colors.text
                )

                // Options
                Column(Modifier.padding(top = 24.dp)) {
                    question.options.forEachIndexed { i, opt ->
                        val isFocused = queueIndex == i + 1
                        val isSelected = selectedOption == i
                        val isCorrect = i == question.correctIndex

                        val cardBackground = when {
                            submitted && isCorrect -> colors.correctFill
                            submitted && isSelected && !isCorrect -> colors.incorrectFill
                            isSelected -> Color(0xFF2A2A2A)
                            else -> Color(0xFF1A1A1A)
                        }
                        val textColor = when {
                            submitted && (isCorrect || isSelected) -> colors.feedbackText
                            else -> Color.White
                        }
                        val borderMod = if (isFocused && !submitted)
                            Modifier.border(2.dp, Color(0xFFFFAE00), RoundedCornerShape(16.dp))
                        else Modifier

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(cardBackground)
                                .then(borderMod)
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Letter badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2E2E2E))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = opt.letter.toString(),
                                    color = Color.White,
                                    fontFamily = MetropolisBlack,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Text(
                                text = opt.text,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = textColor
                            )
                        }
                    }
                }

                // Feedback banner
                if (submitted) {
                    val feedbackFill = if (lastAnswerCorrect == true) colors.correctFill else colors.incorrectFill
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(feedbackFill)
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Text(
                            if (lastAnswerCorrect == true) "Correct — swipe down to continue."
                            else "Incorrect — swipe down to continue.",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = colors.feedbackText
                        )
                    }
                }

                Spacer(Modifier.padding(bottom = 24.dp))
            }
        }
    }
}