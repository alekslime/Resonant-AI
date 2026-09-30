package com.resonant.app.ui.components

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.resonant.app.ui.theme.BrandInk
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

enum class DotsState { Idle, Listening, Transcribing, Thinking, Speaking, Offline }

/**
 * The braille "R" mark, alive. Six dot slots (a full braille cell); the logo uses 1-2-3-5.
 *
 *  - Idle: spells R-e-s-o-n-a-n-t in braille, eased, with a speed ramp (slow -> fast -> slow).
 *  - Listening: R tightens, drifts and sways; the right dot leans out like an ear. With a
 *    [level] (live mic loudness) the ear reaches out further and the others get more restless.
 *  - Transcribing: a tighter, faster orbit — you stopped talking and the words are being
 *    turned into text, so it never looks frozen.
 *  - Thinking: four dots orbit with a speed-ramped loop.
 *  - Speaking: dots ride a traveling wave with squash/stretch, driven by [level] (0..1).
 *  - Offline: the R with its right dot cut loose, drifting away and flickering — the AI server
 *    is unreachable and answers come from the bundled lessons.
 *
 * Decorative: cleared from the semantics tree, since state is already spoken/haptic.
 * With "remove animations" on, it holds a static pose.
 *
 * @param level loudness 0..1, read every frame: speech loudness while Speaking, mic loudness
 *   while Listening. null = built-in fake voice envelope / baseline drift.
 * @param timeScale playback speed of the idle word (splash runs it fast).
 * @param appearFromNothing start with all dots at scale 0 and grow into the first pose (splash).
 */
@Composable
fun ResonantDots(
    state: DotsState,
    modifier: Modifier = Modifier,
    color: Color = BrandInk,
    level: (() -> Float)? = null,
    timeScale: Float = 1f,
    appearFromNothing: Boolean = false
) {
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) withFrameNanos { time = (it - start) / 1_000_000_000f }
    }
    val engine = remember { DotsEngine(appearFromNothing) }

    Canvas(modifier.clearAndSetSemantics { }) {
        val t = time // read in draw phase: redraws each frame without recomposing
        val pose = engine.frame(state, t, level?.invoke(), reduceMotion, timeScale)
        val k = min(size.width, size.height) / 1400f
        val c = Offset(size.width / 2f, size.height / 2f)
        for (i in 0 until 6) {
            val sx = pose[i * 4 + 2]; val sy = pose[i * 4 + 3]
            if (sx < .003f || sy < .003f) continue
            val rx = R * sx * k; val ry = R * sy * k
            drawOval(
                color,
                topLeft = Offset(c.x + pose[i * 4] * k - rx, c.y + pose[i * 4 + 1] * k - ry),
                size = Size(rx * 2, ry * 2)
            )
        }
    }
}

// ---- geometry, measured from the logo PNG (units: px of a 1920 source) -----------------------
private const val R = 147f
private const val CX = 183.5f
private const val CY = 339f
private val SLOT = arrayOf(
    -CX to -CY, -CX to 0f, -CX to CY, // dots 1-2-3
    CX to -CY, CX to 0f, CX to CY     // dots 4-5-6
)
private val ACT = intArrayOf(0, 1, 2, 4)

// braille: R e s o n a n t
private val LETTERS = arrayOf(
    intArrayOf(1, 2, 3, 5), intArrayOf(1, 5), intArrayOf(2, 3, 4), intArrayOf(1, 3, 5),
    intArrayOf(1, 3, 4, 5), intArrayOf(1), intArrayOf(1, 3, 4, 5), intArrayOf(2, 3, 4, 5)
)
private val HOLD = floatArrayOf(1.6f, .5f, .3f, .25f, .25f, .3f, .5f, .9f)   // speed ramp
private val TRANS = floatArrayOf(.9f, .7f, .5f, .4f, .4f, .5f, .7f, .9f)
private val TOTAL = HOLD.sum() + TRANS.sum()

/** Seconds (at timeScale 1) until idle letter [i] of R-e-s-o-n-a-n-t is fully formed. */
internal fun idleLetterFormedAt(i: Int): Float {
    var t = 0f
    for (j in 0 until i.coerceIn(0, 7)) t += HOLD[j] + TRANS[j]
    return t
}

