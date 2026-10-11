package com.resonant.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FontAndSpacingTest {

    @Test
    fun font_ids_are_unique_and_unknown_ids_fall_back_to_metropolis() {
        assertEquals(AppFonts.ALL.size, AppFonts.ALL.map { it.id }.toSet().size)
        assertEquals(AppFonts.METROPOLIS, AppFonts.byId("nope"))
        assertEquals(AppFonts.METROPOLIS, AppFonts.byId(null))
        assertEquals(AppFonts.LEXEND, AppFonts.byId("lexend"))
    }

    @Test
    fun next_font_wraps_around() {
        assertEquals(AppFonts.METROPOLIS, AppFonts.next(AppFonts.ALL.last()))
    }

    @Test
    fun spacing_gets_looser_at_each_step() {
        TextSpacings.ALL.zipWithNext().forEach { (a, b) ->
            assertTrue(b.letterSpacingEm > a.letterSpacingEm)
            assertTrue(b.lineHeightScale > a.lineHeightScale)
        }
        assertEquals(0f, TextSpacings.NORMAL.letterSpacingEm, 0f)
        assertEquals(1f, TextSpacings.NORMAL.lineHeightScale, 0f)
    }

    @Test
    fun unknown_spacing_falls_back_to_normal_and_next_wraps() {
        assertEquals(TextSpacings.NORMAL, TextSpacings.byId("nope"))
        assertEquals(TextSpacings.NORMAL, TextSpacings.next(TextSpacings.ALL.last()))
    }
}
