package com.resonant.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import com.resonant.app.ui.lessons.LessonsTopBar
import com.resonant.app.ui.theme.BrandInk
import com.resonant.app.ui.theme.MetropolisBlack
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.ui.theme.LocalResonantColors
import com.resonant.app.ui.theme.ScreenHorizontalPadding

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
    onBack: (() -> Unit)? = null,
    backAnnouncement: String = "Back.",
    header: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = LocalResonantColors.current
    ResonantSurface {
        Column(Modifier.fillMaxSize()) {
            if (header != null) header() else if (onBack != null) {
                // Same back pill as Lessons and Quizzes. No settings circle: these screens are
                // Settings or sit inside it, so a gear here would only lead back to itself.
                LessonsTopBar(
                    onBack = onBack,
                    title = title,
                    backAnnouncement = backAnnouncement,
                    showSettings = false
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
                        color = BrandInk,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                    )
                }
            } else {
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
                trailing?.invoke()
            }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                content()
            }
            bottomBar?.invoke()
        }
    }
}
