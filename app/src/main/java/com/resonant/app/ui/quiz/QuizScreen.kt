package com.resonant.app.ui.quiz

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.resonant.app.ui.theme.ResonantBlack
import com.resonant.app.ui.theme.ResonantCorrectGreen
import com.resonant.app.ui.theme.ResonantIncorrectRed

@Composable
fun QuizScreen(quiz: QuizSet, onFinished: (correct: Int, total: Int) -> Unit) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current

    var questionIndex by remember { mutableIntStateOf(0) }
    var queueIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var submitted by remember { mutableStateOf(false) }
    var lastAnswerCorrect by remember { mutableStateOf<Boolean?>(null) }
    var correctCount by remember { mutableIntStateOf(0) }

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

    LaunchedEffect(quiz.id) {
        debug.setScreen("Quiz")
        loadQuestion(0)
    }

    LaunchedEffect(Unit) {
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
            haptics.play(HapticPattern.INCORRECT)
            audio.announce("Incorrect. ${question.explanation}")
        }
    }

    fun advance() {
        haptics.play(HapticPattern.NEXT)
        if (questionIndex + 1 < quiz.questions.size) {
            loadQuestion(questionIndex + 1)
        } else {
            onFinished(correctCount, quiz.questions.size)
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
                        SwipeDirection.UP -> if (submitted) advance() else { audio.next(); haptics.play(HapticPattern.NEXT) }
                        SwipeDirection.DOWN -> if (!submitted) { audio.previous(); haptics.play(HapticPattern.PREVIOUS) }
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
                }
                ResonantGesture.ThreeFingerTap -> audio.repeatCurrent()
                ResonantGesture.ThreeFingerHold -> {
                    val status = when {
                        submitted -> "Submitted. ${if (lastAnswerCorrect == true) "Correct." else "Incorrect."} Swipe up to continue."
                        selectedOption != null -> "Option ${question.options[selectedOption!!].letter} selected. Swipe right to submit."
                        else -> "No option selected. Swipe up or down to browse, tap to select, swipe right to submit."
                    }
                    audio.announce("Quiz, question ${questionIndex + 1} of ${quiz.questions.size}. $status")
                }
                ResonantGesture.HoldSpeedUp -> { audio.increaseSpeed(); haptics.play(HapticPattern.SPEED_UP) }
                ResonantGesture.HoldSpeedDown -> { audio.decreaseSpeed(); haptics.play(HapticPattern.SPEED_DOWN) }
                else -> {}
            }
        }) {
            Column(Modifier.fillMaxSize().padding(start = 32.dp, end = 24.dp, top = 40.dp, bottom = 24.dp)) {
                Text(
                    question.prompt.text,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = ResonantBlack
                )
                Column(Modifier.padding(top = 32.dp)) {
                    question.options.forEachIndexed { i, opt ->
                        val isFocused = queueIndex == i + 1
                        val isSelected = selectedOption == i
                        val color = when {
                            submitted && i == question.correctIndex -> ResonantCorrectGreen
                            submitted && isSelected -> ResonantIncorrectRed
                            isSelected || isFocused -> ResonantBlack
                            else -> ResonantBlack.copy(alpha = 0.3f)
                        }
                        Text(
                            "${opt.letter}. ${opt.text}" + if (isFocused && !submitted) "  ◂" else "",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = color,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
                if (submitted) {
                    Text(
                        if (lastAnswerCorrect == true) "Correct — swipe up to continue."
                        else "Incorrect — swipe up to continue.",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = if (lastAnswerCorrect == true) ResonantCorrectGreen else ResonantIncorrectRed,
                        modifier = Modifier.padding(top = 32.dp)
                    )
                }
            }
        }
    }
}
