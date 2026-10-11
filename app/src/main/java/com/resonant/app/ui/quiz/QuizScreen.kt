package com.resonant.app.ui.quiz

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import com.resonant.app.ui.theme.ActiveMotion
import com.resonant.app.ui.theme.cardShape
import com.resonant.app.ui.theme.ActiveSpacing
import com.resonant.app.ui.theme.ActivePalette
import com.resonant.app.ui.theme.cardBorder
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import com.resonant.app.ui.theme.ResonantCard
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.resonant.app.ui.components.SystemBarsColor
import com.resonant.app.ui.lessons.FittedTitle
import com.resonant.app.ui.lessons.LessonsTopBar
import com.resonant.app.ui.theme.BrandInk
import com.resonant.app.ui.theme.BrandOrange
import com.resonant.app.ui.theme.MetropolisBlack

// Figma "correct" frame: bright lime background, text stays black.

@Composable
fun QuizScreen(
    quiz: QuizSet,
    onFinished: (correct: Int, total: Int, missed: List<QuizQuestion>) -> Unit,
    onBack: () -> Unit,
    onOpenSettings: (() -> Unit)? = null
) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current

    // rememberSaveable (not remember) so the place survives opening Settings from the gear and
    // coming back: the navigation back stack saves these and hands them back.
    var questionIndex by rememberSaveable { mutableStateOf(0) }
    var queueIndex by rememberSaveable { mutableStateOf(0) }
    var selectedOption by rememberSaveable { mutableStateOf<Int?>(null) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var lastAnswerCorrect by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var correctCount by rememberSaveable { mutableStateOf(0) }
    // Indexes of missed questions, e.g. "0,3". A plain string because it saves cleanly.
    var missedIndexes by rememberSaveable { mutableStateOf("") }
    var started by rememberSaveable { mutableStateOf(false) }
    fun missedQuestions(): List<QuizQuestion> =
        missedIndexes.split(',').filter { it.isNotEmpty() }.map { quiz.questions[it.toInt()] }

    val question = quiz.questions[questionIndex]

    // Which option is currently visible (queueIndex 0 = prompt, 1..N = options)
    val visibleOptionIndex = if (queueIndex >= 1) queueIndex - 1 else null

    fun unitsFor(q: QuizQuestion): List<SemanticUnit> = listOf(q.prompt) + q.options.mapIndexed { oi, opt ->
        SemanticUnit("${q.id}_opt_$oi", "Option ${opt.letter}. ${opt.text}.")
    }

    fun loadQuestion(i: Int) {
        questionIndex = i
        selectedOption = null
        submitted = false
        lastAnswerCorrect = null
        debug.setSelectedOption("—")
        audio.setQueue(unitsFor(quiz.questions[i]), startIndex = 0, autoAdvance = false)
    }

    /** Coming back from Settings: same question, same option, same score; nothing is reset. */
    fun restoreQuestion() {
        audio.announce("Back to the quiz.")
        audio.setQueue(
            unitsFor(quiz.questions[questionIndex]),
            startIndex = if (submitted) 0 else queueIndex,
            autoAdvance = false,
            queueBehindAnnouncement = true
        )
    }

    LaunchedEffect(quiz.id) {
        debug.setScreen("Quiz")
        if (started) restoreQuestion() else { loadQuestion(0); started = true }
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
            missedIndexes = if (missedIndexes.isEmpty()) "$questionIndex" else "$missedIndexes,$questionIndex"
            haptics.play(HapticPattern.INCORRECT)
            audio.announce("Incorrect. ${question.explanation}")
        }
    }

    fun advance() {
        haptics.play(HapticPattern.NEXT)
        if (questionIndex + 1 < quiz.questions.size) {
            loadQuestion(questionIndex + 1)
        } else {
            onFinished(correctCount, quiz.questions.size, missedQuestions())
        }
    }

    // After an answer the screen takes the theme's "correct" or "incorrect" colors. They differ in
    // brightness, not just hue, and the check or cross and the word below say it too, so the
    // result never depends on telling colors apart.
    val palette = ActivePalette.current
    val targetBackground = when {
        submitted && lastAnswerCorrect == true -> palette.correct
        submitted && lastAnswerCorrect == false -> palette.incorrect
        else -> BrandOrange
    }

    val targetTextColor = when {
        submitted && lastAnswerCorrect == true -> palette.onCorrect
        submitted && lastAnswerCorrect == false -> palette.onIncorrect
        else -> BrandInk
    }

    // The result fades in over about a quarter of a second; at once with Reduce motion.
    val fade = tween<Color>(ActiveMotion.ms(250))
    val screenBackground by animateColorAsState(targetBackground, fade, label = "resultBackground")
    val questionTextColor by animateColorAsState(targetTextColor, fade, label = "resultText")

    SystemBarsColor(screenBackground)

    Box(
        Modifier
            .fillMaxSize()
            .background(screenBackground)
    ) {
        GestureSurface(
            hint = "Swipe down to hear the next answer, tap the middle to choose it, swipe right to submit, or hold three fingers to hear where you are.",
            onGesture = { gesture ->
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
                    backAnnouncement = "Leaving the quiz. Back to Quizzes.",
                    onSettings = onOpenSettings
                )

                // Question prompt (Figma: 35/48, heavy, straight under the header)
                Text(
                    text = question.prompt.text,
                    fontFamily = MetropolisBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 35.sp,
                    lineHeight = ActiveSpacing.lineHeight(48f),
                    letterSpacing = ActiveSpacing.letterSpacing,
                    color = questionTextColor,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                // The result in shape and words. It is already spoken, so TalkBack skips it.
                if (submitted) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .clearAndSetSemantics { },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ResultMark(
                            correct = lastAnswerCorrect == true,
                            tint = questionTextColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (lastAnswerCorrect == true) "Correct" else "Incorrect",
                            fontFamily = MetropolisBlack,
                            fontWeight = FontWeight.Black,
                            fontSize = 36.sp,
                            color = questionTextColor
                        )
                    }
                }

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
                        if (showUp) VerticalArrow(up = true, tint = questionTextColor.copy(alpha = 0.8f))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                            .heightIn(min = 140.dp)
                            .clip(cardShape())
                            .background(if (visibleOptionIndex != null) ResonantCard else ResonantCard.copy(alpha = 0.3f))
                            .then(if (visibleOptionIndex != null) Modifier.cardBorder(cardShape()) else Modifier)
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
                                color = questionTextColor.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    val showDown = !submitted && visibleOptionIndex != null &&
                        visibleOptionIndex < question.options.size - 1
                    Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                        if (showDown) VerticalArrow(up = false, tint = questionTextColor.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

/**
 * A check or a cross, drawn with lines so it needs no icon file and scales cleanly. It grows
 * from 80% and fades in once when the result appears; no bounce, and shown at once with Reduce
 * motion.
 */
@Composable
private fun ResultMark(correct: Boolean, tint: Color, modifier: Modifier = Modifier) {
    val appear = remember { Animatable(if (ActiveMotion.reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(ActiveMotion.ms(250), easing = FastOutSlowInEasing))
    }
    Canvas(modifier.graphicsLayer {
        val p = appear.value
        alpha = p
        scaleX = 0.8f + 0.2f * p
        scaleY = 0.8f + 0.2f * p
    }) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.14f
        if (correct) {
            drawLine(tint, Offset(w * 0.18f, h * 0.54f), Offset(w * 0.40f, h * 0.76f), stroke, StrokeCap.Round)
            drawLine(tint, Offset(w * 0.40f, h * 0.76f), Offset(w * 0.84f, h * 0.26f), stroke, StrokeCap.Round)
        } else {
            drawLine(tint, Offset(w * 0.22f, h * 0.22f), Offset(w * 0.78f, h * 0.78f), stroke, StrokeCap.Round)
            drawLine(tint, Offset(w * 0.78f, h * 0.22f), Offset(w * 0.22f, h * 0.78f), stroke, StrokeCap.Round)
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
