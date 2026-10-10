package com.resonant.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every theme must keep every text pairing the app draws at AAA (7:1) or better. */
class ThemePaletteTest {

    private fun luminance(c: Color): Double {
        fun channel(v: Float): Double {
            val d = v.toDouble()
            return if (d <= 0.03928) d / 12.92 else Math.pow((d + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = luminance(a).let { x -> luminance(b).let { y -> if (x > y) x to y else y to x } }
        return (hi + 0.05) / (lo + 0.05)
    }

    private val aaa = 7.0

    @Test
    fun ink_is_readable_on_the_screen_and_on_cards() {
        Palettes.ALL.filter { it.id != "default" }.forEach { p ->
            assertTrue("${p.id} ink on background", contrast(p.ink, p.background) >= aaa)
            assertTrue("${p.id} ink on card", contrast(p.ink, p.card) >= aaa)
        }
    }

    @Test
    fun text_on_an_ink_fill_is_readable() {
        Palettes.ALL.forEach { p ->
            assertTrue("${p.id} onInk on ink", contrast(p.onInk, p.ink) >= aaa)
        }
    }

    @Test
    fun unfocused_menu_text_is_readable_on_the_screen() {
        Palettes.ALL.filter { it.id != "default" }.forEach { p ->
            assertTrue("${p.id} dim on background", contrast(p.dim, p.background) >= aaa)
        }
    }

    @Test
    fun ids_are_unique_and_unknown_ids_fall_back_to_default() {
        assertEquals(Palettes.ALL.size, Palettes.ALL.map { it.id }.toSet().size)
        assertEquals(Palettes.DEFAULT, Palettes.byId("nope"))
        assertEquals(Palettes.DEFAULT, Palettes.byId(null))
    }

    @Test
    fun next_wraps_around() {
        assertEquals(Palettes.DEFAULT, Palettes.next(Palettes.ALL.last()))
    }
}
