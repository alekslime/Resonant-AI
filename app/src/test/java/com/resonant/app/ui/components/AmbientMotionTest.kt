package com.resonant.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientMotionTest {

    @Test
    fun a_patch_ends_each_lap_exactly_where_it_began_so_the_loop_never_jumps() {
        val start = AmbientMotion.position(0f, 1, 2, 0.8f, 0f)
        val end = AmbientMotion.position(AmbientMotion.lapShare(AmbientMotion.PERIOD_A_S, AmbientMotion.PERIOD_A_S), 1, 2, 0.8f, 0f)
        assertEquals(start.x, end.x, 0.0001f)
        assertEquals(start.y, end.y, 0.0001f)
    }

    @Test
    fun patches_stay_on_the_screen() {
        for (i in 0..200) {
            val p = AmbientMotion.position(i / 200f, 1, 2, 0.8f, 0f)
            assertTrue(p.x in 0.1f..0.9f && p.y in 0.1f..0.9f)
        }
    }

    @Test
    fun the_drift_is_very_slow_and_very_faint() {
        // Over one refresh the centre moves far less than a pixel on any phone.
        val a = AmbientMotion.position(0f, 1, 2, 0.8f, 0f)
        val b = AmbientMotion.position(AmbientMotion.TICK_MS / 1000f / AmbientMotion.PERIOD_A_S, 1, 2, 0.8f, 0f)
        assertTrue(kotlin.math.abs(a.x - b.x) < 0.005f && kotlin.math.abs(a.y - b.y) < 0.005f)
        assertTrue(AmbientMotion.PERIOD_A_S >= 120f && AmbientMotion.PERIOD_B_S >= 120f)
        assertTrue(AmbientMotion.PATCH_ALPHA * 2 <= 0.2f)
    }
}
