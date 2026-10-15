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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.resonant.app.ui.theme.ActiveMotion
import kotlinx.coroutines.delay

/** How the items of a list arrive one after another. Plain numbers so the timing can be tested. */
internal object EnterTiming {
    const val STEP_MS = 45
    const val MAX_STEPS = 8
    const val DURATION_MS = 300

    /** Item 0 starts at once; each later one waits a little longer, up to a cap, so a long list is not slow. */
    fun delayMs(index: Int): Int = index.coerceIn(0, MAX_STEPS) * STEP_MS
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
 * The item that has focus grows a touch and settles back when focus moves on, so focus can be seen travelling.
 * Colour already fades (see [focusColor]); this adds size. Off with Reduce motion.
 */
fun Modifier.focusLift(focused: Boolean, amount: Float = 1.02f): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = if (focused && !ActiveMotion.reduce) amount else 1f,
        animationSpec = tween(ActiveMotion.ms(200), easing = FastOutSlowInEasing),
        label = "focusLift"
    )
    Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
