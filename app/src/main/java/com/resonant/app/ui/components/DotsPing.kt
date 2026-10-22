package com.resonant.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Fill
import com.resonant.app.ui.theme.ActiveMotion
import com.resonant.app.ui.theme.BrandInk
import kotlin.math.min

/** The ping behind the dots when the conversation changes state. Plain numbers so the range can be tested. */
internal object DotsPingTiming {
    const val PING_MS = 700
    /** Pings closer together than this are skipped, so a flickering state never becomes a flicker of rings. */
    const val MIN_GAP_MS = 1200L
    const val START_ALPHA = 0.22f
    /** The wave runs from this share of the dots' half-size to that share. */
    const val RADIUS_FROM = 0.7f
    const val RADIUS_TO = 1.3f

    fun radius(progress: Float, halfSize: Float): Float =
        halfSize * (RADIUS_FROM + (RADIUS_TO - RADIUS_FROM) * progress.coerceIn(0f, 1f))

    /** Strongest at the start, gone at the end. */
    fun alpha(progress: Float): Float {
        val left = 1f - progress.coerceIn(0f, 1f)
        return START_ALPHA * left * left
    }

    fun shouldPing(sinceLastMs: Long): Boolean = sinceLastMs >= MIN_GAP_MS
}

/**
 * Wraps the dots. When [state] changes (listening, thinking, speaking, offline...), a soft filled wave leaves the
 * dots once and fades, so the change can be felt as well as heard. Not on the first draw. Nothing with Reduce
 * motion on or in a theme whose motion profile has no ping (see MotionProfile).
 */
@Composable
fun DotsPing(state: DotsState, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val progress = remember { Animatable(1f) } // 1f = finished, nothing drawn
    var first by remember { mutableStateOf(true) }
    var lastPingAt by remember { mutableLongStateOf(0L) }
    val profile = ActiveMotion.profile
    val strength = profile.pingStrength
    val ink = BrandInk
    LaunchedEffect(state) {
        if (first) { first = false; return@LaunchedEffect }
        val now = System.currentTimeMillis()
        if (ActiveMotion.reduce || strength <= 0f || !DotsPingTiming.shouldPing(now - lastPingAt)) return@LaunchedEffect
        lastPingAt = now
        progress.snapTo(0f)
        progress.animateTo(1f, tween(DotsPingTiming.PING_MS, easing = FastOutSlowInEasing))
    }
    Box(
        modifier.drawBehind {
            val p = progress.value
            if (p >= 1f) return@drawBehind
            drawCircle(
                color = ink.copy(alpha = DotsPingTiming.alpha(p) * strength),
                radius = DotsPingTiming.radius(p, min(size.width, size.height) / 2f),
                center = Offset(size.width / 2f, size.height / 2f),
                style = Fill
            )
        }
    ) { content() }
}
