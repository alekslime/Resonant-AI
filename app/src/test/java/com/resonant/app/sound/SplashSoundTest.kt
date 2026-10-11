package com.resonant.app.sound

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SplashSoundTest {

    @Test
    fun every_tone_is_in_the_range_a_phone_speaker_can_play() {
        SplashSound.tones.forEach {
            assertTrue("${it.hz} Hz too low", it.hz >= SoundCues.MIN_AUDIBLE_HZ && it.hzEnd >= SoundCues.MIN_AUDIBLE_HZ)
            assertTrue("${it.hz} Hz too high", it.hz <= SoundCues.MAX_HZ && it.hzEnd <= SoundCues.MAX_HZ)
        }
    }

    @Test
    fun four_notes_rise_one_per_dot_landing() {
        val notes = SplashSound.tones.filter { it.gain == 0.10f }
        assertEquals(4, notes.size)
        assertEquals(notes.map { it.hz }.sorted(), notes.map { it.hz })
        assertEquals(notes.map { it.startMs }.sorted(), notes.map { it.startMs })
    }

    @Test
    fun render_is_audible_never_clips_and_has_the_expected_length() {
        val pcm = SplashSound.render()
        val peak = pcm.maxOf { abs(it.toInt()) }
        assertTrue("silent", peak > 1000)
        assertTrue("clips", peak <= Short.MAX_VALUE * SplashSound.PEAK + 2)
        assertEquals(ToneSynth.samplesFor(SplashSound.durationMs()) + 1, pcm.size)
    }

    @Test
    fun splash_sound_is_the_length_of_the_animation() {
        assertTrue(SplashSound.durationMs() in 2200..3300)
    }
}
