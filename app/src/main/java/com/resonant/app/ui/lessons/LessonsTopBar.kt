package com.resonant.app.ui.lessons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.haptics.HapticPattern
import com.resonant.app.ui.home.SettingsPlaceholderIcon
import com.resonant.app.ui.theme.BrandInk
import com.resonant.app.ui.theme.MetropolisBlack

/** Shared by the Lessons list, the single-lesson screen and the Quizzes list (same Figma header). */
@Composable
internal fun LessonsTopBar(
    onBack: () -> Unit,
    title: String = "Lessons",
    backAnnouncement: String = "Back to Home.",
    onSettings: (() -> Unit)? = null
) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val bar = Color(0xFF0A0A0A)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(52.dp)
                .clip(RoundedCornerShape(50))
                .background(bar)
                .clickable { haptics.play(HapticPattern.BACK); audio.announce(backAnnouncement); onBack() }
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ArrowIcon(pointRight = false, tint = Color.White, modifier = Modifier.size(22.dp))
            Text(
                text = title,
                style = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
                color = Color.White
            )
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(bar)
                .then(if (onSettings != null) Modifier.clickable { onSettings() } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            SettingsPlaceholderIcon(tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

/**
 * Big title at the Figma size, shrunk just enough that the longest single word fits the width
 * (otherwise "Photosynthesis" would be broken mid-word). Short titles keep the full size.
 */
@Composable
internal fun FittedTitle(
    title: String,
    maxSp: Float = 56f,
    lineRatio: Float = 72f / 56f,
    minSp: Float = 26f,
    textAlign: TextAlign = TextAlign.Start
) {
    val measurer = rememberTextMeasurer()
    val base = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.SemiBold)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val maxPx = with(LocalDensity.current) { maxWidth.toPx() }
        val sizeSp = remember(title, maxPx, maxSp) {
            var sp = maxSp
            while (sp > minSp) {
                val widest = title.split(" ").maxOf { w ->
                    measurer.measure(w, base.copy(fontSize = sp.sp)).size.width
                }
                if (widest <= maxPx) break
                sp -= 2f
            }
            sp
        }
        Text(
            text = title,
            style = base.copy(fontSize = sizeSp.sp, lineHeight = (sizeSp * lineRatio).sp),
            color = BrandInk,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun ArrowIcon(pointRight: Boolean, tint: Color, modifier: Modifier = Modifier) {
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
