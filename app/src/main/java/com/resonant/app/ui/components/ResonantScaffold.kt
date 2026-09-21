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
import androidx.compose.ui.unit.dp
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
    content: @Composable () -> Unit
) {
    val colors = LocalResonantColors.current
    ResonantSurface {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = ScreenHorizontalPadding,
                        end = ScreenHorizontalPadding,
                        top = ScreenTopPadding,
                        bottom = 24.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.text,
                        modifier = Modifier.semantics { heading() }
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.text,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                BrailleRMark(color = colors.text)
            }

            Box(Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

/**
 * The header mark — the app's actual logo (LogoBlack_NOBG.png), the braille
 * cell for "R" (dots 1, 2, 3, 5), drawn as four circles rather than swapped
 * in as a static PNG so it tints to [color] and stays sharp at any density.
 *
 * The dot positions/radius below are fractions of this composable's own
 * width/height, not fixed dp — they were derived by measuring the source
 * PNG directly (dot centers + radii via a pixel scan, not eyeballed) and
 * normalizing against its bounding box (2000x2000px source; bbox x
 * 697-1379, y 497-1502). Same fractions drive the launcher icon
 * (ic_launcher_foreground.xml), so the header mark and the icon are the
 * same shape at every size, not two separate approximations of it.
 *
 * Matches the header's text color so it stays visible in both themes (it
 * used to be hardcoded black, which vanished against a dark background).
 */
@Composable
private fun BrailleRMark(color: Color) {
    Canvas(
        modifier = Modifier
            .size(width = 19.dp, height = 28.dp)
            .padding(top = 6.dp)
    ) {
        val radius = size.height * 0.150f
        val dots = listOf(
            0.221f to 0.150f, // dot 1: top-left
            0.221f to 0.500f, // dot 2: mid-left
            0.778f to 0.500f, // dot 3: mid-right
            0.221f to 0.850f  // dot 4: bottom-left
        )
        dots.forEach { (nx, ny) ->
            drawCircle(
                color = color,
                radius = radius,
                center = Offset(size.width * nx, size.height * ny)
            )
        }
    }
}
