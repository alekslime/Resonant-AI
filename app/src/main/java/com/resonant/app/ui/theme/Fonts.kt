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
val AghartiBlack: FontFamily = FontFamily(Font(R.font.agharti_black, FontWeight.Black))

// val GcSublimeRegular: FontFamily = FontFamily(Font(R.font.gc_sublime_regular, FontWeight.Normal))
val GcSublimeRegular: FontFamily = FontFamily.Default
