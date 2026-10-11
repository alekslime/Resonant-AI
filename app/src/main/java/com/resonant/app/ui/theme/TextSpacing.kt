package com.resonant.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * How loosely reading text is set. Wider letter spacing and taller lines help some people track
 * a line of text. Applied to lesson text and the quiz question, where there is the most to read.
 */
class TextSpacing(
    val id: String,
    val label: String,
    val letterSpacingEm: Float,
    val lineHeightScale: Float
)

object TextSpacings {
    val NORMAL = TextSpacing("normal", "Normal", 0f, 1f)
    val ROOMY = TextSpacing("roomy", "Roomy", 0.04f, 1.15f)
    val EXTRA_ROOMY = TextSpacing("extra_roomy", "Extra roomy", 0.08f, 1.3f)

    val ALL = listOf(NORMAL, ROOMY, EXTRA_ROOMY)

    fun byId(id: String?): TextSpacing = ALL.firstOrNull { it.id == id } ?: NORMAL

    fun next(current: TextSpacing): TextSpacing = ALL[(ALL.indexOf(current) + 1) % ALL.size]
}

/** The spacing in use. A snapshot state, like ActivePalette. */
object ActiveSpacing {
    var current: TextSpacing by mutableStateOf(TextSpacings.NORMAL)

    val letterSpacing: TextUnit get() = current.letterSpacingEm.em

    /** A line height of [sp] scaled by the chosen spacing. */
    fun lineHeight(sp: Float): TextUnit = (sp * current.lineHeightScale).sp
}
