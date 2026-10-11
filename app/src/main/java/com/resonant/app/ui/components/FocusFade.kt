package com.resonant.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.resonant.app.ui.theme.ActiveMotion

/**
 * The color a focus highlight is drawing, eased over about 250 ms when focus moves, so the eye
 * can follow it. Instant with Reduce motion. Wrap the color you would have used:
 * `focusColor(if (focused) BrandInk else ResonantCard)`.
 */
@Composable
fun focusColor(target: Color): Color {
    val color by animateColorAsState(target, tween(ActiveMotion.ms(250)), label = "focus")
    return color
}
