package com.resonant.app.ui.splash

import com.resonant.app.sound.SplashSound
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * The splash choreography as pure functions of time (unit tested). Dots fly in and gather (listening),
 * orbit (thinking), land one by one in the braille R and pulse (speaking), then rings ripple out.
 * Geometry is in logo units, so the finished mark matches the brand logo at any screen size.
 */
internal object SplashMotion {

    const val DOT_R = 147f
    const val GATHER_END = 0.7f
    const val ORBIT_END = 1.6f
    const val LAND_STAGGER = 0.08f
    const val LAND_DUR = 0.5f

    const val RINGS_AT = ORBIT_END + 3 * LAND_STAGGER + LAND_DUR
    const val RING_GAP = 0.18f
    const val RING_LIFE = 1.2f
    const val RING_COUNT = 3

    const val WORD_AT = 2.3f

    const val ANIM_S = 3.6f

    /** Reduce motion shows this moment: everything settled. */
    const val FINAL_S = 10f

    /** Resting place of braille dots 1, 2, 3 and 5, from the middle of the logo. */
    private val FINAL = arrayOf(
        floatArrayOf(-183f, -340f),
        floatArrayOf(-183f, 0f),
        floatArrayOf(-183f, 339f),
        floatArrayOf(184f, 0f)
    )

    /** Start of each dot: a fraction of the screen from its middle, off in the corners. */
    private val START = arrayOf(
        floatArrayOf(-0.60f, -0.50f),
        floatArrayOf(0.60f, -0.45f),
        floatArrayOf(-0.55f, 0.55f),
        floatArrayOf(0.60f, 0.50f)
    )

    class Geometry(val scale: Float, val cx: Float, val cy: Float)
    class DotPose(val x: Float, val y: Float, val radius: Float)

    fun geometry(w: Float, h: Float): Geometry =
        Geometry(scale = min(h * 0.36f / 973f, w * 0.5f / 760f), cx = w / 2f, cy = h * 0.44f)

    /** Real time, in ms from the start, when dot [i] settles: the haptic tick. */
    fun landingMs(i: Int): Long = ((ORBIT_END + i * LAND_STAGGER + 0.4f) * 1000f / SplashSound.SPEED).toLong()

    /** 0..1 progress of ring [j], -1 when off screen. */
    fun ringProgress(j: Int, t: Float): Float {
        val age = t - RINGS_AT - j * RING_GAP
        return if (age > 0f && age < RING_LIFE) age / RING_LIFE else -1f
    }

    fun dotPose(i: Int, t: Float, w: Float, h: Float, g: Geometry): DotPose {
        val s = g.scale
        val a = (-PI / 2 + i * PI / 2).toFloat()
        val ringX = g.cx + cos(a) * 150f * s
        val ringY = g.cy + sin(a) * 150f * s
        val x: Float
        val y: Float
        var k: Float
        when {
            t < GATHER_END -> {
                val u = easeInOutCubic(t / GATHER_END)
                x = lerp(g.cx + START[i][0] * w, ringX, u)
                y = lerp(g.cy + START[i][1] * h, ringY, u)
                k = 0.55f * easeOutCubic(min(1f, t / 0.5f))
            }
            t < ORBIT_END -> {
                val u = (t - GATHER_END) / (ORBIT_END - GATHER_END)
                val angle = a + (2 * PI).toFloat() * easeInOutCubic(u)
                val r = (150f + 70f * sin(PI.toFloat() * u)) * s
                x = g.cx + cos(angle) * r
                y = g.cy + sin(angle) * r
                k = 0.55f
            }
            else -> {
                val start = ORBIT_END + i * LAND_STAGGER
                val v = ((t - start) / LAND_DUR).coerceIn(0f, 1f)
                val b = easeOutBack(v)
                x = lerp(ringX, g.cx + FINAL[i][0] * s, b)
                y = lerp(ringY, g.cy + FINAL[i][1] * s, b)
                k = 0.55f + 0.45f * easeOutCubic(v)
                val sincePulse = t - (start + LAND_DUR)
                if (sincePulse > 0f && sincePulse < 1f) {
                    k *= 1f + 0.08f * sin(sincePulse * 20f + i) * (1f - sincePulse)
                }
            }
        }
        return DotPose(x, y, DOT_R * s * k.coerceAtLeast(0f))
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    private fun easeOutCubic(v: Float) = 1f - (1f - v).pow(3)
    private fun easeInOutCubic(t: Float) = if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f
    private fun easeOutBack(v: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        return 1f + c3 * (v - 1f).pow(3) + c1 * (v - 1f).pow(2)
    }

    fun easeOut(v: Float) = easeOutCubic(v.coerceIn(0f, 1f))
}
