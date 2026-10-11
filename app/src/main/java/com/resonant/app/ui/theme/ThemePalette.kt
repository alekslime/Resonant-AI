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
 *  - [correct] / [onCorrect] and [incorrect] / [onIncorrect]: the screen after a quiz answer, and
 *    the text on it. The two fills differ a lot in brightness, not just hue, and the result screen
 *    also shows a check or cross and the word, so it never depends on telling colors apart.
 *  - [cardOutline] / [controlOutline]: an outline around cards, or around bars and buttons.
 *    Null means no outline. [outlineWidth] is the thickness of both.
 *  - [cardRadius] / [controlRadius]: how round cards and buttons are. 50.dp on a button is a
 *    full pill; small values give a squarer, crisper look. Part of what makes a theme feel
 *    like its own place and not only a color swap.
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
    val correct: Color,
    val onCorrect: Color,
    val incorrect: Color,
    val onIncorrect: Color,
    val cardOutline: Color?,
    val controlOutline: Color?,
    val outlineWidth: Dp,
    val cardRadius: Dp = 28.dp,
    val controlRadius: Dp = 50.dp
)

object Palettes {
    val DEFAULT = ThemePalette(
        id = "default", label = "Resonant native",
        background = Color(0xFFFFAE00), card = Color(0xFFFFFFFF), ink = Color(0xFF0A0A0A), onInk = Color(0xFFFFFFFF), dim = Color(0xFFFFFFFF),
        bar = Color(0xFF0A0A0A), onBar = Color(0xFFFFFFFF), button = Color(0xFF0A0A0A), onButton = Color(0xFFFFFFFF),
        correct = Color(0xFF8CCBFF), onCorrect = Color(0xFF000000), incorrect = Color(0xFF7A2900), onIncorrect = Color(0xFFFFFFFF),
        cardOutline = null, controlOutline = null, outlineWidth = 0.dp
    )

    val LOW_VISION = ThemePalette(
        id = "low_vision", label = "Low vision, enormous contrast",
        background = Color(0xFF090A0C), card = Color(0xFF191714), ink = Color(0xFFFFE100), onInk = Color(0xFF090A0C), dim = Color(0xFFD4BE00),
        bar = Color(0xFF191714), onBar = Color(0xFFFFFFFF), button = Color(0xFF000000), onButton = Color(0xFFFFFFFF),
        correct = Color(0xFFFFE100), onCorrect = Color(0xFF090A0C), incorrect = Color(0xFF5A1A00), onIncorrect = Color(0xFFFFFFFF),
        cardOutline = Color(0xFFFFE100), controlOutline = Color(0xFFFFE100), outlineWidth = 3.dp,
        cardRadius = 12.dp, controlRadius = 14.dp
    )

    val CATARACTS = ThemePalette(
        id = "cataracts", label = "Cataracts, high clarity",
        background = Color(0xFF22252C), card = Color(0xFF22211D), ink = Color(0xFFF8F7F6), onInk = Color(0xFF22211D), dim = Color(0xFFC5C8CF),
        bar = Color(0xFF484745), onBar = Color(0xFFFFFFFF), button = Color(0xFF16140E), onButton = Color(0xFFFFFFFF),
        correct = Color(0xFF6FB8F0), onCorrect = Color(0xFF0B1620), incorrect = Color(0xFF6B2B00), onIncorrect = Color(0xFFFFFFFF),
        cardOutline = Color(0xFFFFE100), controlOutline = null, outlineWidth = 4.dp,
        cardRadius = 20.dp, controlRadius = 20.dp
    )

    val MONOCHROME = ThemePalette(
        id = "monochrome", label = "Monochrome, colorblind",
        background = Color(0xFFFFFFFF), card = Color(0xFFE5E5E5), ink = Color(0xFF000000), onInk = Color(0xFFFFFFFF), dim = Color(0xFF4D4D4D),
        bar = Color(0xFF000000), onBar = Color(0xFFFFFFFF), button = Color(0xFF000000), onButton = Color(0xFFFFFFFF),
        correct = Color(0xFFD0D0D0), onCorrect = Color(0xFF000000), incorrect = Color(0xFF000000), onIncorrect = Color(0xFFFFFFFF),
        cardOutline = Color(0xFF000000), controlOutline = null, outlineWidth = 2.dp,
        cardRadius = 4.dp, controlRadius = 4.dp
    )

    val HANDSFREE = ThemePalette(
        id = "handsfree", label = "Handsfree, screen reader",
        background = Color(0xFF000000), card = Color(0xFF000000), ink = Color(0xFFFFFFFF), onInk = Color(0xFF000000), dim = Color(0xFFB3B3B3),
        bar = Color(0xFF000000), onBar = Color(0xFFFFFFFF), button = Color(0xFF000000), onButton = Color(0xFFFFFFFF),
        correct = Color(0xFFFFFFFF), onCorrect = Color(0xFF000000), incorrect = Color(0xFF4D4D4D), onIncorrect = Color(0xFFFFFFFF),
        cardOutline = Color(0xFFFFFFFF), controlOutline = Color(0xFFFFFFFF), outlineWidth = 3.dp,
        cardRadius = 8.dp, controlRadius = 8.dp
    )

