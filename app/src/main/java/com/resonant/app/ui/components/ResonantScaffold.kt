package com.resonant.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.ui.theme.ResonantBlack
import com.resonant.app.ui.theme.ResonantCaption

private val ScreenTitleStyle = TextStyle(
    fontWeight = FontWeight.Black,
    fontSize = 24.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.4).sp
)

private val ScreenSubtitleStyle = TextStyle(
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    lineHeight = 20.sp
)

/**
 * Every non-home screen. Same background image as the menu, so a screen
 * change reads as the content moving rather than the app repainting.
 *
 * Header is deliberately minimal — just a wordmark-weight title and the
 * three-dot mark, no boxed rule bar — matching the reference look (soft
 * background wash, plain header, generous whitespace) rather than the
 * earlier boxed/bar-based header treatment.
 */
@Composable
fun ResonantScaffold(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    ResonantSurface {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 28.dp, top = 48.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = title,
                        style = ScreenTitleStyle,
                        color = ResonantBlack,
                        modifier = Modifier.semantics { heading() }
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = ScreenSubtitleStyle,
                            color = ResonantCaption
                        )
                    }
                }
                DotMenuMark()
            }

            Box(Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

/** The three-stacked-dots mark from the reference design — visual only for
 *  now, not wired to a tap action (the app's real navigation is gesture-
 *  driven; this exists to match the reference's header language). */
@Composable
private fun DotMenuMark() {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 4.dp)
    ) {
        repeat(3) {
            Box(
                Modifier
                    .size(5.dp)
                    .background(ResonantBlack, CircleShape)
            )
        }
    }
}

val ScreenPadding = PaddingValues(24.dp)
