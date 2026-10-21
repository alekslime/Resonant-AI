package com.resonant.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class TextScaleTest {

    @Test
    fun scale_stays_between_three_quarters_and_two_and_a_half() {
        assertEquals(0.75f, TextScale.apply(1f, 0.5f), 0f)
        assertEquals(0.75f, TextScale.apply(0.75f, 0.1f), 0f)
        assertEquals(2.5f, TextScale.apply(2f, 3f), 0f)
        assertEquals(1.5f, TextScale.apply(1f, 1.5f), 0f)
    }

    @Test
    fun a_saved_size_outside_the_range_is_pulled_back_in() {
        assertEquals(0.75f, TextScale.clamp(0.2f), 0f)
        assertEquals(2.5f, TextScale.clamp(9f), 0f)
        assertEquals(1.25f, TextScale.clamp(1.25f), 0f)
    }

    @Test
    fun steps_change_every_quarter() {
        assertEquals(TextScale.stepOf(1.0f), TextScale.stepOf(1.2f))
        assertEquals(TextScale.stepOf(1.0f) + 1, TextScale.stepOf(1.25f))
    }

    @Test
    fun percent_is_rounded() {
        assertEquals(75, TextScale.percent(0.75f))
        assertEquals(150, TextScale.percent(1.5f))
        assertEquals(250, TextScale.percent(2.5f))
    }

    // ---- pinch ----

    @Test
    fun spreading_inside_a_step_changes_the_size_without_a_tick() {
        val outcome = TextScale.pinch(1.0f, 1.1f)
        assertEquals(1.1f, outcome.scale, 0.0001f)
        assertEquals(SizeCue.NONE, outcome.cue)
    }

    @Test
    fun crossing_a_quarter_step_up_ticks_larger_and_down_ticks_smaller() {
        assertEquals(SizeCue.LARGER, TextScale.pinch(1.2f, 1.1f).cue)
        assertEquals(SizeCue.SMALLER, TextScale.pinch(1.3f, 0.9f).cue)
    }

    @Test
    fun reaching_a_limit_bumps() {
        val top = TextScale.pinch(2.4f, 2f)
        assertEquals(2.5f, top.scale, 0f)
        assertEquals(SizeCue.EDGE, top.cue)
        val bottom = TextScale.pinch(0.8f, 0.5f)
        assertEquals(0.75f, bottom.scale, 0f)
        assertEquals(SizeCue.EDGE, bottom.cue)
    }

    @Test
    fun pinching_further_at_a_limit_does_nothing() {
        val top = TextScale.pinch(2.5f, 1.5f)
        assertEquals(2.5f, top.scale, 0f)
        assertEquals(SizeCue.NONE, top.cue)
        assertEquals(SizeCue.NONE, TextScale.pinch(0.75f, 0.5f).cue)
    }

    // ---- presets (Settings row, setup step) ----

    @Test
    fun presets_run_from_smallest_to_largest_and_match_the_pinch_range() {
        assertEquals(TextScale.MIN, TextScale.PRESETS.first(), 0f)
        assertEquals(TextScale.MAX, TextScale.PRESETS.last(), 0f)
        assertEquals(TextScale.PRESETS.sorted(), TextScale.PRESETS)
    }

    @Test
    fun a_pinched_size_lands_on_the_nearest_preset() {
        assertEquals(1, TextScale.presetIndexOf(1.0f))
        assertEquals(2, TextScale.presetIndexOf(1.2f))
        assertEquals(4, TextScale.presetIndexOf(1.9f))
        assertEquals(0, TextScale.presetIndexOf(0.75f))
    }

    @Test
    fun a_tap_goes_to_the_next_size_up_and_wraps_after_the_largest() {
        assertEquals(1.25f, TextScale.nextPreset(1f), 0f)
        assertEquals(1.25f, TextScale.nextPreset(1.2f), 0f)   // off a preset: the next one above, not one skipped
        assertEquals(2.5f, TextScale.nextPreset(2f), 0f)
        assertEquals(0.75f, TextScale.nextPreset(2.5f), 0f)
    }

    @Test
    fun preset_labels_are_spoken_as_percentages() {
        assertEquals("75 percent", TextScale.presetLabel(0))
        assertEquals("100 percent", TextScale.presetLabel(1))
        assertEquals("250 percent", TextScale.presetLabel(5))
    }
}
