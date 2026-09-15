package com.resonant.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.resonant.app.ui.theme.ResonantDisc
import com.resonant.app.ui.theme.ResonantGradientBottom
import com.resonant.app.ui.theme.ResonantGradientMid
import com.resonant.app.ui.theme.ResonantGradientTop

/**
 * The Resonant surface: a diagonal gold-to-orange wash with three soft discs
 * drifting off the left edge.
 *
 * Every screen sits on this, so moving between screens never changes the
 * background — only the content on top of it. The discs are sized and placed in
 * fractions of the viewport rather than fixed dp, so the composition holds its
 * proportions on any screen size instead of drifting on tablets or short phones.
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
                Brush.linearGradient(
                    // 0.0 top-left → 1.0 bottom-right. The mid stop sits past halfway
                    // so the top third holds its gold instead of washing orange.
                    colorStops = arrayOf(
                        0.0f to ResonantGradientTop,
                        0.55f to ResonantGradientMid,
                        1.0f to ResonantGradientBottom
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
    ) {
        // Purely decorative — hidden from screen readers so TalkBack never
        // announces it ahead of the actual content.
        Canvas(
            Modifier
                .fillMaxSize()
                .clearAndSetSemantics { }
        ) {
            val w = size.width
            val h = size.height
            val radius = w * 0.29f
            val centerX = w * 0.06f
            listOf(0.24f, 0.50f, 0.76f).forEach { fraction ->
                drawCircle(
                    color = ResonantDisc.copy(alpha = 0.50f),
                    radius = radius,
                    center = Offset(centerX, h * fraction)
                )
            }
        }
        content()
    }
}
