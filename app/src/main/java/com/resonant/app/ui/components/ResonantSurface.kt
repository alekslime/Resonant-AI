package com.resonant.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.resonant.app.ui.theme.ActiveMotion
import com.resonant.app.ui.theme.BrandInk
import com.resonant.app.ui.theme.LocalResonantColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/**
 * The slow drift behind every screen: two very soft patches of the theme's ink that wander across the
 * background, one lap taking minutes. Plain numbers so the path and strength can be tested.
 */
internal object AmbientMotion {
    /** Seconds for one full lap of each patch. Deliberately very slow. */
    const val PERIOD_A_S = 150f
    const val PERIOD_B_S = 210f

    /** Ink strength at the centre of a patch. Two patches overlapping give at most twice this. */
    const val PATCH_ALPHA = 0.08f

    /** Patch radius as a share of the longer screen side. */
    const val RADIUS_SHARE = 0.9f

    /** How often the position is refreshed. The drift is so slow that this is invisible, and it spares the battery. */
    const val TICK_MS = 100L

    class Point(val x: Float, val y: Float)

    /** Where a patch is, as a 0..1 share of the screen, [lapShare] of the way round (0..1). Loops without a jump. */
    fun position(lapShare: Float, xTurns: Int, yTurns: Int, xShift: Float, yShift: Float): Point {
        val tau = (2.0 * PI).toFloat()
        return Point(
            x = 0.5f + 0.35f * sin(tau * xTurns * lapShare + xShift),
            y = 0.5f + 0.30f * sin(tau * yTurns * lapShare + yShift)
        )
    }

    fun lapShare(elapsedS: Float, periodS: Float): Float = (elapsedS % periodS) / periodS
}


/**
 * The Resonant surface: a flat, harsh two/three-stop gradient — no grain, no
 * blur. Replaces the earlier soft blurred-blob background image. Read from
 * [LocalResonantColors] so it automatically switches between the light and dark palette; screens
 * never branch on dark mode themselves.
 *
 * Every screen sits on this, so moving between screens never changes the
 * background — only the content on top of it.
 */
@Composable
fun ResonantSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LocalResonantColors.current
    SystemBarsColor(colors.statusBar)

    // Seconds since this screen appeared; stands still with Reduce motion on.
    var elapsedS by remember { mutableFloatStateOf(0f) }
    val reduce = ActiveMotion.reduce
    val profile = ActiveMotion.profile
    val drifts = profile.driftStrength > 0f
    LaunchedEffect(reduce, drifts) {
        if (reduce || !drifts) return@LaunchedEffect
        val start = System.nanoTime()
        while (true) {
            elapsedS = (System.nanoTime() - start) / 1_000_000_000f
            delay(AmbientMotion.TICK_MS)
        }
    }
    val ink = BrandInk
    val patchAlpha = AmbientMotion.PATCH_ALPHA * profile.driftStrength
    val slowdown = profile.driftSlowdown

    Box(
        modifier
            .fillMaxSize()
            .background(colors.gradient)
            .drawBehind {
                // Read in the draw phase, so the drift redraws without recomposing the screen.
                val t = elapsedS
                if (patchAlpha <= 0f) return@drawBehind
                val radius = max(size.width, size.height) * AmbientMotion.RADIUS_SHARE
                val a = AmbientMotion.position(AmbientMotion.lapShare(t, AmbientMotion.PERIOD_A_S * slowdown), 1, 2, 0.8f, 0f)
                val b = AmbientMotion.position(AmbientMotion.lapShare(t, AmbientMotion.PERIOD_B_S * slowdown), 1, 1, 2.6f, 1.9f)
                for (patch in listOf(a, b)) {
                    val center = Offset(patch.x * size.width, patch.y * size.height)
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(ink.copy(alpha = patchAlpha), ink.copy(alpha = 0f)),
                            center = center,
                            radius = radius
                        )
                    )
                }
            },
        content = content
    )
}
