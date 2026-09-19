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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                DotMenuMark(color = colors.text)
            }

            Box(Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

/** The three-stacked-dots mark — visual only, matches the header's text
 *  color so it stays visible in both light and dark theme (it used to be
 *  hardcoded black, which vanished against a dark background). Sized up
 *  slightly to match the bigger type scale around it. */
@Composable
private fun DotMenuMark(color: Color) {
    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        repeat(3) {
            Box(
                Modifier
                    .size(7.dp)
                    .background(color, CircleShape)
            )
        }
    }
}

val ScreenPadding = PaddingValues(horizontal = ScreenHorizontalPadding, vertical = ScreenTopPadding)
