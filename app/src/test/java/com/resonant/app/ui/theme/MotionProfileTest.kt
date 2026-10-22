package com.resonant.app.ui.theme

import com.resonant.app.ui.components.SpringFeel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionProfileTest {

    @Test
    fun every_theme_has_a_profile_and_an_unknown_one_gets_the_standard() {
        for (p in Palettes.ALL) MotionProfiles.forTheme(p.id) // never throws
        assertSame(MotionProfiles.STANDARD, MotionProfiles.forTheme(Palettes.DEFAULT.id))
        assertSame(MotionProfiles.STANDARD, MotionProfiles.forTheme("nope"))
        assertSame(MotionProfiles.STANDARD, MotionProfiles.forTheme(null))
    }

    @Test
    fun light_sensitive_themes_are_quiet() {
        for (id in listOf("photophobia", "night_red")) {
            val m = MotionProfiles.forTheme(id)
            assertEquals(0f, m.pingStrength, 0f)
            assertEquals(0f, m.driftStrength, 0f)
            assertTrue(m.minDamping >= 0.9f)
        }
    }

    @Test
    fun low_vision_themes_show_focus_with_a_steady_ring_and_a_flat_background() {
        for (id in listOf("low_vision", "cataracts", "handsfree", "monochrome")) {
            val m = MotionProfiles.forTheme(id)
            assertTrue(m.focusRingDp >= 3f)
            assertEquals(0f, m.driftStrength, 0f)
            assertEquals(0f, m.pingStrength, 0f)
        }
    }

    @Test
    fun only_the_clear_profile_has_a_ring_and_the_gentle_one_is_softer_and_slower() {
        assertEquals(0f, MotionProfiles.STANDARD.focusRingDp, 0f)
        assertEquals(0f, MotionProfiles.GENTLE.focusRingDp, 0f)
        assertTrue(MotionProfiles.GENTLE.pingStrength < MotionProfiles.STANDARD.pingStrength)
        assertTrue(MotionProfiles.GENTLE.driftStrength < MotionProfiles.STANDARD.driftStrength)
        assertTrue(MotionProfiles.GENTLE.driftSlowdown > 1f)
    }

    @Test
    fun a_calm_theme_can_only_make_the_springs_calmer() {
        assertEquals(0.6f, SpringFeel.damping(0.6f, 0f), 0f)
        assertEquals(0.9f, SpringFeel.damping(0.6f, 0.9f), 0f)
        assertEquals(0.95f, SpringFeel.damping(0.95f, 0.9f), 0f)
    }
}
