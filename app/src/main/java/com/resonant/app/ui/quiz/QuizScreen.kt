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
import com.resonant.app.ui.components.EdgeHints
import com.resonant.app.ui.components.GestureSurface
import com.resonant.app.ui.components.ResonantScaffold
import com.resonant.app.ui.theme.ResonantBlack
import com.resonant.app.ui.theme.ResonantCorrectGreen
import com.resonant.app.ui.theme.ResonantGray
import com.resonant.app.ui.theme.ResonantIncorrectRed
import com.resonant.app.ui.theme.ResonantOrange

/**
 * Queue layout for a question: index 0 is the prompt, indices 1..N are the
 * options. Reusing the same next()/previous() primitives as every other
 * screen is what keeps "explore the options" feeling identical to "browse
 * the home menu" or "move through a lesson".
 */
@Composable
fun QuizScreen(quiz: QuizSet, onFinished: (correct: Int, total: Int) -> Unit) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current

    var questionIndex by remember { mutableIntStateOf(0) }
    var queueIndex by remember { mutableIntStateOf(0) } // 0 = prompt, 1..N = options
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
        val chosen = selectedOption ?: return
        submitted = true
        haptics.play(HapticPattern.SUBMIT)
        audio.announce("Submitting.")
        val correct = chosen == question.correctIndex
        lastAnswerCorrect = correct
        if (correct) {
            correctCount += 1
            haptics.play(HapticPattern.CORRECT)
            audio.announce("Correct.")
        } else {
            haptics.play(HapticPattern.INCORRECT)
            audio.announce("Incorrect. ${question.explanation}")
        }
    }

    fun advance() {
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
                        SwipeDirection.UP -> if (submitted) advance() else audio.next()
                        SwipeDirection.DOWN -> if (!submitted) audio.previous()
                        SwipeDirection.RIGHT -> if (!submitted) submit()
                        SwipeDirection.LEFT -> {}
                    }
                }
                is ResonantGesture.DoubleTap -> if (gesture.zone == InteractionZone.CENTER && !submitted) {
                    if (queueIndex >= 1) {
                        val optIndex = queueIndex - 1
                        selectedOption = optIndex
                        haptics.play(HapticPattern.SELECT)
                        val letter = question.options[optIndex].letter
                        debug.setSelectedOption(letter.toString())
                        audio.announce("Option $letter selected.")
                    }
                }
                is ResonantGesture.Tap -> if (gesture.zone == InteractionZone.LEFT_EDGE) {
                    audio.togglePause(); haptics.play(HapticPattern.CONFIRM)
                }
                ResonantGesture.ThreeFingerTap -> audio.repeatCurrent()
                ResonantGesture.ThreeFingerHold -> {
                    val status = when {
                        submitted -> "You have submitted this answer. ${if (lastAnswerCorrect == true) "It was correct." else "It was incorrect."}"
                        selectedOption != null -> "You have selected option ${question.options[selectedOption!!].letter}, not yet submitted."
                        else -> "You are exploring answer options. Swipe right to submit once you've selected one."
                    }
                    audio.announce(
                        "You are in the Quiz, question ${questionIndex + 1} of ${quiz.questions.size}. $status"
                    )
                }
                ResonantGesture.HoldSpeedUp -> { audio.increaseSpeed(); haptics.play(HapticPattern.SPEED_UP) }
                ResonantGesture.HoldSpeedDown -> { audio.decreaseSpeed(); haptics.play(HapticPattern.SPEED_DOWN) }
                else -> {}
            }
        }) {
            EdgeHints()
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                Text(question.prompt.text, style = MaterialTheme.typography.headlineMedium, color = ResonantBlack)
                Column(Modifier.padding(top = 24.dp)) {
                    question.options.forEachIndexed { i, opt ->
                        val isFocused = queueIndex == i + 1
                        val isSelected = selectedOption == i
                        val color = when {
                            submitted && i == question.correctIndex -> ResonantCorrectGreen
                            submitted && isSelected -> ResonantIncorrectRed
                            isSelected -> ResonantOrange
                            else -> ResonantGray
                        }
                        Text(
                            "${opt.letter}. ${opt.text}" + if (isFocused && !submitted) "  ◂" else "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isFocused || isSelected || submitted) color else ResonantBlack,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
                if (submitted) {
                    Text(
                        if (lastAnswerCorrect == true) "Correct — swipe up to continue." else "Incorrect — swipe up to continue.",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (lastAnswerCorrect == true) ResonantCorrectGreen else ResonantIncorrectRed,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            }
        }
    }
}
