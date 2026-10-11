package com.resonant.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResumePlannerTest {

    @Test
    fun resumes_at_the_frame_where_it_stopped() {
        assertEquals(12_000, ResumePlanner.startFrame(3, 3, 12_000, 30_000))
    }

    @Test
    fun starts_again_when_the_kept_audio_is_for_another_unit() {
        assertNull(ResumePlanner.startFrame(2, 3, 12_000, 30_000))
    }

    @Test
    fun starts_again_when_nothing_was_heard() {
        assertNull(ResumePlanner.startFrame(3, 3, 0, 30_000))
    }

    @Test
    fun starts_again_when_it_had_already_reached_the_end() {
        assertNull(ResumePlanner.startFrame(3, 3, 30_000, 30_000))
    }

    @Test
    fun starts_again_when_there_is_no_audio() {
        assertNull(ResumePlanner.startFrame(-1, 0, 0, 0))
    }
}
