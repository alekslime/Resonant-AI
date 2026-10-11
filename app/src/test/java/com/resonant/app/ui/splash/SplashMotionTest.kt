package com.resonant.app.ui.splash

import com.resonant.app.sound.SplashSound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SplashMotionTest {

    private val w = 1080f
    private val h = 2340f
    private val g = SplashMotion.geometry(w, h)

    @Test
    fun dots_start_invisible() {
        for (i in 0 until 4) assertEquals(0f, SplashMotion.dotPose(i, 0f, w, h, g).radius, 0.001f)
    }

    @Test
    fun finished_mark_is_the_braille_R_at_full_size() {
        // dots 1-2-3 in the left column, dot 5 to the right of the middle one
        val p = (0 until 4).map { SplashMotion.dotPose(it, SplashMotion.FINAL_S, w, h, g) }
        p.forEach { assertEquals(SplashMotion.DOT_R * g.scale, it.radius, 0.01f) }
        assertEquals(p[0].x, p[1].x, 0.01f)
        assertEquals(p[1].x, p[2].x, 0.01f)
        assertEquals(p[1].y, p[3].y, 0.01f)
        assertTrue(p[3].x > p[1].x)
        assertTrue(p[0].y < p[1].y && p[1].y < p[2].y)
    }

    @Test
    fun the_logo_is_centered_horizontally_and_fits_the_screen() {
        val p = (0 until 4).map { SplashMotion.dotPose(it, SplashMotion.FINAL_S, w, h, g) }
        val left = p.minOf { it.x - it.radius }
        val right = p.maxOf { it.x + it.radius }
        assertEquals(w / 2f, (left + right) / 2f, 1f)
        assertTrue(left > 0f && right < w)
        assertTrue(p.minOf { it.y - it.radius } > 0f && p.maxOf { it.y + it.radius } < h)
    }

    @Test
    fun dots_land_one_after_another_in_order() {
        val t = SplashMotion.ORBIT_END + 0.2f
        val progress = (0 until 4).map { SplashMotion.dotPose(it, t, w, h, g).radius }
        for (i in 0 until 3) assertTrue("dot $i should be further along than ${i + 1}", progress[i] >= progress[i + 1])
    }

    @Test
    fun rings_start_only_when_the_R_is_formed_and_all_end_within_the_animation() {
        assertEquals(-1f, SplashMotion.ringProgress(0, SplashMotion.RINGS_AT - 0.01f), 0f)
        assertTrue(SplashMotion.ringProgress(0, SplashMotion.RINGS_AT + 0.1f) > 0f)
        val lastEnd = SplashMotion.RINGS_AT + (SplashMotion.RING_COUNT - 1) * SplashMotion.RING_GAP + SplashMotion.RING_LIFE
        assertTrue(lastEnd <= SplashMotion.ANIM_S + 0.35f)
        for (j in 0 until SplashMotion.RING_COUNT) assertEquals(-1f, SplashMotion.ringProgress(j, SplashMotion.FINAL_S), 0f)
    }

    @Test
    fun haptic_ticks_fall_inside_the_animation_and_in_order() {
        val ms = (0 until 4).map { SplashMotion.landingMs(it) }
        assertEquals(ms.sorted(), ms)
        assertTrue(ms.all { it < SplashMotion.ANIM_S * 1000 / SplashSound.SPEED })
    }

    @Test
    fun dot_motion_is_continuous_across_phase_changes() {
        for (i in 0 until 4) for (edge in listOf(SplashMotion.GATHER_END, SplashMotion.ORBIT_END)) {
            val a = SplashMotion.dotPose(i, edge - 0.001f, w, h, g)
            val b = SplashMotion.dotPose(i, edge + 0.001f, w, h, g)
            assertTrue("dot $i jumps at $edge", abs(a.x - b.x) < 20f && abs(a.y - b.y) < 20f)
        }
    }
}
