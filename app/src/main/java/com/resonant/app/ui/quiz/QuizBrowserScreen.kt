package com.resonant.app.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.content.QuizData
import com.resonant.app.content.QuizProgressStore
import com.resonant.app.content.QuizProgressStore.QuizProgress
import com.resonant.app.content.QuizProgressStore.Status
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
import kotlinx.coroutines.launch

private val CardBackground = Color(0xFF1A1A1A)
private val ProgressBarBackground = Color(0xFF3A3A3A)
private val ProgressBarFill = Color(0xFFFFAE00)
private val TopBarBackground = Color(0xFF0A0A0A)

private val TopBarTitleStyle = TextStyle(
    fontFamily = MetropolisBlack,
    fontWeight = FontWeight.Black,
    fontSize = 18.sp
)
private val HeadlineStyle = TextStyle(
    fontFamily = MetropolisBlack,
    fontWeight = FontWeight.Black,
    fontSize = 28.sp,
    lineHeight = 34.sp
)
private val SubheadStyle = TextStyle(
    fontFamily = MetropolisBlack,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp
)
private val CardTitleStyle = TextStyle(
    fontFamily = MetropolisBlack,
    fontWeight = FontWeight.Black,
    fontSize = 18.sp
)
private val CardMetaStyle = TextStyle(
    fontFamily = MetropolisBlack,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp
)
private val CardStatusStyle = TextStyle(
    fontFamily = MetropolisBlack,
    fontWeight = FontWeight.Bold,
    fontSize = 13.sp
)
private val CardButtonStyle = TextStyle(
    fontFamily = MetropolisBlack,
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp
)

@Composable
fun QuizBrowserScreen(
    onOpenQuiz: (quizId: String) -> Unit,
    onBack: () -> Unit
) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val quizSets = QuizData.allQuizSets

    var index by remember { mutableIntStateOf(0) }
    var progressMap by remember { mutableStateOf<Map<String, QuizProgress>>(emptyMap()) }

    val completedCount = progressMap.values.count { it.status == Status.COMPLETED }

    LaunchedEffect(Unit) {
        debug.setScreen("Quizzes")
        progressMap = QuizProgressStore.loadAll(context)
        audio.setQueue(
            quizSets.mapIndexed { i, q ->
                val p = progressMap[q.id] ?: QuizProgress()
                SemanticUnit(
                    "quiz_browser_$i",
                    "${q.title}. ${q.category}. ${q.questions.size} questions. " +
                    "${p.statusLabel}. ${p.percentComplete} percent complete."
                )
            },
            startIndex = 0,
            autoAdvance = false
        )
        audio.index.collect { idx ->
            index = idx.coerceIn(0, (quizSets.size - 1).coerceAtLeast(0))
        }
    }

    fun open(i: Int) {
        val quiz = quizSets[i]
        haptics.play(HapticPattern.SELECT)
        audio.announce("Opening ${quiz.title}.")
        scope.launch {
            QuizProgressStore.markInProgress(context, quiz.id, quiz.questions.size)
            progressMap = QuizProgressStore.loadAll(context)
        }
        onOpenQuiz(quiz.id)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BrandOrange)
    ) {
        GestureSurface(onGesture = { gesture ->
            when (gesture) {
                is ResonantGesture.Swipe -> if (gesture.zone == InteractionZone.CENTER) {
                    when (gesture.direction) {
                        SwipeDirection.UP -> if (audio.next()) haptics.play(HapticPattern.NEXT)
                        SwipeDirection.DOWN -> if (audio.previous()) haptics.play(HapticPattern.PREVIOUS)
                        else -> {}
                    }
                }
                is ResonantGesture.Tap -> when (gesture.zone) {
                    InteractionZone.CENTER -> open(index)
                    InteractionZone.LEFT_EDGE -> { audio.togglePause(); haptics.play(HapticPattern.CONFIRM) }
                    InteractionZone.RIGHT_EDGE -> {}
                }
                is ResonantGesture.DoubleTap -> if (gesture.zone == InteractionZone.LEFT_EDGE) audio.repeatCurrent()
                is ResonantGesture.LongPress -> if (gesture.zone == InteractionZone.RIGHT_EDGE) {
                    haptics.play(HapticPattern.BACK)
                    audio.announce("Back to Home.")
                    onBack()
                }
                ResonantGesture.ThreeFingerTap -> audio.repeatCurrent()
                ResonantGesture.ThreeFingerHold -> {
                    val q = quizSets[index]
                    val p = progressMap[q.id] ?: QuizProgress()
                    audio.announce(
                        "Quizzes. $completedCount of ${quizSets.size} completed. " +
                        "Currently focused: ${q.title}. ${p.statusLabel}."
                    )
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
                        .background(TopBarBackground)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                haptics.play(HapticPattern.BACK)
                                audio.announce("Back to Home.")
                                onBack()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }

                    Text("Quizzes", style = TopBarTitleStyle, color = Color.White)

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        SettingsPlaceholderIcon(tint = BrandInk, modifier = Modifier.size(18.dp))
                    }
                }

                // Headline
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 12.dp)
                ) {
                    Text("Test what you know.", style = HeadlineStyle, color = BrandInk)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "$completedCount of ${quizSets.size} quizzes completed. Choose a quiz to begin or review.",
                        style = SubheadStyle,
                        color = BrandInk
                    )
                }

                // Quiz cards
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    quizSets.forEachIndexed { i, quiz ->
                        val progress = progressMap[quiz.id] ?: QuizProgress()
                        QuizCard(
                            quiz = quiz,
                            progress = progress,
                            focused = i == index,
                            onClick = { open(i) }
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun QuizCard(
    quiz: QuizSet,
    progress: QuizProgress,
    focused: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .clickable(onClick = onClick)
            .padding(20.dp)
            .semantics {
                contentDescription = "${quiz.title}. ${quiz.category}. " +
                    "${quiz.questions.size} questions. ${progress.statusLabel}. " +
                    "${progress.percentComplete} percent complete." +
                    if (focused) " Focused." else ""
            }
    ) {
        // Title row with help icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = quiz.title,
                style = CardTitleStyle,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            // Help / info icon placeholder
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3A3A3A)),
                contentAlignment = Alignment.Center
            ) {
                Text("?", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Meta: question count · category
        Text(
            text = "${quiz.questions.size} questions · ${quiz.category}",
            style = CardMetaStyle,
            color = Color(0xFFAAAAAA),
            modifier = Modifier.padding(top = 4.dp)
        )

        // Progress bar
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(ProgressBarBackground)
        ) {
            val fraction = (progress.percentComplete / 100f).coerceIn(0f, 1f)
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(ProgressBarFill)
                )
            }
        }

        // Status + button row
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = progress.statusLabel,
                    style = CardStatusStyle,
                    color = Color.White
                )
                Text(
                    text = "${progress.percentComplete}% complete",
                    style = CardMetaStyle,
                    color = Color(0xFFAAAAAA)
                )
            }

            // Action button pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color.White)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = progress.buttonLabel,
                    style = CardButtonStyle,
                    color = BrandInk
                )
                Spacer(Modifier.width(8.dp))
                Text("→", style = CardButtonStyle, color = BrandInk)
            }
        }
    }
}
