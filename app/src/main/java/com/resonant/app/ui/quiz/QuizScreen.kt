package com.resonant.app.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
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
import com.resonant.app.ui.home.SettingsPlaceholderIcon
import com.resonant.app.ui.theme.BrandInk
import com.resonant.app.ui.theme.BrandOrange
import com.resonant.app.ui.theme.MetropolisBlack

private val CorrectGreen = Color(0xFF4CAF50)
private val WrongRed = Color(0xFFE53935)

@Composable
fun QuizScreen(
    quiz: QuizSet,
    onFinished: (correct: Int, total: Int, missed: List<QuizQuestion>) -> Unit,
    onBack: () -> Unit
) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current

    var questionIndex by remember { mutableIntStateOf(0) }
    var queueIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var submitted by remember { mutableStateOf(false) }
    var lastAnswerCorrect by remember { mutableStateOf<Boolean?>(null) }
    var correctCount by remember { mutableIntStateOf(0) }
    val missed = remember(quiz.id) { mutableListOf<QuizQuestion>() }

    val question = quiz.questions[questionIndex]

    // Which option is currently visible (queueIndex 0 = prompt, 1..N = options)
    val visibleOptionIndex = if (queueIndex >= 1) queueIndex - 1 else null

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

    // Background: green on correct, red on wrong, orange otherwise
    val screenBackground = when {
        submitted && lastAnswerCorrect == true -> CorrectGreen
        submitted && lastAnswerCorrect == false -> WrongRed
        else -> BrandOrange
    }

    // Question text color: orange on correct (over green bg), white on wrong, black otherwise
    val questionTextColor = when {
        submitted && lastAnswerCorrect == true -> BrandOrange
        submitted && lastAnswerCorrect == false -> Color.White
        else -> BrandInk
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(screenBackground)
    ) {
        GestureSurface(onGesture = { gesture ->
            when (gesture) {
                is ResonantGesture.Swipe -> if (gesture.zone == InteractionZone.CENTER) {
                    when (gesture.direction) {
                        SwipeDirection.UP -> if (submitted) advance() else if (!audio.next()) haptics.play(HapticPattern.EDGE)
                        SwipeDirection.DOWN -> if (!submitted) {
                            if (!audio.previous()) haptics.play(HapticPattern.EDGE)
                            else if (audio.index.value == 0) haptics.play(HapticPattern.PREVIOUS)
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
                    audio.announce("Leaving the quiz. Back to Quizzes.")
                    onBack()
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
                ResonantGesture.HoldSpeedUp -> { if (audio.increaseSpeed()) haptics.play(HapticPattern.SPEED_UP) else haptics.play(HapticPattern.ERROR) }
                ResonantGesture.HoldSpeedDown -> { if (audio.decreaseSpeed()) haptics.play(HapticPattern.SPEED_DOWN) else haptics.play(HapticPattern.ERROR) }
                else -> {}
            }
        }) {
            Column(Modifier.fillMaxSize()) {

                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color(0xFF0A0A0A))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Quizzes",
                        fontFamily = MetropolisBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        SettingsPlaceholderIcon(tint = BrandInk, modifier = Modifier.size(18.dp))
                    }
                }

                // Question prompt
                Text(
                    text = question.prompt.text,
                    fontFamily = MetropolisBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    color = questionTextColor,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )

                // Option card area — fills remaining space
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Up arrow — shows when not on first option
                    val showUp = !submitted && queueIndex > 1
                    if (showUp) {
                        Text(
                            text = "↑",
                            fontFamily = MetropolisBlack,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            color = BrandInk.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    } else {
                        Spacer(Modifier.height(52.dp))
                    }

                    // Option card — shows when focused on an option, hidden on prompt
                    if (visibleOptionIndex != null) {
                        val opt = question.options[visibleOptionIndex]
                        val isSelected = selectedOption == visibleOptionIndex

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White)
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = opt.text,
                                fontFamily = MetropolisBlack,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                fontSize = 22.sp,
                                lineHeight = 30.sp,
                                color = BrandInk,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // On prompt — show placeholder card with hint
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White.copy(alpha = 0.3f))
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Swipe up to see options",
                                fontFamily = MetropolisBlack,
                                fontWeight = FontWeight.Normal,
                                fontSize = 16.sp,
                                color = BrandInk.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Down arrow — shows when not submitted and more options below
                    val showDown = !submitted && visibleOptionIndex != null &&
                        visibleOptionIndex < question.options.size - 1
                    if (showDown) {
                        Text(
                            text = "↓",
                            fontFamily = MetropolisBlack,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            color = BrandInk.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    } else {
                        Spacer(Modifier.height(52.dp))
                    }
                }
            }
        }
    }
}
