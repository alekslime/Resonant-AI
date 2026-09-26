package com.resonant.app.ui.theme

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
val MetropolisBlack: FontFamily = FontFamily(Font(R.font.metropolis_black, FontWeight.Black))

@Deprecated("Replaced by MetropolisBlack — retained until font file is confirmed on device.")
val AghartiBlack: FontFamily = FontFamily(Font(R.font.agharti_black, FontWeight.Black))

// Retired — was a placeholder for GC Sublime which is no longer in the design.
val GcSublimeRegular: FontFamily = MetropolisBlack
