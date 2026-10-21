package com.resonant.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.resonant.app.ui.theme.ActiveMotion
import com.resonant.app.ui.theme.BrandInk
import com.resonant.app.ui.theme.cardShape
import kotlinx.coroutines.delay

/** How the items of a list arrive one after another. Plain numbers so the timing can be tested. */
internal object EnterTiming {
    const val STEP_MS = 45
    const val MAX_STEPS = 8
    const val DURATION_MS = 300

    /** Item 0 starts at once; each later one waits a little longer, up to a cap, so a long list is not slow. */
    fun delayMs(index: Int): Int = index.coerceIn(0, MAX_STEPS) * STEP_MS
}

/** How the focus glow breathes. Plain numbers so the range can be tested. */
internal object GlowTiming {
    /** One swell takes this long; swell and fade back make a breath of twice this. Slow on purpose. */
    const val HALF_PERIOD_MS = 1600
    const val MIN_SPREAD_DP = 2f
    const val MAX_SPREAD_DP = 7f
    const val MIN_ALPHA = 0.10f
    const val MAX_ALPHA = 0.32f

    /** How far the halo reaches past the card at [pulse] 0..1. */
    fun spreadDp(pulse: Float): Float = MIN_SPREAD_DP + (MAX_SPREAD_DP - MIN_SPREAD_DP) * pulse.coerceIn(0f, 1f)

    /** How strong the halo is at [pulse] 0..1. Never fully gone, never solid. */
    fun alpha(pulse: Float): Float = MIN_ALPHA + (MAX_ALPHA - MIN_ALPHA) * pulse.coerceIn(0f, 1f)
}

/**
 * A soft halo in the theme's ink that slowly swells and fades behind the focused card, so the focus feels
 * alive and not only drawn. Only the focused card runs it. Nothing at all with Reduce motion on, and it is
 * never the only signal: the inverted card, the haptic and the speech still say where focus is.
 */
fun Modifier.focusGlow(focused: Boolean, shape: Shape? = null): Modifier = composed {
    if (!focused || ActiveMotion.reduce) return@composed Modifier
    val transition = rememberInfiniteTransition(label = "focusGlow")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(GlowTiming.HALF_PERIOD_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "focusGlowPulse"
    )
    val ink = BrandInk
    val glowShape = shape ?: cardShape()
    Modifier.drawBehind {
        // Read in the draw phase: the glow redraws each frame without recomposing the card.
        val p = pulse
        val spread = GlowTiming.spreadDp(p).dp.toPx()
        val outline = glowShape.createOutline(
            Size(size.width + 2 * spread, size.height + 2 * spread),
            layoutDirection,
            this
        )
        translate(-spread, -spread) {
            drawOutline(outline, ink.copy(alpha = GlowTiming.alpha(p)))
        }
    }
}

/**
 * Fades an item in while it rises a few dp, a moment after the one before it, when its screen opens. Plays once.
 * Never delays speech or touch: the item is there and works from the first frame, it is only drawn lighter.
 * Instant with Reduce motion on.
 */
fun Modifier.enterStagger(index: Int): Modifier = composed {
    val appear = remember { Animatable(if (ActiveMotion.reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (appear.value < 1f) {
            delay(EnterTiming.delayMs(index).toLong())
            appear.animateTo(1f, tween(ActiveMotion.ms(EnterTiming.DURATION_MS), easing = FastOutSlowInEasing))
        }
    }
    val rise = with(LocalDensity.current) { 14.dp.toPx() }
    Modifier.graphicsLayer {
        alpha = appear.value
        translationY = (1f - appear.value) * rise
    }
}

/**
 * The item that has focus grows a touch and settles back when focus moves on, so focus can be seen travelling,
 * and it breathes a soft halo (see [focusGlow]). Colour already fades (see [focusColor]); this adds size and
 * life. Off with Reduce motion.
 */
fun Modifier.focusLift(focused: Boolean, amount: Float = 1.02f): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = if (focused && !ActiveMotion.reduce) amount else 1f,
        animationSpec = tween(ActiveMotion.ms(200), easing = FastOutSlowInEasing),
        label = "focusLift"
    )
    Modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .focusGlow(focused)
}
