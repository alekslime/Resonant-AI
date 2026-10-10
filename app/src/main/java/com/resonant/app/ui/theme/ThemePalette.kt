package com.resonant.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * One complete color theme for the app. Every screen is drawn from these five colors:
 *
 *  - [background]: the screen.
 *  - [ink]: text and icons on the screen and on cards, and the fill of buttons and bars.
 *  - [card]: the fill of cards and unfocused rows. [ink] is always readable on it.
 *  - [onInk]: text and icons drawn on top of an [ink] fill.
 *  - [dim]: unfocused menu text on the screen, next to focused text in [ink].
 *
 * Contrast of each pairing is checked in ThemePaletteTest.
 */
data class ThemePalette(
    val id: String,
    val label: String,
    val background: Color,
    val ink: Color,
    val card: Color,
    val onInk: Color,
    val dim: Color
)

object Palettes {
    val DEFAULT = ThemePalette("default", "Default orange", Color(0xFFFFAE00), Color(0xFF0A0A0A), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFFFFFF))
    val YELLOW_ON_BLACK = ThemePalette("yellow_on_black", "Yellow on black", Color(0xFF000000), Color(0xFFFFFF00), Color(0xFF333333), Color(0xFF000000), Color(0xFFC8C800))
    val WHITE_ON_BLUE = ThemePalette("white_on_blue", "White on blue", Color(0xFF0B2A6F), Color(0xFFFFFFFF), Color(0xFF1A44A3), Color(0xFF0B2A6F), Color(0xFFB9CEFF))
    val WHITE_ON_BLACK = ThemePalette("white_on_black", "White on black", Color(0xFF000000), Color(0xFFFFFFFF), Color(0xFF333333), Color(0xFF000000), Color(0xFFC8C8C8))
    val BLACK_ON_WHITE = ThemePalette("black_on_white", "Black on white", Color(0xFFFFFFFF), Color(0xFF000000), Color(0xFFD0D0D0), Color(0xFFFFFFFF), Color(0xFF4D4D4D))

    val ALL = listOf(DEFAULT, YELLOW_ON_BLACK, WHITE_ON_BLUE, WHITE_ON_BLACK, BLACK_ON_WHITE)

    fun byId(id: String?): ThemePalette = ALL.firstOrNull { it.id == id } ?: DEFAULT

    fun next(current: ThemePalette): ThemePalette = ALL[(ALL.indexOf(current) + 1) % ALL.size]
}

/**
 * The theme in use. A snapshot state, so every screen that reads a theme color redraws the
 * moment this changes. Set once at startup from the saved choice, then by the Settings item.
 */
object ActivePalette {
    var current: ThemePalette by mutableStateOf(Palettes.DEFAULT)
}
