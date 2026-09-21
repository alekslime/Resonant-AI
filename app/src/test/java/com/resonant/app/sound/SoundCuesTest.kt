package com.resonant.app.sound

import com.resonant.app.haptics.HapticPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SoundCuesTest {

    private val specs = SoundCues.specs

    // ---- the vocabulary

    @Test
    fun every_haptic_pattern_has_a_sound_cue() {
        HapticPattern.values().forEach { assertNotNull("$it has no sound cue", specs[it]) }
    }

    @Test
    fun no_two_patterns_share_a_cue() {
        // The point of sound cues: patterns that buzz alike (NEXT / CONFIRM, BACK / PREVIOUS)
        // must still sound different. Guards against the set drifting back to duplicates.
        val list = specs.entries.toList()
        for (i in list.indices) for (j in i + 1 until list.size) {
            assertFalse(
                "${list[i].key} and ${list[j].key} sound identical",
                list[i].value.notes == list[j].value.notes
            )
        }
    }

    @Test
    fun pitches_stay_in_the_range_a_phone_speaker_can_play() {
        specs.forEach { (pattern, spec) ->
            spec.notes.filter { it.freqHz != 0 }.forEach {
                assertTrue("$pattern has ${it.freqHz} Hz, too low", it.freqHz >= SoundCues.MIN_AUDIBLE_HZ)
                assertTrue("$pattern has ${it.freqHz} Hz, too high", it.freqHz <= SoundCues.MAX_HZ)
            }
        }
    }

    @Test
    fun cues_are_short_enough_to_keep_up_with_quick_gestures() {
        specs.forEach { (pattern, spec) ->
            val ms = ToneSynth.durationMs(spec)
            assertTrue("$pattern lasts $ms ms", ms <= SoundCues.MAX_CUE_MS)
        }
    }

    @Test
    fun every_cue_has_at_least_one_tone_and_sane_durations() {
        specs.forEach { (pattern, spec) ->
            assertTrue("$pattern has no tone", spec.notes.any { it.freqHz != 0 })
            spec.notes.forEach { assertTrue("$pattern has a note of ${it.ms} ms", it.ms > 0) }
            assertTrue("$pattern gain out of range", spec.gain > 0f && spec.gain <= 1f)
        }
    }

    @Test
    fun a_cue_never_starts_or_ends_on_silence() {
        // A leading or trailing rest would add dead air before the cue is heard.
        specs.forEach { (pattern, spec) ->
            assertTrue("$pattern starts with a rest", spec.notes.first().freqHz != 0)
            assertTrue("$pattern ends with a rest", spec.notes.last().freqHz != 0)
        }
    }

    @Test
    fun direction_is_contour_rising_is_forward_falling_is_back() {
        fun pitches(p: HapticPattern) = specs.getValue(p).notes.filter { it.freqHz != 0 }.map { it.freqHz }
        assertTrue(pitches(HapticPattern.NEXT).zipWithNext().all { (a, b) -> b > a })
        assertTrue(pitches(HapticPattern.PREVIOUS).zipWithNext().all { (a, b) -> b < a })
        assertTrue(pitches(HapticPattern.SPEED_UP).zipWithNext().all { (a, b) -> b > a })
        assertTrue(pitches(HapticPattern.SPEED_DOWN).zipWithNext().all { (a, b) -> b < a })
        assertTrue(pitches(HapticPattern.CORRECT).zipWithNext().all { (a, b) -> b > a })
        assertTrue(pitches(HapticPattern.INCORRECT).zipWithNext().all { (a, b) -> b < a })
        assertTrue(pitches(HapticPattern.BACK).zipWithNext().all { (a, b) -> b < a })
    }

    @Test
    fun quiz_options_are_counted_beeps() {
        listOf(
            HapticPattern.OPTION_A, HapticPattern.OPTION_B,
            HapticPattern.OPTION_C, HapticPattern.OPTION_D
        ).forEachIndexed { i, pattern ->
            val tones = specs.getValue(pattern).notes.count { it.freqHz != 0 }
            assertEquals("$pattern beep count", i + 1, tones)
        }
    }

    @Test
    fun the_repeating_thinking_tick_is_much_quieter_than_the_rest() {
        val thinking = specs.getValue(HapticPattern.THINKING).gain
        assertTrue(thinking <= 0.35f)
        specs.filterKeys { it != HapticPattern.THINKING }.forEach { (pattern, spec) ->
            assertTrue("$pattern is not louder than THINKING", spec.gain > thinking)
        }
    }

    // ---- the synthesizer

    private fun single(hz: Int, ms: Int, gain: Float = 1f) = CueSpec(listOf(CueNote(hz, ms)), gain)

    @Test
    fun rendered_length_matches_the_notes() {
        specs.forEach { (pattern, spec) ->
            val expected = spec.notes.sumOf { ToneSynth.samplesFor(it.ms) }
            assertEquals("$pattern length", expected, ToneSynth.render(spec).size)
        }
    }

    @Test
    fun a_tone_has_the_pitch_it_claims() {
        // A sine at f Hz crosses zero 2f times a second. Count them over 200 ms.
        for (hz in listOf(440, 659, 988)) {
            val pcm = ToneSynth.render(single(hz, 200))
            var crossings = 0
            for (i in 1 until pcm.size) {
                if ((pcm[i - 1] < 0) != (pcm[i] < 0)) crossings++
            }
            val measured = crossings / 2.0 / 0.2
            assertTrue("$hz Hz measured as $measured Hz", abs(measured - hz) <= hz * 0.03)
        }
    }

    @Test
    fun the_peak_level_follows_master_and_cue_gain_and_never_clips() {
        val loud = ToneSynth.render(single(659, 100, gain = 1f)).maxOf { abs(it.toInt()) }
        val quiet = ToneSynth.render(single(659, 100, gain = 0.5f)).maxOf { abs(it.toInt()) }
        val ceiling = Short.MAX_VALUE * ToneSynth.MASTER_GAIN
        assertTrue("peak $loud above ${ceiling}", loud <= ceiling + 1)
        assertTrue("peak $loud too far below ${ceiling}", loud >= ceiling * 0.95f)
        assertTrue("half gain should be about half: $quiet vs $loud", abs(quiet - loud / 2) <= loud * 0.03)
        specs.forEach { (pattern, spec) ->
            val peak = ToneSynth.render(spec).maxOf { abs(it.toInt()) }
            assertTrue("$pattern clips", peak < Short.MAX_VALUE)
        }
    }

    @Test
    fun every_tone_fades_in_and_out_so_it_cannot_click() {
        specs.forEach { (pattern, spec) ->
            val pcm = ToneSynth.render(spec)
            assertEquals("$pattern first sample", 0, pcm.first().toInt())
            assertEquals("$pattern last sample", 0, pcm.last().toInt())
        }
        // ...and the fade is a ramp, not a cut: the first few samples are still small.
        val pcm = ToneSynth.render(single(659, 100))
        val peak = pcm.maxOf { abs(it.toInt()) }
        assertTrue(abs(pcm[3].toInt()) < peak / 2)
    }

    @Test
    fun rests_are_true_silence() {
        val spec = CueSpec(listOf(CueNote(659, 50), CueNote(0, 50), CueNote(659, 50)))
        val pcm = ToneSynth.render(spec)
        val restStart = ToneSynth.samplesFor(50)
        val restEnd = restStart + ToneSynth.samplesFor(50)
        for (i in restStart until restEnd) assertEquals("sample $i", 0, pcm[i].toInt())
        assertTrue(pcm.take(restStart).any { it.toInt() != 0 })
        assertTrue(pcm.drop(restEnd).any { it.toInt() != 0 })
    }

    @Test
    fun a_very_short_tone_still_renders_without_error() {
        // 1 ms is shorter than the fade; the fade must shrink to fit rather than overrun.
        val pcm = ToneSynth.render(single(1319, 1))
        assertEquals(ToneSynth.samplesFor(1), pcm.size)
    }
}
