package com.resonant.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.resonant.app.ui.theme.LocalResonantColors

/**
 * The left/right edge strips are functionally invisible (they're just a zone
 * of the same gesture surface, not a separate hit target) — but a faint
 * visual hint helps sighted users discover them without turning the screen
 * into a "generic dashboard with visible buttons everywhere". Deliberately
 * subtle, and uses the theme's text color (not a hardcoded black) so it
 * stays visible in both light and dark mode instead of vanishing into a
 * black-on-black dark background.
 */
@Composable
fun BoxScope.EdgeHints() {
    val color = LocalResonantColors.current.text
    Box(
        Modifier
            .align(Alignment.CenterStart)
            .fillMaxHeight()
            .width(3.dp)
            .alpha(0.28f)
            .background(color)
    )
    Box(
        Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .width(3.dp)
            .alpha(0.28f)
            .background(color)
    )
}
