package com.resonant.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RewindPlannerTest {

    // 1000 frames per second keeps the numbers readable.
    private fun seg(seconds: Float) = RewindPlanner.Segment(1000, (seconds * 1000).toInt())

    @Test
    fun rewinding_inside_the_current_unit_stays_in_it() {
        val at = RewindPlanner.locate(10f, listOf(seg(30f)))!!
        assertEquals(0, at.segment)
        assertEquals(20_000, at.frame)
    }

    @Test
    fun rewinding_past_the_start_of_a_unit_goes_into_the_one_before() {
        // 20 s of the previous unit were heard, then 4 s of the current one.
        val at = RewindPlanner.locate(10f, listOf(seg(20f), seg(4f)))!!
        assertEquals(0, at.segment)
        assertEquals(14_000, at.frame)
    }

    @Test
    fun rewinding_can_cross_several_units() {
        val at = RewindPlanner.locate(15f, listOf(seg(8f), seg(3f), seg(2f)))!!
        assertEquals(0, at.segment)
        assertEquals(1_000, at.frame)
    }

    @Test
    fun rewinding_further_than_was_heard_goes_to_the_very_start() {
        val at = RewindPlanner.locate(60f, listOf(seg(8f), seg(3f)))!!
        assertEquals(0, at.segment)
        assertEquals(0, at.frame)
    }

    @Test
    fun nothing_heard_means_nothing_to_rewind() {
        assertNull(RewindPlanner.locate(10f, emptyList()))
        assertNull(RewindPlanner.locate(10f, listOf(seg(0f))))
    }

    @Test
    fun zero_seconds_does_nothing() {
        assertNull(RewindPlanner.locate(0f, listOf(seg(5f))))
    }

    @Test
    fun a_unit_with_nothing_heard_is_skipped() {
        val at = RewindPlanner.locate(2f, listOf(seg(6f), seg(0f)))
        assertNotNull(at)
        assertEquals(0, at!!.segment)
        assertEquals(4_000, at.frame)
    }
}
