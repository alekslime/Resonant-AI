package com.resonant.app.ui.splash

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.haptics.HapticPattern
import com.resonant.app.ui.theme.MetropolisBlack
import com.resonant.app.ui.theme.BrandOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Brand mark color on top of [BrandOrange]. The background itself now lives
 * in ui/theme/Color.kt (BrandOrange) since Home uses the exact same flat
 * color per the Figma redesign — kept in one place instead of two copies of
 * the same hex.
 */
private val SplashMark = Color(0xFF0A0A0A)

private const val WORDMARK = "RESONANT"

/**
 * The four dots are the same braille cell as the launcher's adaptive-icon
 * foreground (dots 1-2-3-5, the braille letter "R") — see
 * ic_launcher_foreground.xml for the source measurements this layout is
 * derived from. Order is column-then-crossbar, i.e. the order a fingertip
 * would actually read the cell: top-left, mid-left, bottom-left, then the
 * mid-right dot that turns "the letter I" into "the letter R".
 */
private data class DotSpec(val dx: Float, val dy: Float)

private val DOT_RADIUS = 9.dp
private val DOT_LAYOUT = listOf(
    DotSpec(dx = -1f, dy = -1f),   // top-left    (braille dot 1)
    DotSpec(dx = -1f, dy = 0f),    // mid-left    (braille dot 2)
    DotSpec(dx = -1f, dy = 1f),    // bottom-left (braille dot 3)
    DotSpec(dx = 1f, dy = 0f)      // mid-right   (braille dot 5)
)
private val DOT_SPACING_X = 22.dp
private val DOT_SPACING_Y = 21.dp

/**
 * Animated launch splash. Not a fade-in: the app's own braille "R" mark
 * (see ic_launcher_foreground.xml) assembles dot by dot — the order a
 * fingertip would trace the cell — each landing with a small resonant
 * overshoot and an expanding ring, echoing the haptic pulses the rest of
 * the app uses for confirmation. The wordmark then reveals letter by
 * letter, holds briefly, and the whole mark scales and fades to uncover
 * the real UI already sitting underneath it.
 *
 * Respects the system "remove animations" accessibility setting
 * (Settings.Global.ANIMATOR_DURATION_SCALE == 0): in that case the mark
 * appears instantly and only the final reveal is a plain, brief cross-fade.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticManager.current
    val reduceMotion = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }

    val dotReveal = remember { DOT_LAYOUT.map { Animatable(0f) } }
    val letterReveal = remember { WORDMARK.map { Animatable(0f) } }
    val exit = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            dotReveal.forEach { it.snapTo(1f) }
            letterReveal.forEach { it.snapTo(1f) }
            delay(200)
            exit.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
            onFinished()
            return@LaunchedEffect
        }

        // Dots assemble in reading order, each with a small resonant
        // overshoot rather than a hard stop.
        dotReveal.forEachIndexed { index, anim ->
            launch {
                delay(index * 110L)
                anim.animateTo(
                    1f,
                    spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }

        // The last dot (the one that turns the cell into "R") is the beat
        // the haptic pulse rides on — the same confirmation vocabulary the
        // rest of the app uses when something locks in.
        delay(DOT_LAYOUT.size * 110L + 120L)
        haptics.play(HapticPattern.SECTION_CHANGE)

        letterReveal.forEachIndexed { index, anim ->
            launch {
                delay(index * 35L)
                anim.animateTo(
                    1f,
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }

        delay(letterReveal.size * 35L + 350L)
        exit.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 1f - exit.value
                val scale = 1f + exit.value * 0.14f
                scaleX = scale
                scaleY = scale
            }
            .background(BrandOrange),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(width = 64.dp, height = 84.dp),
                contentAlignment = Alignment.Center
            ) {
                DOT_LAYOUT.forEachIndexed { index, spec ->
                    BrailleDot(
                        reveal = dotReveal[index].value,
                        modifier = Modifier.offset(
                            x = spec.dx * DOT_SPACING_X,
                            y = spec.dy * DOT_SPACING_Y
                        )
                    )
                }
            }

            Row(modifier = Modifier.offset(y = (-4).dp)) {
                WORDMARK.forEachIndexed { index, letter ->
                    val value = letterReveal[index].value
                    Text(
                        text = letter.toString(),
                        style = TextStyle(
                            fontFamily = MetropolisBlack,
                            fontWeight = FontWeight.Black,
                            fontSize = 34.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = SplashMark,
                        modifier = Modifier.graphicsLayer {
                            alpha = value.coerceIn(0f, 1f)
                            translationY = (1f - value.coerceIn(0f, 1f)) * 28f
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BrailleDot(reveal: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Expanding ring: a resonance pulse leaving the dot as it lands.
        Box(
            modifier = Modifier
                .size(DOT_RADIUS * 2)
                .graphicsLayer {
                    val ring = reveal.coerceAtLeast(0f)
                    scaleX = 1f + ring * 1.1f
                    scaleY = 1f + ring * 1.1f
                    alpha = (1f - ring).coerceIn(0f, 1f) * 0.5f
                }
                .border(2.dp, SplashMark, CircleShape)
        )
        // The solid dot itself, riding the same spring value so its
        // landing slightly overshoots before settling — the "resonant"
        // wobble, not a hard stop.
        Box(
            modifier = Modifier
                .size(DOT_RADIUS * 2)
                .graphicsLayer {
                    scaleX = reveal
                    scaleY = reveal
                    alpha = reveal.coerceIn(0f, 1f)
                }
                .background(SplashMark, CircleShape)
        )
    }
}
