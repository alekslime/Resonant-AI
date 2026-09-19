package com.resonant.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression guard for the low-vision redesign pass. The old design put text
 * directly on a gradient at varying alpha/position, which measured as low as
 * ~1.3:1 in places (see git history / design notes). Every text-on-gradient
 * and chip-text pairing this app actually uses is checked here against WCAG
 * 2.1 so a future edit can't silently reintroduce a low-contrast pairing.
 *
 * These are plain Color math, not Compose-dependent, so they run as ordinary
 * JVM unit tests — no device or Robolectric needed.
 */
class ContrastTest {

    private fun luminance(c: Color): Double {
        fun channel(v: Float): Double {
            val d = v.toDouble()
            return if (d <= 0.03928) d / 12.92 else Math.pow((d + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val l1 = luminance(a)
        val l2 = luminance(b)
        val (hi, lo) = if (l1 > l2) l1 to l2 else l2 to l1
        return (hi + 0.05) / (lo + 0.05)
    }

    /** WCAG AA minimum for normal-size body text. */
    private val AA_NORMAL_TEXT = 4.5

    @Test
    fun light_theme_text_clears_AA_against_every_gradient_stop() {
        listOf(GradientLightTop, GradientLightMid, GradientLightBottom).forEach { stop ->
            assertTrue(
                "ResonantTextOnLight vs $stop was ${contrast(ResonantTextOnLight, stop)}",
                contrast(ResonantTextOnLight, stop) >= AA_NORMAL_TEXT
            )
        }
    }

    @Test
    fun dark_theme_text_clears_AA_against_every_gradient_stop() {
        listOf(GradientDarkTop, GradientDarkMid, GradientDarkBottom).forEach { stop ->
            assertTrue(
                "ResonantTextOnDark vs $stop was ${contrast(ResonantTextOnDark, stop)}",
                contrast(ResonantTextOnDark, stop) >= AA_NORMAL_TEXT
            )
        }
    }

    @Test
    fun dark_theme_accent_text_clears_AA_against_every_gradient_stop() {
        listOf(GradientDarkTop, GradientDarkMid, GradientDarkBottom).forEach { stop ->
            assertTrue(
                "ResonantOrangeOnDark vs $stop was ${contrast(ResonantOrangeOnDark, stop)}",
                contrast(ResonantOrangeOnDark, stop) >= AA_NORMAL_TEXT
            )
        }
    }

    @Test
    fun focused_chip_text_is_always_near_maximum_contrast() {
        // Solid fills, not gradient-position-dependent — should be extremely high,
        // not just barely passing.
        assertTrue(contrast(ResonantFocusedTextLight, ResonantFocusedFillLight) > 15.0)
        assertTrue(contrast(ResonantFocusedTextDark, ResonantFocusedFillDark) > 15.0)
    }

    @Test
    fun feedback_chip_text_clears_AA() {
        assertTrue(contrast(ResonantWhite, ResonantCorrectFillLight) >= AA_NORMAL_TEXT)
        assertTrue(contrast(ResonantWhite, ResonantIncorrectFillLight) >= AA_NORMAL_TEXT)
        assertTrue(contrast(ResonantBlack, ResonantCorrectFillDark) >= AA_NORMAL_TEXT)
        assertTrue(contrast(ResonantBlack, ResonantIncorrectFillDark) >= AA_NORMAL_TEXT)
    }

    @Test
    fun feedback_chips_are_visible_against_their_own_theme_background() {
        // Non-text ("graphical object") contrast — WCAG 1.4.11 recommends >= 3:1
        // so the chip's edge doesn't disappear into the page behind it.
        val NON_TEXT_MIN = 3.0
        assertTrue(contrast(ResonantCorrectFillLight, GradientLightBottom) >= NON_TEXT_MIN)
        assertTrue(contrast(ResonantIncorrectFillLight, GradientLightBottom) >= NON_TEXT_MIN)
        assertTrue(contrast(ResonantCorrectFillDark, GradientDarkBottom) >= NON_TEXT_MIN)
        assertTrue(contrast(ResonantIncorrectFillDark, GradientDarkBottom) >= NON_TEXT_MIN)
    }
}
