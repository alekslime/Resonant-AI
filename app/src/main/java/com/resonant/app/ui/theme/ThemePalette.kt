package com.resonant.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One complete color theme for the app, taken from the design mockups. Every screen is drawn
 * from these colors:
 *
 *  - [background]: the screen.
 *  - [card]: the fill of cards and unfocused rows.
 *  - [ink]: text and icons on the screen and on cards, and the fill of the focused row.
 *  - [onInk]: text and icons drawn on an [ink] fill.
 *  - [dim]: unfocused menu text on the screen, next to focused text in [ink].
 *  - [bar] / [onBar]: the top bar pill, the settings circle and the ask pill, and what is drawn on them.
 *  - [button] / [onButton]: buttons such as "Continue lesson", and what is drawn on them.
 *  - [cardOutline] / [controlOutline]: an outline around cards, or around bars and buttons.
 *    Null means no outline. [outlineWidth] is the thickness of both.
 *
 * Contrast of each pairing is checked in ThemePaletteTest.
 */
data class ThemePalette(
    val id: String,
    val label: String,
    val background: Color,
    val card: Color,
    val ink: Color,
    val onInk: Color,
    val dim: Color,
    val bar: Color,
    val onBar: Color,
    val button: Color,
    val onButton: Color,
    val cardOutline: Color?,
    val controlOutline: Color?,
    val outlineWidth: Dp
)

object Palettes {
    val DEFAULT = ThemePalette(
        id = "default", label = "Resonant native",
        background = Color(0xFFFFAE00), card = Color(0xFFFFFFFF), ink = Color(0xFF0A0A0A), onInk = Color(0xFFFFFFFF), dim = Color(0xFFFFFFFF),
        bar = Color(0xFF0A0A0A), onBar = Color(0xFFFFFFFF), button = Color(0xFF0A0A0A), onButton = Color(0xFFFFFFFF),
        cardOutline = null, controlOutline = null, outlineWidth = 0.dp
    )

    val LOW_VISION = ThemePalette(
        id = "low_vision", label = "Low vision, enormous contrast",
        background = Color(0xFF090A0C), card = Color(0xFF191714), ink = Color(0xFFFFE100), onInk = Color(0xFF090A0C), dim = Color(0xFFD4BE00),
        bar = Color(0xFF191714), onBar = Color(0xFFFFFFFF), button = Color(0xFF000000), onButton = Color(0xFFFFFFFF),
        cardOutline = null, controlOutline = null, outlineWidth = 0.dp
    )

    val CATARACTS = ThemePalette(
        id = "cataracts", label = "Cataracts, high clarity",
        background = Color(0xFF22252C), card = Color(0xFF22211D), ink = Color(0xFFF8F7F6), onInk = Color(0xFF22211D), dim = Color(0xFFC5C8CF),
        bar = Color(0xFF484745), onBar = Color(0xFFFFFFFF), button = Color(0xFF16140E), onButton = Color(0xFFFFFFFF),
        cardOutline = Color(0xFFFFE100), controlOutline = null, outlineWidth = 4.dp
    )

    val MONOCHROME = ThemePalette(
        id = "monochrome", label = "Monochrome, colorblind",
        background = Color(0xFFFFFFFF), card = Color(0xFFE5E5E5), ink = Color(0xFF000000), onInk = Color(0xFFFFFFFF), dim = Color(0xFF4D4D4D),
        bar = Color(0xFF000000), onBar = Color(0xFFFFFFFF), button = Color(0xFF000000), onButton = Color(0xFFFFFFFF),
        cardOutline = null, controlOutline = null, outlineWidth = 0.dp
    )

    val HANDSFREE = ThemePalette(
        id = "handsfree", label = "Handsfree, screen reader",
        background = Color(0xFF000000), card = Color(0xFF000000), ink = Color(0xFFFFFFFF), onInk = Color(0xFF000000), dim = Color(0xFFB3B3B3),
        bar = Color(0xFF000000), onBar = Color(0xFFFFFFFF), button = Color(0xFF000000), onButton = Color(0xFFFFFFFF),
        cardOutline = Color(0xFFFFFFFF), controlOutline = Color(0xFFFFFFFF), outlineWidth = 3.dp
    )

    val PHOTOPHOBIA = ThemePalette(
        id = "photophobia", label = "Photophobia",
        background = Color(0xFF2C2922), card = Color(0xFF22211D), ink = Color(0xFFFFF8E9), onInk = Color(0xFF22211D), dim = Color(0xFFC9C0AA),
        bar = Color(0xFF15130F), onBar = Color(0xFFFFF8E9), button = Color(0xFF16140E), onButton = Color(0xFFD3B86D),
        cardOutline = null, controlOutline = null, outlineWidth = 0.dp
    )

    val WARM = ThemePalette(
        id = "warm_ambiance", label = "Warm ambiance",
        background = Color(0xFFE6D9BA), card = Color(0xFFFFF8E9), ink = Color(0xFF000000), onInk = Color(0xFFFFF8E9), dim = Color(0xFF3D3D3D),
        bar = Color(0xFFCEBC99), onBar = Color(0xFF000000), button = Color(0xFF000000), onButton = Color(0xFFFFF8E9),
        cardOutline = null, controlOutline = null, outlineWidth = 0.dp
    )

    val ALL = listOf(DEFAULT, LOW_VISION, CATARACTS, MONOCHROME, HANDSFREE, PHOTOPHOBIA, WARM)

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
