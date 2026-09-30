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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.R
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
import com.resonant.app.ui.lessons.FittedTitle
import com.resonant.app.ui.lessons.LessonsTopBar
import com.resonant.app.ui.theme.BrandInk
import com.resonant.app.ui.theme.BrandOrange
import com.resonant.app.ui.theme.MetropolisBlack

// Figma "correct" frame: bright lime background, text stays black.
private val CorrectGreen = Color(0xFF80FF00)
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

    // Question text: black on orange and on the lime "correct" screen (Figma); white on red.
    val questionTextColor = when {
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
                LessonsTopBar(
                    onBack = onBack,
                    title = "Quizzes",
                    backAnnouncement = "Leaving the quiz. Back to Quizzes."
                )

                // Question prompt (Figma: 35/48, heavy, straight under the header)
                Text(
                    text = question.prompt.text,
                    fontFamily = MetropolisBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 35.sp,
                    lineHeight = 48.sp,
                    color = questionTextColor,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                // Arrow / option card / arrow — card takes whatever height is left
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val showUp = !submitted && queueIndex > 1
                    Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                        if (showUp) VerticalArrow(up = true, tint = BrandInk.copy(alpha = 0.8f))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                            .heightIn(min = 140.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(if (visibleOptionIndex != null) Color.White else Color.White.copy(alpha = 0.3f))
                            .padding(horizontal = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (visibleOptionIndex != null) {
                            FittedTitle(
                                title = question.options[visibleOptionIndex].text,
                                maxSp = 36f,
                                lineRatio = 41f / 36f,
                                minSp = 20f,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "Swipe up to see options",
                                fontFamily = MetropolisBlack,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 24.sp,
                                color = BrandInk.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    val showDown = !submitted && visibleOptionIndex != null &&
                        visibleOptionIndex < question.options.size - 1
                    Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                        if (showDown) VerticalArrow(up = false, tint = BrandInk.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

/**
 * Big arrow from the Figma quiz frame (res/drawable/ic_arrow_up.xml / ic_arrow_down.xml). The
 * artwork sits inside a padded 117 box, so it is drawn larger than the 76dp slot it fills and
 * overflows it invisibly, which keeps the layout unchanged.
 */
@Composable
private fun VerticalArrow(up: Boolean, tint: Color) {
    Icon(
        painterResource(if (up) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.requiredSize(114.dp)
    )
}
