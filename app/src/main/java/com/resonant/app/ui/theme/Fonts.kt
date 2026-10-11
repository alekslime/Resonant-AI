package com.resonant.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.resonant.app.R

/**
 * The two brand display fonts from the Figma redesign:
 *   - Agharti (Black) — the "RESONANT" wordmark, on Splash and Home.
 *   - GC Sublime (Regular) — the Home menu items and the chat-input placeholder.
 *
 * Agharti is in (res/font/agharti_black.ttf) and wired up below.
 *
 * GC Sublime is NOT in yet — there's no res/font/gc_sublime_regular.ttf (or
 * .otf) in the project. Until it's added, GcSublimeRegular falls back to the
 * system default, which is why the Home menu still renders in a generic
 * sans rather than the brand condensed face. Once that file is dropped in
 * at exactly that path, delete the `= FontFamily.Default` line below and
 * uncomment the real one — nothing else in the app needs to change.
 */
/**
 * Brand fonts:
 *   - Metropolis (Black) — the entire app's typeface per the redesign.
 *     File: res/font/metropolis_black.ttf
 *     Source: https://github.com/fontsource/font-files/tree/main/fonts/other/metropolis/files
 *             → metropolis-latin-900-normal.ttf, renamed to metropolis_black.ttf
 *
 *   - AghartiBlack — kept in res/font/ but no longer used. Safe to delete
 *     once Metropolis is confirmed on device.
 *
 * GC Sublime is retired — all screens now use Metropolis.
 */
/** The brand typeface. Always used for the RESONANT wordmark, and the default for everything else. */
val BrandFamily: FontFamily = FontFamily(
    Font(R.font.metropolis_black, FontWeight.Black),
    Font(R.font.metropolis_semibold, FontWeight.SemiBold)
)

/**
 * A typeface the listener can choose in Settings. Weights in between the files resolve to the
 * nearest one, so Normal and Medium take the regular file and Bold takes the semibold or black.
 */
class AppFont(val id: String, val label: String, val family: FontFamily)

object AppFonts {
    val METROPOLIS = AppFont("metropolis", "Metropolis", BrandFamily)

    /** Atkinson Hyperlegible Next, from the Braille Institute: letters made to be told apart at low vision. */
    val ATKINSON = AppFont(
        "atkinson", "Atkinson Hyperlegible",
        FontFamily(
            Font(R.font.atkinson_regular, FontWeight.Normal),
            Font(R.font.atkinson_semibold, FontWeight.SemiBold),
            Font(R.font.atkinson_extrabold, FontWeight.Black)
        )
    )

    /** Lexend: a family designed around easier reading. */
    val LEXEND = AppFont(
        "lexend", "Lexend",
        FontFamily(
            Font(R.font.lexend_regular, FontWeight.Normal),
            Font(R.font.lexend_semibold, FontWeight.SemiBold),
            Font(R.font.lexend_black, FontWeight.Black)
        )
    )

    val ALL = listOf(METROPOLIS, ATKINSON, LEXEND)

    fun byId(id: String?): AppFont = ALL.firstOrNull { it.id == id } ?: METROPOLIS

    fun next(current: AppFont): AppFont = ALL[(ALL.indexOf(current) + 1) % ALL.size]
}

/** The font in use. A snapshot state, like ActivePalette, so text redraws when it changes. */
object ActiveFont {
    var current: AppFont by mutableStateOf(AppFonts.METROPOLIS)
}

/**
 * The font every screen draws its text in. The name is from when Metropolis was the only
 * one; it now follows the choice in Settings, so each `fontFamily = MetropolisBlack` already
 * means "the chosen font".
 */
val MetropolisBlack: FontFamily get() = ActiveFont.current.family

@Deprecated("Replaced by MetropolisBlack — retained until font file is confirmed on device.")
val AghartiBlack: FontFamily = FontFamily(Font(R.font.agharti_black, FontWeight.Black))

// Retired — was a placeholder for GC Sublime which is no longer in the design.
val GcSublimeRegular: FontFamily get() = MetropolisBlack