    val PHOTOPHOBIA = ThemePalette(
        id = "photophobia", label = "Photophobia",
        background = Color(0xFF2C2922), card = Color(0xFF22211D), ink = Color(0xFFFFF8E9), onInk = Color(0xFF22211D), dim = Color(0xFFC9C0AA),
        bar = Color(0xFF15130F), onBar = Color(0xFFFFF8E9), button = Color(0xFF16140E), onButton = Color(0xFFD3B86D),
        correct = Color(0xFF3F5666), onCorrect = Color(0xFFFFF8E9), incorrect = Color(0xFF2A1006), onIncorrect = Color(0xFFFFF8E9),
        cardOutline = null, controlOutline = null, outlineWidth = 0.dp,
        cardRadius = 36.dp, controlRadius = 50.dp
    )

    val WARM = ThemePalette(
        id = "warm_ambiance", label = "Warm ambiance",
        background = Color(0xFFE6D9BA), card = Color(0xFFFFF8E9), ink = Color(0xFF000000), onInk = Color(0xFFFFF8E9), dim = Color(0xFF3D3D3D),
        bar = Color(0xFFCEBC99), onBar = Color(0xFF000000), button = Color(0xFF000000), onButton = Color(0xFFFFF8E9),
        correct = Color(0xFFBFE0F5), onCorrect = Color(0xFF000000), incorrect = Color(0xFF6B2400), onIncorrect = Color(0xFFFFF8E9),
        cardOutline = null, controlOutline = null, outlineWidth = 0.dp,
        cardRadius = 24.dp, controlRadius = 50.dp
    )

    val NIGHT_RED = ThemePalette(
        id = "night_red", label = "Night red",
        background = Color(0xFF000000), card = Color(0xFF140000), ink = Color(0xFFFF7878), onInk = Color(0xFF000000), dim = Color(0xFFFA7474),
        bar = Color(0xFF140000), onBar = Color(0xFFFF7878), button = Color(0xFF140000), onButton = Color(0xFFFF7878),
        correct = Color(0xFFFF7878), onCorrect = Color(0xFF000000), incorrect = Color(0xFF140000), onIncorrect = Color(0xFFFF9494),
        cardOutline = null, controlOutline = null, outlineWidth = 0.dp
    )

    val BLUE_ON_YELLOW = ThemePalette(
        id = "blue_on_yellow", label = "Blue on yellow",
        background = Color(0xFFFFEB3B), card = Color(0xFFFFF8C4), ink = Color(0xFF0A1B6B), onInk = Color(0xFFFFF8C4), dim = Color(0xFF23358F),
        bar = Color(0xFF0A1B6B), onBar = Color(0xFFFFEB3B), button = Color(0xFF0A1B6B), onButton = Color(0xFFFFF8C4),
        correct = Color(0xFF0A1B6B), onCorrect = Color(0xFFFFEB3B), incorrect = Color(0xFFFFFFFF), onIncorrect = Color(0xFF0A1B6B),
        cardOutline = null, controlOutline = null, outlineWidth = 0.dp
    )

    val SOFT_CREAM = ThemePalette(
        id = "soft_cream", label = "Soft cream",
        background = Color(0xFFF3ECD6), card = Color(0xFFFFFDF5), ink = Color(0xFF1B2A49), onInk = Color(0xFFFFFDF5), dim = Color(0xFF3A4A6B),
        bar = Color(0xFF1B2A49), onBar = Color(0xFFF3ECD6), button = Color(0xFF1B2A49), onButton = Color(0xFFFFFDF5),
        correct = Color(0xFFBFD9F2), onCorrect = Color(0xFF1B2A49), incorrect = Color(0xFF6B2400), onIncorrect = Color(0xFFFFFDF5),
        cardOutline = null, controlOutline = null, outlineWidth = 0.dp
    )

    val PAPER_GRAY = ThemePalette(
        id = "paper_gray", label = "Paper gray",
        background = Color(0xFFDADAD6), card = Color(0xFFF2F2EF), ink = Color(0xFF1F1F1F), onInk = Color(0xFFF2F2EF), dim = Color(0xFF3A3A3A),
        bar = Color(0xFF3A3A3A), onBar = Color(0xFFF2F2EF), button = Color(0xFF2B2B2B), onButton = Color(0xFFF2F2EF),
        correct = Color(0xFFBFE0F5), onCorrect = Color(0xFF1F1F1F), incorrect = Color(0xFF6B2400), onIncorrect = Color(0xFFF2F2EF),
        cardOutline = null, controlOutline = null, outlineWidth = 0.dp
    )

    val WHITE_ON_BLUE = ThemePalette(
        id = "white_on_blue", label = "White on blue",
        background = Color(0xFF0B2A6F), card = Color(0xFF1A44A3), ink = Color(0xFFFFFFFF), onInk = Color(0xFF0B2A6F), dim = Color(0xFFB9CEFF),
        bar = Color(0xFF061A47), onBar = Color(0xFFFFFFFF), button = Color(0xFF061A47), onButton = Color(0xFFFFFFFF),
        correct = Color(0xFFFFFFFF), onCorrect = Color(0xFF0B2A6F), incorrect = Color(0xFF4A1A00), onIncorrect = Color(0xFFFFFFFF),
        cardOutline = null, controlOutline = null, outlineWidth = 0.dp
    )

    val ALL = listOf(
        DEFAULT, LOW_VISION, CATARACTS, MONOCHROME, HANDSFREE, PHOTOPHOBIA, WARM,
        NIGHT_RED, BLUE_ON_YELLOW, SOFT_CREAM, PAPER_GRAY, WHITE_ON_BLUE
    )

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
