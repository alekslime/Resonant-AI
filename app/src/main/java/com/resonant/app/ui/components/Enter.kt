package com.resonant.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
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

/** The ping that leaves a card when focus lands on it. Plain numbers so the range can be tested. */
internal object GlowTiming {
    /** The whole ping, from the card's edge to gone. One shot, so it is short. */
    const val PING_MS = 520
    /** How far the wave travels past the card's edge by the time it is gone. */
    const val MAX_SPREAD_DP = 12f
    /** How strong the wave is where it leaves the card. */
    const val START_ALPHA = 0.26f
    /** Soft edge: the wave is drawn as this many nested layers, thinning outwards. */
    const val LAYERS = 3

    /** How far the wave has travelled at [progress] 0..1. */
    fun spreadDp(progress: Float): Float = MAX_SPREAD_DP * progress.coerceIn(0f, 1f)

    /** How strong the whole wave is at [progress] 0..1: full at the card, fading to nothing as it goes. */
    fun alpha(progress: Float): Float {
        val left = 1f - progress.coerceIn(0f, 1f)
        return START_ALPHA * left * left
    }
}

/**
 * A resonance ping: when focus lands on a card, a soft filled wave in the theme's ink leaves its edge, spreads
 * outwards and fades away once. Then the card is still. Not a stroke and not a loop. Nothing at all with Reduce
 * motion on, and never the only signal: the inverted card, the haptic and the speech still say where focus is.
 */
fun Modifier.focusGlow(focused: Boolean, shape: Shape? = null): Modifier = composed {
    // 1f means finished (nothing drawn).
    val progress = remember { Animatable(1f) }
    LaunchedEffect(focused) {
        if (focused && !ActiveMotion.reduce) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(GlowTiming.PING_MS, easing = FastOutSlowInEasing))
        } else {
            progress.snapTo(1f)
        }
    }
    val ink = BrandInk
    val waveShape = shape ?: cardShape()
    Modifier.drawBehind {
        // Read in the draw phase: the wave redraws each frame without recomposing the card.
        val p = progress.value
        if (p >= 1f) return@drawBehind
        val layerAlpha = GlowTiming.alpha(p) / GlowTiming.LAYERS
        val spread = GlowTiming.spreadDp(p).dp.toPx()
        for (k in 1..GlowTiming.LAYERS) {
            val grow = spread * k / GlowTiming.LAYERS
            val outline = waveShape.createOutline(
                Size(size.width + 2 * grow, size.height + 2 * grow),
                layoutDirection,
                this
            )
            translate(-grow, -grow) {
                drawOutline(outline, ink.copy(alpha = layerAlpha))
            }
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
 * and a soft ping leaves it as focus lands (see [focusGlow]). Colour already fades (see [focusColor]); this adds size and
 * life. Off with Reduce motion.
 */
fun Modifier.focusLift(focused: Boolean, amount: Float = 1.02f): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = if (focused && !ActiveMotion.reduce) amount else 1f,
        animationSpec = SpringFeel.focus(),
        label = "focusLift"
    )
    Modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .focusGlow(focused)
}
