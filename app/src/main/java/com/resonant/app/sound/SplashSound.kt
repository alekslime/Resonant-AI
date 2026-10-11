package com.resonant.app.sound

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * The splash soundtrack: tones synthesized in code, none below [SoundCues.MIN_AUDIBLE_HZ].
 * A rising sweep as the dots gather, quiet ticks while they orbit, four rising notes as they land
 * in the R, then a soft chord as the rings ripple out. Times below are on the animation's own
 * timeline and are sped up by [SPEED]. Pure maths, tested on the JVM; see [SoundCueManager.playSplash].
 */
data class SplashTone(
    val startMs: Int,
    val durMs: Int,
    val hz: Int,
    /** Where a sweep ends; equal to [hz] for a steady note. */
    val hzEnd: Int = hz,
    val gain: Float
)

object SplashSound {

    /** How much faster the splash plays than the timeline below (the picture uses the same number). */
    const val SPEED = 1.35f

    private const val ATTACK_MS = 20

    private const val FLOOR = 1e-4

    const val PEAK = 0.5f

    val tones: List<SplashTone> = buildList {
        add(SplashTone(50, 650, 330, 523, 0.05f))
        listOf(800, 1000, 1200, 1400).forEach { add(SplashTone(it, 100, 440, gain = 0.025f)) }
        listOf(392, 494, 587, 784).forEachIndexed { i, hz ->
            add(SplashTone(1950 + i * 80, 500, hz, gain = 0.10f))
        }
        listOf(392, 587, 784).forEach { add(SplashTone(2340, 1300, it, gain = 0.05f)) }
    }

    fun durationMs(): Int = (tones.maxOf { it.startMs + it.durMs } / SPEED).toInt()

    fun render(): ShortArray {
        val rate = ToneSynth.SAMPLE_RATE
        val total = ToneSynth.samplesFor(durationMs()) + 1
        val mix = DoubleArray(total)
        for (tone in tones) {
            val start = ToneSynth.samplesFor((tone.startMs / SPEED).toInt())
            val n = ToneSynth.samplesFor((tone.durMs / SPEED).toInt())
            val attack = minOf(ToneSynth.samplesFor(ATTACK_MS), n / 2).coerceAtLeast(1)
            val ratio = tone.hzEnd.toDouble() / tone.hz
            var phase = 0.0
            for (i in 0 until n) {
                val x = i.toDouble() / n
                val hz = tone.hz * ratio.pow(x) // exponential glide (a no-op when hz == hzEnd)
                phase += 2.0 * PI * hz / rate
                val env = if (i < attack) {
                    tone.gain.toDouble() * i / attack
                } else {
                    val d = (i - attack).toDouble() / (n - attack).coerceAtLeast(1)
                    tone.gain * exp(ln(FLOOR / tone.gain) * d)
                }
                val at = start + i
                if (at < total) mix[at] += sin(phase) * env
            }
        }
        val loudest = mix.maxOf { abs(it) }
        val scale = if (loudest > 0.0) PEAK / loudest else 0.0
        return ShortArray(total) { (mix[it] * scale * Short.MAX_VALUE).toInt().toShort() }
    }
}
