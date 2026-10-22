package com.resonant.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTypeDefaultsTest {

    private val fontIds = setOf("metropolis", "atkinson", "lexend")

    @Test
    fun every_theme_starts_with_a_font_and_spacing_that_exist() {
        for (p in Palettes.ALL) {
            val d = ThemeTypeDefaults.forTheme(p.id)
            assertTrue("${p.id} font ${d.fontId}", d.fontId in fontIds)
            assertTrue("${p.id} spacing ${d.spacingId}", TextSpacings.ALL.any { it.id == d.spacingId })
        }
    }

    @Test
    fun the_native_theme_and_unknown_ones_keep_the_brand_type() {
        assertEquals("metropolis", ThemeTypeDefaults.forTheme(Palettes.DEFAULT.id).fontId)
        assertEquals("normal", ThemeTypeDefaults.forTheme(Palettes.DEFAULT.id).spacingId)
        assertEquals("metropolis", ThemeTypeDefaults.forTheme("nope").fontId)
        assertEquals("metropolis", ThemeTypeDefaults.forTheme(null).fontId)
    }

    @Test
    fun the_low_vision_themes_start_with_the_hyperlegible_font() {
        for (id in listOf("low_vision", "cataracts", "handsfree")) {
            assertEquals(id, "atkinson", ThemeTypeDefaults.forTheme(id).fontId)
        }
        assertEquals("extra_roomy", ThemeTypeDefaults.forTheme("cataracts").spacingId)
    }

    @Test
    fun the_calm_themes_start_with_lexend() {
        for (id in listOf("photophobia", "night_red", "warm_ambiance", "soft_cream", "paper_gray")) {
            assertEquals(id, "lexend", ThemeTypeDefaults.forTheme(id).fontId)
        }
    }
}
