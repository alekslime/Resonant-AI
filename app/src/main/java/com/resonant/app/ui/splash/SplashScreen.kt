package com.resonant.app.ui.splash

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.ui.components.DotsState
import com.resonant.app.ui.components.ResonantDots
import com.resonant.app.ui.components.idleLetterFormedAt
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

/** Idle word playback speed on the splash: the whole R-e-s-o-n-a-n-t spell-out in ~2.2 s. */
private const val SPLASH_SPEED = 3.5f

/**
 * Animated launch splash. Not a fade-in: the braille "R" mark (see ResonantDots) grows in,
 * then spells R-e-s-o-n-a-n-t in braille at speed, with each wordmark letter landing as its
 * braille letter forms. It holds briefly, then the whole mark scales and fades to uncover the
 * real UI already sitting underneath it.
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

    val letterReveal = remember { WORDMARK.map { Animatable(0f) } }
    val exit = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            letterReveal.forEach { it.snapTo(1f) }
            delay(200)
            exit.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
            onFinished()
            return@LaunchedEffect
        }

        // The dots spell R-e-s-o-n-a-n-t in braille (ResonantDots idle, sped up) and each
        // wordmark letter lands the moment its braille letter is fully formed.
        launch {
            delay(450L)
            haptics.play(HapticPattern.SECTION_CHANGE) // the R has grown in
        }
        letterReveal.forEachIndexed { index, anim ->
            launch {
                delay((idleLetterFormedAt(index) / SPLASH_SPEED * 1000f).toLong())
                anim.animateTo(
                    1f,
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }

        delay((idleLetterFormedAt(letterReveal.lastIndex) / SPLASH_SPEED * 1000f).toLong() + 700L)
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
            ResonantDots(
                state = DotsState.Idle,
                timeScale = SPLASH_SPEED,
                appearFromNothing = true,
                modifier = Modifier.size(width = 120.dp, height = 140.dp)
            )

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
