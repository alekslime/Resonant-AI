package com.resonant.app.sound

import com.resonant.app.haptics.HapticPattern
import kotlin.math.PI
import kotlin.math.sin

/**
 * The prototype's SOUND vocabulary: one short tone sequence per [HapticPattern], so
 * whatever the phone says with a buzz it can also say with a note. Like the haptic
 * vocabulary these are a starting point, kept in one place ([SoundCues.specs]) so
 * they are trivial to retune by ear.
 *
 * Why sound as well as touch: several haptic patterns are close to indistinguishable
 * (a 50 ms pulse can mean "next" or "confirm"), and fine vibration timing is hard to
 * tell apart on many phones. A pitch contour is not. The rules the set follows:
 *
 *  - DIRECTION is contour. Rising = forward / up / faster / good; falling = back /
 *    down / slower / bad. Learnable in one try, and the same idea everywhere.
 *  - Frequent cues (next, previous) are quiet and short; rare, important ones
 *    (correct, error, listening) are longer and fuller.
 *  - The quiz options are counted beeps, A = 1 ... D = 4, mirroring their haptics.
 *  - Nothing is below [MIN_AUDIBLE_HZ]: phone speakers cannot reproduce low notes, so a
 *    "low, heavy" cue is made from a slide and a longer duration instead of a lower pitch.
 *
 * Deliberately no speech, no files: tones are synthesized in code, so there are no
 * audio assets to ship and nothing that can be missing at runtime.
 */

/** One step of a cue: a tone at [freqHz] for [ms], or silence when [freqHz] is 0. */
data class CueNote(val freqHz: Int, val ms: Int)

/** A cue: its notes in order, and how loud it is relative to a normal cue ([gain], 0..1). */
data class CueSpec(val notes: List<CueNote>, val gain: Float = 1f)

object SoundCues {

    /** Below this, typical phone speakers lose the note. Guarded by a unit test. */
    const val MIN_AUDIBLE_HZ = 300
    const val MAX_HZ = 3000

    /** Cues must not outlast a quick sequence of gestures. Guarded by a unit test. */
    const val MAX_CUE_MS = 500

    // Pitches (equal temperament, rounded to whole Hz).
    private const val E4 = 330
    private const val F4 = 349
    private const val FS4 = 370
    private const val G4 = 392
    private const val A4 = 440
    private const val CS5 = 554
    private const val C5 = 523
    private const val D5 = 587
    private const val E5 = 659
    private const val G5 = 784
    private const val A5 = 880
    private const val B5 = 988
    private const val C6 = 1047
    private const val E6 = 1319
    private const val DS4 = 311

    private const val REST = 0

    private fun note(hz: Int, ms: Int) = CueNote(hz, ms)
    private fun rest(ms: Int) = CueNote(REST, ms)

    /** n identical beeps with gaps — the counted family used for quiz options. */
    private fun beeps(n: Int, hz: Int = D5, beepMs: Int = 70, gapMs: Int = 50): List<CueNote> =
        buildList {
            repeat(n) { i ->
                if (i > 0) add(rest(gapMs))
                add(note(hz, beepMs))
            }
        }

    val specs: Map<HapticPattern, CueSpec> = mapOf(
        // --- Moving around: quiet, short, and told apart by direction.
        HapticPattern.NEXT to CueSpec(listOf(note(C5, 40), note(E5, 40)), gain = 0.75f),
        HapticPattern.PREVIOUS to CueSpec(listOf(note(E5, 40), note(C5, 40)), gain = 0.75f),
        HapticPattern.SECTION_CHANGE to CueSpec(listOf(note(G5, 60), note(D5, 60), note(G5, 130))),

        // --- Acting: a single decisive note, or a small "click, ding".
        HapticPattern.CONFIRM to CueSpec(listOf(note(A5, 55))),
        HapticPattern.SELECT to CueSpec(listOf(note(E5, 40), note(A5, 110))),
        // Back is falling and in a lower register than PREVIOUS, and slower.
        HapticPattern.BACK to CueSpec(listOf(note(A4, 70), rest(30), note(E4, 90))),

        // --- Results.
        HapticPattern.CORRECT to CueSpec(listOf(note(C5, 70), note(E5, 70), note(G5, 70), note(C6, 150))),
        // A slide down a semitone: reads as "wrong" without needing a low note.
        HapticPattern.INCORRECT to CueSpec(listOf(note(FS4, 100), note(F4, 200))),
        HapticPattern.ERROR to CueSpec(listOf(note(DS4, 110), rest(70), note(DS4, 110))),
        // Bumping the end of a list: one short, dull note.
        HapticPattern.EDGE to CueSpec(listOf(note(E4, 45)), gain = 0.9f),

        // --- Speech speed: three quick notes, up or down.
        HapticPattern.SPEED_UP to CueSpec(listOf(note(A4, 30), note(CS5, 30), note(E5, 50))),
        HapticPattern.SPEED_DOWN to CueSpec(listOf(note(E5, 50), note(CS5, 30), note(A4, 30))),

        // --- Chat state.
        // The microphone opens only after "Listening." has been spoken (see ChatScreen),
        // so this tone is over long before recording starts and cannot leak into it.
        HapticPattern.LISTENING to CueSpec(listOf(note(B5, 140))),
        HapticPattern.HOLD_ENGAGED to CueSpec(
            listOf(note(E6, 15), rest(30), note(E6, 15), rest(30), note(E6, 15)),
            gain = 0.8f
        ),
        // Repeats every couple of seconds while the model works: must be barely there.
        HapticPattern.THINKING to CueSpec(listOf(note(G4, 30)), gain = 0.3f),

        // --- Quiz options: counted beeps, A = 1 ... D = 4.
        HapticPattern.OPTION_A to CueSpec(beeps(1)),
        HapticPattern.OPTION_B to CueSpec(beeps(2)),
        HapticPattern.OPTION_C to CueSpec(beeps(3)),
        HapticPattern.OPTION_D to CueSpec(beeps(4))
    )
}

/**
 * Turns a [CueSpec] into raw 16-bit mono PCM. Pure maths — no Android — so it is unit
 * tested on the JVM, and the phone only has to hand the samples to an AudioTrack.
 */
object ToneSynth {

    const val SAMPLE_RATE = 22_050

    /**
     * Peak level as a fraction of full scale. Cues sit underneath speech instead of
     * competing with it, and the phone's volume keys still scale everything.
     */
    const val MASTER_GAIN = 0.4f

    /** Fade in/out on every tone. Without it each note start/end is an audible click. */
    private const val RAMP_MS = 5

    fun samplesFor(ms: Int): Int = ms * SAMPLE_RATE / 1000

    fun durationMs(spec: CueSpec): Int = spec.notes.sumOf { it.ms }

    fun render(spec: CueSpec): ShortArray {
        val out = ShortArray(spec.notes.sumOf { samplesFor(it.ms) })
        val peak = Short.MAX_VALUE * MASTER_GAIN * spec.gain
        var pos = 0
        for (note in spec.notes) {
            val n = samplesFor(note.ms)
            if (note.freqHz > 0) {
                val ramp = minOf(samplesFor(RAMP_MS), n / 2)
                val step = 2.0 * PI * note.freqHz / SAMPLE_RATE
                for (i in 0 until n) {
                    val envelope = when {
                        ramp == 0 -> 1.0
                        i < ramp -> i.toDouble() / ramp
                        i >= n - ramp -> (n - 1 - i).toDouble() / ramp
                        else -> 1.0
                    }
                    out[pos + i] = (sin(step * i) * envelope * peak).toInt().toShort()
                }
            }
            pos += n
        }
        return out
    }
}
