package com.resonant.app.sound

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

/**
 * The splash soundtrack: tones synthesized in code, no fundamental below [SoundCues.MIN_AUDIBLE_HZ].
 * A rising sweep as the dots gather, quiet ticks while they orbit, four rising notes as they land
 * in the R, then a soft chord as the rings ripple out. Times are on the animation's own timeline
 * and sped up by [SPEED]. Pure maths, tested on the JVM; see [SoundCueManager.playSplash].
 *
 * Warmth: every tone is a bell-like stack of harmonics played by two slightly detuned voices,
 * and the mix gets a short room tail. A soft limiter lifts the quiet parts so it carries on a
 * phone speaker without clipping.
 */
data class SplashTone(
    val startMs: Int,
    val durMs: Int,
    val hz: Int,
    val hzEnd: Int = hz,
    val gain: Float
)

object SplashSound {

    /** How much faster the splash plays than the timeline below (the picture uses the same number). */
    const val SPEED = 1.35f

    /** Loudest point as a fraction of full scale. Raise for louder, lower for quieter. */
    const val PEAK = 0.8f

    private const val ATTACK_MS = 20
    private const val FLOOR = 1e-4
    private const val TAIL_MS = 500
    private const val DRIVE = 1.6
    private const val REVERB_MIX = 0.25
    private const val MAX_PARTIAL_HZ = 8000.0

    private val PARTIALS = doubleArrayOf(1.0, 0.35, 0.18, 0.08)

    /** Pitch multiplier and level of each voice: the second is about 4 cents sharp. */
    private val VOICES = arrayOf(doubleArrayOf(1.0, 1.0), doubleArrayOf(1.0025, 0.6))

    private val COMB_MS = doubleArrayOf(29.7, 37.1, 41.1, 43.7)
    private val COMB_FEEDBACK = doubleArrayOf(0.68, 0.66, 0.64, 0.62)

    val tones: List<SplashTone> = buildList {
        add(SplashTone(50, 650, 330, 523, 0.08f))
        listOf(800, 1000, 1200, 1400).forEach { add(SplashTone(it, 100, 440, gain = 0.04f)) }
        listOf(392, 494, 587, 784).forEachIndexed { i, hz ->
            add(SplashTone(1950 + i * 80, 500, hz, gain = 0.12f))
        }
        listOf(392, 587, 784).forEach { add(SplashTone(2340, 1300, it, gain = 0.07f)) }
    }

    fun durationMs(): Int = (tones.maxOf { it.startMs + it.durMs } / SPEED).toInt() + TAIL_MS

    fun render(): ShortArray {
        val rate = ToneSynth.SAMPLE_RATE
        val total = ToneSynth.samplesFor(durationMs()) + 1
        val mix = DoubleArray(total)
        for (tone in tones) {
            val start = ToneSynth.samplesFor((tone.startMs / SPEED).toInt())
            val n = ToneSynth.samplesFor((tone.durMs / SPEED).toInt())
            val attack = minOf(ToneSynth.samplesFor(ATTACK_MS), n / 2).coerceAtLeast(1)
            val gain = tone.gain.toDouble()
            val ratio = tone.hzEnd.toDouble() / tone.hz
            val phases = DoubleArray(VOICES.size)
            for (i in 0 until n) {
                val hz = tone.hz * ratio.pow(i.toDouble() / n)
                val env = if (i < attack) {
                    gain * i / attack
                } else {
                    val d = (i - attack).toDouble() / (n - attack).coerceAtLeast(1)
                    gain * exp(ln(FLOOR / gain) * d)
                }
                var sample = 0.0
                for (v in VOICES.indices) {
                    val voiceHz = hz * VOICES[v][0]
                    phases[v] += 2.0 * PI * voiceHz / rate
                    for (k in PARTIALS.indices) {
                        val harmonic = k + 1
                        if (voiceHz * harmonic < MAX_PARTIAL_HZ) {
                            sample += sin(phases[v] * harmonic) * PARTIALS[k] * VOICES[v][1]
                        }
                    }
                }
                val at = start + i
                if (at < total) mix[at] += sample * env
            }
        }
        val wet = reverb(mix, rate)
        for (i in mix.indices) mix[i] += wet[i] * REVERB_MIX

        val loudest = mix.maxOf { abs(it) }
        if (loudest == 0.0) return ShortArray(total)
        val peak = PEAK.toDouble() * Short.MAX_VALUE.toInt()
        return ShortArray(total) { (tanh(DRIVE * mix[it] / loudest) / tanh(DRIVE) * peak).toInt().toShort() }
    }

    private fun reverb(x: DoubleArray, rate: Int): DoubleArray {
        val out = DoubleArray(x.size)
        for (c in COMB_MS.indices) {
            val delay = (COMB_MS[c] * rate / 1000.0).toInt()
            val y = DoubleArray(x.size)
            for (n in x.indices) {
                y[n] = x[n] + if (n >= delay) COMB_FEEDBACK[c] * y[n - delay] else 0.0
                out[n] += y[n] / COMB_MS.size
            }
        }
        return out
    }
}
