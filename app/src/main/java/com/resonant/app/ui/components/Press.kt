package com.resonant.app.ui.components

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.resonant.app.ui.theme.ActiveMotion

/**
 * How touch and focus feel: light springs that overshoot a hair and settle, instead of fixed-time fades.
 * Damping stays high on purpose, so it is a soft give and never a bounce. Plain numbers so they are tested.
 */
internal object SpringFeel {
    /** 1.0 would not overshoot at all; lower is bouncier. Kept well above a visible wobble. */
    const val PRESS_DAMPING = 0.6f
    const val PRESS_STIFFNESS = Spring.StiffnessMedium
    const val FOCUS_DAMPING = 0.55f
    const val FOCUS_STIFFNESS = Spring.StiffnessMediumLow

    /** The press spring, or an instant jump with Reduce motion on. */
    fun press(): FiniteAnimationSpec<Float> =
        if (ActiveMotion.reduce) snap() else spring(PRESS_DAMPING, PRESS_STIFFNESS)

    /** The focus-lift spring, or an instant jump with Reduce motion on. */
    fun focus(): FiniteAnimationSpec<Float> =
        if (ActiveMotion.reduce) snap() else spring(FOCUS_DAMPING, FOCUS_STIFFNESS)
}

/**
 * A button or card shrinks a little while a finger is on it and springs back on release, with a hair of overshoot. Purely
 * a visual reply to a touch: the haptic, sound and speech are still what tell you things.
 * Off when Reduce motion is on. Put it before the clip and background so the whole thing scales.
 */
fun Modifier.pressScale(source: InteractionSource): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !ActiveMotion.reduce) 0.97f else 1f,
        animationSpec = SpringFeel.press(),
        label = "press"
    )
    Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
