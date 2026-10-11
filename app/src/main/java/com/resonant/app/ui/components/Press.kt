package com.resonant.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.resonant.app.ui.theme.ActiveMotion

/**
 * A button or card shrinks a little while a finger is on it and eases back on release. Purely
 * a visual reply to a touch: the haptic, sound and speech are still what tell you things.
 * Off when Reduce motion is on. Put it before the clip and background so the whole thing scales.
 */
fun Modifier.pressScale(source: InteractionSource): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !ActiveMotion.reduce) 0.97f else 1f,
        animationSpec = tween(ActiveMotion.ms(120)),
        label = "press"
    )
    Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
