package com.resonant.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.resonant.app.ui.theme.ResonantGradientBottom
import com.resonant.app.ui.theme.ResonantGradientTop

/**
 * The Resonant surface: a plain top-to-bottom gold-to-orange wash — matching
 * the supplied background asset exactly, colour for colour.
 *
 * Every screen sits on this, so moving between screens never changes the
 * background — only the content on top of it.
 */
@Composable
fun ResonantSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ResonantGradientTop, ResonantGradientBottom)
                )
            )
    ) {
        content()
    }
}
