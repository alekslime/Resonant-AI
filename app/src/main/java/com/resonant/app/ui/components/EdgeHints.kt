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
import com.resonant.app.ui.theme.ResonantWhite

/**
 * The left/right edge strips are functionally invisible (they're just a zone
 * of the same gesture surface, not a separate hit target) — but a faint visual
 * hint helps sighted users discover them without turning the screen into a
 * "generic dashboard with visible buttons everywhere". Deliberately subtle.
 */
@Composable
fun BoxScope.EdgeHints() {
    Box(
        Modifier
            .align(Alignment.CenterStart)
            .fillMaxHeight()
            .width(3.dp)
            .alpha(0.28f)
            .background(ResonantWhite)
    )
    Box(
        Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .width(3.dp)
            .alpha(0.28f)
            .background(ResonantWhite)
    )
}
