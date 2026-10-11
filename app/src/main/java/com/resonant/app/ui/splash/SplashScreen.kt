package com.resonant.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.haptics.HapticPattern
import com.resonant.app.sound.SoundCueManager
import com.resonant.app.sound.SplashSound
import com.resonant.app.ui.theme.ActiveMotion
import com.resonant.app.ui.theme.BrandFamily
import com.resonant.app.ui.theme.BrandInk
import com.resonant.app.ui.theme.BrandOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt

private const val WORDMARK = "RESONANT"
private const val TAGLINE = "Learn by listening."

private const val EXIT_MS = 350

private const val STILL_HOLD_MS = 1500L

/**
 * Launch splash, drawn in the active theme: [BrandOrange] screen, [BrandInk] dots, rings and word.
 * Motion in [SplashMotion], sound in [SplashSound] (honours Sound cues), a haptic tick per dot.
 * Then it scales and fades to uncover the UI underneath. With Reduce motion it shows the
 * finished mark still, with no sound or buzz, and only a brief fade ends it.
 */
@Composable
fun SplashScreen(soundCues: SoundCueManager? = null, onFinished: () -> Unit) {
    val haptics = LocalHapticManager.current
    val reduceMotion = remember { ActiveMotion.reduce }

    val time = remember { mutableFloatStateOf(if (reduceMotion) SplashMotion.FINAL_S else 0f) }
    val exit = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            delay(STILL_HOLD_MS)
            exit.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
            onFinished()
            return@LaunchedEffect
        }

        val start = withFrameNanos { it }
        soundCues?.playSplash()
        for (i in 0 until 4) {
            launch {
                delay(SplashMotion.landingMs(i))
                haptics.play(HapticPattern.CONFIRM, withSound = false)
            }
        }
        do {
            val now = withFrameNanos { it }
            time.floatValue = ((now - start) / 1_000_000_000f * SplashSound.SPEED).coerceAtMost(SplashMotion.ANIM_S)
        } while (time.floatValue < SplashMotion.ANIM_S)

        exit.animateTo(1f, tween(EXIT_MS, easing = FastOutSlowInEasing))
        onFinished()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 1f - exit.value
                val scale = if (reduceMotion) 1f else 1f + exit.value * 0.14f
                scaleX = scale
                scaleY = scale
            }
            .background(BrandOrange)
    ) {
        val density = LocalDensity.current
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val geo = remember(w, h) { SplashMotion.geometry(w, h) }
        val ink = BrandInk

        Canvas(Modifier.fillMaxSize().clearAndSetSemantics { }) {
            val t = time.floatValue
            for (j in 0 until SplashMotion.RING_COUNT) {
                val p = SplashMotion.ringProgress(j, t)
                if (p < 0f) continue
                drawCircle(
                    color = ink.copy(alpha = 0.4f * (1f - p).pow(1.5f)),
                    radius = (170f + p * 800f) * geo.scale,
                    center = Offset(geo.cx, geo.cy),
                    style = Stroke(width = (10f * (1f - p) + 2f) * geo.scale)
                )
            }
            for (i in 0 until 4) {
                val pose = SplashMotion.dotPose(i, t, w, h, geo)
                drawCircle(color = ink, radius = pose.radius, center = Offset(pose.x, pose.y))
            }
        }

        val wordDp = (w / density.density * 0.07f).coerceIn(24f, 44f)
        val tagDp = (w / density.density * 0.036f).coerceIn(14f, 20f)
        val wordSp = with(density) { wordDp.dp.toSp() }
        val tagSp = with(density) { tagDp.dp.toSp() }
        val top = geo.cy + 490f * geo.scale + with(density) { 34.dp.toPx() }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, top.roundToInt()) }
                .clearAndSetSemantics { contentDescription = "Resonant. $TAGLINE" },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.padding(start = (wordDp * 0.34f).dp)) {
                WORDMARK.forEachIndexed { index, letter ->
                    Text(
                        text = letter.toString(),
                        style = TextStyle(
                            fontFamily = BrandFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = wordSp,
                            letterSpacing = 0.34.em
                        ),
                        color = ink,
                        modifier = Modifier.graphicsLayer {
                            val p = ((time.floatValue - SplashMotion.WORD_AT - index * 0.045f) / 0.6f)
                                .coerceIn(0f, 1f)
                            alpha = (p * 1.2f).coerceAtMost(1f)
                            translationY = (1f - SplashMotion.easeOut(p)) * 0.6f * wordDp * density.density
                        }
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = TAGLINE,
                style = TextStyle(
                    fontFamily = BrandFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = tagSp,
                    letterSpacing = 0.06.em
                ),
                color = ink,
                modifier = Modifier.graphicsLayer {
                    alpha = ((time.floatValue - SplashMotion.WORD_AT - 0.55f) / 0.8f).coerceIn(0f, 1f) * 0.85f
                }
            )
        }
    }
}
