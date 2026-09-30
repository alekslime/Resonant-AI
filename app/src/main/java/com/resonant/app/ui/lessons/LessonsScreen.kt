package com.resonant.app.ui.lessons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.content.LessonData
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

private val CardBackground = Color.White
private val CardButtonBackground = Color(0xFF0A0A0A)
private val TopBarBackground = Color(0xFF0A0A0A)

// Figma "Lessons" frame: 20dp margins, 20dp gaps, 28dp card radius.
private val ScreenMargin = 20.dp
private const val CardTitleMaxSp = 56f
private const val CardTitleMinSp = 26f

private val TopBarTitleStyle = TextStyle(
    fontFamily = MetropolisBlack,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp
)
private val CardTitleStyle = TextStyle(
    fontFamily = MetropolisBlack,
    fontWeight = FontWeight.SemiBold,
    fontSize = CardTitleMaxSp.sp,
    lineHeight = 72.sp
)
private val CardButtonStyle = TextStyle(
    fontFamily = MetropolisBlack,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp
)

@Composable
fun LessonsScreen(onOpenLesson: (String) -> Unit, onBack: () -> Unit) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    val lessons = LessonData.allLessons
    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        debug.setScreen("Lessons")
        audio.setQueue(
            lessons.mapIndexed { i, l -> SemanticUnit("lesson_list_$i", l.title) },
            startIndex = 0,
            autoAdvance = false
        )
        audio.index.collect { idx ->
            index = idx.coerceIn(0, (lessons.size - 1).coerceAtLeast(0))
        }
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
                    InteractionZone.CENTER -> {
                        haptics.play(HapticPattern.SELECT)
                        audio.announce("Starting ${lessons[index].title}.")
                        onOpenLesson(lessons[index].id)
                    }
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
                ResonantGesture.ThreeFingerHold -> audio.announce(
                    "Lessons list. Currently focused: ${lessons[index].title}."
                )
                ResonantGesture.HoldSpeedUp -> { if (audio.increaseSpeed()) haptics.play(HapticPattern.SPEED_UP) else haptics.play(HapticPattern.ERROR) }
                ResonantGesture.HoldSpeedDown -> { if (audio.decreaseSpeed()) haptics.play(HapticPattern.SPEED_DOWN) else haptics.play(HapticPattern.ERROR) }
                else -> {}
            }
        }) {
            Column(Modifier.fillMaxSize()) {

                // Top bar: back/title pill (60% wide) + separate settings circle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = ScreenMargin, end = ScreenMargin, top = 16.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(50))
                            .background(TopBarBackground)
                            .clickable { haptics.play(HapticPattern.BACK); audio.announce("Back to Home."); onBack() }
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ArrowIcon(pointRight = false, tint = Color.White, modifier = Modifier.size(22.dp))
                        Text(text = "Lessons", style = TopBarTitleStyle, color = Color.White)
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(TopBarBackground)
                            .clickable { /* settings shortcut — no-op for now */ },
                        contentAlignment = Alignment.Center
                    ) {
                        SettingsPlaceholderIcon(tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }

                // Lesson cards
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = ScreenMargin),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    lessons.forEachIndexed { i, lesson ->
                        LessonCard(
                            title = lesson.title,
                            focused = i == index,
                            onClick = {
                                audio.jumpTo(i)
                                haptics.play(HapticPattern.SELECT)
                                audio.announce("Starting ${lesson.title}.")
                                onOpenLesson(lesson.id)
                            }
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun LessonCard(title: String, focused: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(CardBackground)
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)
            .semantics { contentDescription = title + if (focused) ", focused" else "" }
    ) {
        FittedTitle(title)

        Spacer(Modifier.height(24.dp))

        // "Continue lesson  →" — full width, text left, arrow right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(50))
                .background(CardButtonBackground)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Continue lesson", style = CardButtonStyle, color = Color.White)
            ArrowIcon(pointRight = true, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

/**
 * Big title at the Figma size, shrunk just enough that the longest single word fits the card
 * (otherwise "Photosynthesis" would be broken mid-word). Short titles keep the full size.
 */
@Composable
private fun FittedTitle(title: String) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val maxPx = with(LocalDensity.current) { maxWidth.toPx() }
        val sizeSp = remember(title, maxPx) {
            var sp = CardTitleMaxSp
            while (sp > CardTitleMinSp) {
                val widest = title.split(" ").maxOf { w ->
                    measurer.measure(w, CardTitleStyle.copy(fontSize = sp.sp)).size.width
                }
                if (widest <= maxPx) break
                sp -= 2f
            }
            sp
        }
        Text(
            text = title,
            style = CardTitleStyle.copy(fontSize = sizeSp.sp, lineHeight = (sizeSp * 72f / CardTitleMaxSp).sp),
            color = BrandInk
        )
    }
}

@Composable
private fun ArrowIcon(pointRight: Boolean, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.1f
        val dir = if (pointRight) 1f else -1f
        val tail = Offset(if (pointRight) w * 0.08f else w * 0.92f, h / 2f)
        val tip = Offset(if (pointRight) w * 0.92f else w * 0.08f, h / 2f)
        drawLine(tint, tail, tip, strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(tint, tip, Offset(tip.x - dir * w * 0.3f, h * 0.2f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(tint, tip, Offset(tip.x - dir * w * 0.3f, h * 0.8f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}
