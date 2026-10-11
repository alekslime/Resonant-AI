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
        val x = luminance(a)
        val y = luminance(b)
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }

    private val aaa = 7.0

    @Test
    fun text_is_readable_on_the_screen_and_on_cards() {
        Palettes.ALL.forEach { p ->
            assertTrue("${p.id} text on background", contrast(p.ink, p.background) >= aaa)
            assertTrue("${p.id} text on card", contrast(p.ink, p.card) >= aaa)
        }
    }

    @Test
    fun text_on_a_focused_row_is_readable() {
        Palettes.ALL.forEach { p ->
            assertTrue("${p.id} onInk on ink", contrast(p.onInk, p.ink) >= aaa)
        }
    }

    @Test
    fun bar_and_button_text_is_readable() {
        Palettes.ALL.forEach { p ->
            assertTrue("${p.id} onBar on bar", contrast(p.onBar, p.bar) >= aaa)
            assertTrue("${p.id} onButton on button", contrast(p.onButton, p.button) >= aaa)
        }
    }

    @Test
    fun unfocused_menu_text_is_readable_on_the_screen() {
        // The default keeps its original white-on-orange menu.
        Palettes.ALL.filter { it.id != "default" }.forEach { p ->
            assertTrue("${p.id} dim on background", contrast(p.dim, p.background) >= aaa)
        }
    }

    @Test
    fun outlines_stand_out_from_the_screen() {
        Palettes.ALL.forEach { p ->
            p.cardOutline?.let { assertTrue("${p.id} card outline", contrast(it, p.background) >= 3.0) }
            p.controlOutline?.let { assertTrue("${p.id} control outline", contrast(it, p.background) >= 3.0) }
        }
    }

    @Test
    fun ids_are_unique_and_unknown_ids_fall_back_to_default() {
        assertEquals(Palettes.ALL.size, Palettes.ALL.map { it.id }.toSet().size)
        assertEquals(Palettes.DEFAULT, Palettes.byId("nope"))
        assertEquals(Palettes.DEFAULT, Palettes.byId("yellow_on_black"))
        assertEquals(Palettes.DEFAULT, Palettes.byId(null))
    }

    @Test
    fun next_wraps_around() {
        assertEquals(Palettes.DEFAULT, Palettes.next(Palettes.ALL.last()))
    }

    @Test
    fun quiz_result_text_is_readable() {
        Palettes.ALL.forEach { p ->
            assertTrue("${p.id} onCorrect", contrast(p.onCorrect, p.correct) >= aaa)
            assertTrue("${p.id} onIncorrect", contrast(p.onIncorrect, p.incorrect) >= aaa)
        }
    }

    @Test
    fun correct_and_incorrect_differ_in_brightness_not_just_color() {
        Palettes.ALL.forEach { p ->
            assertTrue("${p.id} correct vs incorrect", contrast(p.correct, p.incorrect) >= 2.0)
        }
    }
}
