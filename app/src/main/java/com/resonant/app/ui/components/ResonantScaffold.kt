package com.resonant.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.ui.theme.LocalResonantColors
import com.resonant.app.ui.theme.ScreenHorizontalPadding
import com.resonant.app.ui.theme.ScreenTopPadding

/**
 * Every non-home screen. Same flat gradient as the menu, so a screen change
 * reads as the content moving rather than the app repainting.
 *
 * Header uses the same type scale as the rest of the app (headlineMedium /
 * bodyMedium) rather than its own bespoke sizes, and the same solid text
 * color as everything else — no separate faded "caption" tone. Horizontal
 * margins match [ScreenHorizontalPadding] used everywhere else, so the
 * header and the body content below it line up on both edges instead of
 * the header sitting slightly narrower/off-center against its own content.
 */
@Composable
fun ResonantScaffold(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = LocalResonantColors.current
    ResonantSurface {
        Column(Modifier.fillMaxSize()) {
            if (header != null) header() else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 20.dp,
                        bottom = 16.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Black
                        ),
                        color = colors.text,
                        modifier = Modifier.semantics { heading() }
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.text,
                            modifier = Modifier.padding(top = 0.dp)
                        )
                    }
                }
                if (trailing != null) trailing() else BrailleRMark(color = colors.text)
            }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                content()
            }
            bottomBar?.invoke()
        }
    }
}

/**
 * The header mark: the app logo (braille "R", dots 1-2-3-5) as four circles, static. The
 * animated dots live only on the Chat screen. Positions/radius are fractions of this
 * composable's size, measured from the logo PNG, same as the launcher icon.
 */
@Composable
private fun BrailleRMark(color: Color) {
    Canvas(
        modifier = Modifier
            .size(width = 19.dp, height = 28.dp)
            .padding(top = 6.dp)
    ) {
        val radius = size.height * 0.150f
        listOf(
            0.221f to 0.150f, // top-left
            0.221f to 0.500f, // mid-left
            0.221f to 0.850f, // bottom-left
            0.778f to 0.500f  // mid-right
        ).forEach { (nx, ny) ->
            drawCircle(color = color, radius = radius, center = Offset(size.width * nx, size.height * ny))
        }
    }
}