private fun clamp(x: Float, a: Float = 0f, b: Float = 1f) = x.coerceIn(a, b)
private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
private fun ease(t: Float) = if (t < .5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f
private fun mask(l: IntArray) = FloatArray(6) { if ((it + 1) in l) 1f else 0f }

internal class DotsEngine(fromNothing: Boolean) {
    private var cur = DotsState.Idle
    private var snap = FloatArray(24)
    private val pose = FloatArray(24)
    private val tgt = FloatArray(24)
    private var switchT = if (fromNothing) 0f else -10f
    private var smooth = 0f
    private var lastT = 0f

    fun frame(state: DotsState, t: Float, level: Float?, reduceMotion: Boolean, timeScale: Float = 1f): FloatArray {
        if (state != cur) { snap = pose.copyOf(); cur = state; switchT = t }
        val dt = (t - lastT).coerceIn(0f, .1f); lastT = t
        val raw = level ?: -1f
        if (raw >= 0f) {
            val rate = if (raw > smooth) 18f else 5f // fast attack, slower release
            smooth += (raw - smooth) * (1f - exp(-rate * dt))
        }
        tgt.fill(0f)
        when (state) {
            DotsState.Idle -> idle(t * timeScale)
            DotsState.Offline -> offline(t)
            DotsState.Listening -> listening(t, if (raw >= 0f) smooth else null)
            DotsState.Thinking -> thinking(t)
            DotsState.Transcribing -> thinking(t * 1.7f, 240f)
            DotsState.Speaking -> speaking(t, if (raw >= 0f) smooth else null)
        }
        val e = if (reduceMotion) 1f else ease(clamp((t - switchT) / .9f))
        for (i in 0 until 24) pose[i] = lerp(snap[i], tgt[i], e)
        return pose
    }

    private fun put(k: Int, x: Float, y: Float, sx: Float, sy: Float = sx) {
        tgt[k * 4] = x; tgt[k * 4 + 1] = y; tgt[k * 4 + 2] = sx; tgt[k * 4 + 3] = sy
    }
    private fun rot(x: Float, y: Float, a: Float) =
        (x * cos(a) - y * sin(a)) to (x * sin(a) + y * cos(a))

    private fun idle(time: Float) {
        var t = time % TOTAL; var i = 0
        while (i < 7 && t >= HOLD[i] + TRANS[i]) { t -= HOLD[i] + TRANS[i]; i++ }
        val a = mask(LETTERS[i]); val b = mask(LETTERS[(i + 1) % 8])
        val p = clamp((t - HOLD[i]) / TRANS[i])
        val ang = sin(PI.toFloat() * p) * .3f * (if (i % 2 == 1) -1f else 1f)
        for (k in 0 until 6) {
            val e = ease(clamp(p * 1.5f - .5f * k / 5f))
            val s = lerp(a[k], b[k], e)
            val (x, y) = SLOT[k]
            val (rx, ry) = rot(x * (.55f + .45f * s), y * (.55f + .45f * s), ang)
            put(k, rx, ry, s)
        }
    }

    private fun offline(t: Float) {
        val flick = ease(clamp((sin(t * 2.3f) * sin(t * 5.1f) + .2f) * 2f))
        for (k in ACT) {
            val (x, y) = SLOT[k]
            if (k == 4) {
                // the "ear" dot: cut loose, drifting out and stuttering
                put(k, x + 110f + 40f * sin(t * .5f), y + 30f * sin(t * .8f), .3f + .7f * flick)
            } else {
                put(k, x + 10f * sin(t * .6f + k), y + 10f * cos(t * .5f + k), 1f)
            }
        }
    }

    private fun listening(t: Float, v: Float?) {
        val lv = v ?: 0f
        val sway = .14f * sin(t * .6f) * (1f + lv)
        val amp = 34f * (1f + 1.5f * lv) // restlessness follows the voice
        for (k in ACT) {
            val ph = k * 1.7f
            val (x, y) = SLOT[k]
            var px = x * .88f + amp * sin(t * .9f + ph)
            val py = y * .88f + amp * cos(t * .7f + ph * 1.3f)
            // right dot = the ear: leans out further the louder the mic hears you
            if (k == 4) px += if (v == null) 70f * ease(.5f + .5f * sin(t * .8f)) else 20f + 150f * lv
            val (rx, ry) = rot(px, py, sway)
            put(k, rx, ry, 1f)
        }
    }

    private fun thinking(t: Float, radius: Float = 330f) {
        val u = t / 2.6f
        val tau = (2 * PI).toFloat()
        val ph = tau * u - .6f * sin(tau * u) // speed ramps, never reverses
        val ry = radius * (.85f + .15f * sin(t * .7f))
        ACT.forEachIndexed { i, k ->
            val a = ph + i * PI.toFloat() / 2f
            val s = .72f + .28f * (.5f + .5f * sin(a))
            val (x, y) = rot(radius * cos(a), ry * sin(a), -.5f * sin(t * .5f))
            put(k, x, y, s)
        }
    }

    private fun speaking(t: Float, level: Float?) {
        val v = level ?: clamp(.35f + .4f * sin(t * 2.1f) + .3f * sin(t * 3.3f + 1.3f))
        ACT.forEachIndexed { i, k ->
            val w = sin(t * 7.5f - i * .8f)
            val sy = 1f + .28f * v * abs(w)
            val (x, y) = rot((i - 1.5f) * 330f, -260f * v * w, .08f * sin(t * .9f))
            put(k, x, y, 1f / sy, sy)
        }
    }
}
