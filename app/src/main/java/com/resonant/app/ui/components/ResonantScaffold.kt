package com.resonant.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.ui.theme.ResonantBlack
import com.resonant.app.ui.theme.ResonantWhite

private val ScreenTitleStyle = TextStyle(
    fontWeight = FontWeight.Black,
    fontSize = 32.sp,
    lineHeight = 38.sp,
    letterSpacing = (-0.8).sp
)

private val ScreenSubtitleStyle = TextStyle(
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    lineHeight = 20.sp
)

/**
 * Every non-home screen. Same gradient surface and same black rule as the menu,
 * so a screen change reads as the content moving rather than the app repainting.
 * The old black header bar is gone — the title now sits directly on the wash,
 * set in the same heavy face as the menu so the type is the only hierarchy.
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
                    .height(IntrinsicSize.Min)
                    .padding(start = 52.dp, end = 24.dp, top = 56.dp, bottom = 28.dp)
            ) {
                Spacer(
                    Modifier
                        .fillMaxHeight()
                        .width(5.dp)
                        .background(ResonantBlack)
                )
                Column(Modifier.padding(start = 22.dp)) {
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
                            color = ResonantWhite
                        )
                    }
                }
            }

            Box(Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

val ScreenPadding = PaddingValues(24.dp)
