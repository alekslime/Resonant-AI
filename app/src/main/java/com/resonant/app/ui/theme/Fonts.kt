package com.resonant.app.ui.theme

import androidx.compose.ui.text.font.FontFamily

/**
 * The two brand display fonts from the Figma redesign:
 *   - Agharti (Black) — the "RESONANT" wordmark, on Splash and Home.
 *   - GC Sublime (Regular) — the Home menu items and the chat-input placeholder.
 *
 * Neither ships with Android, so the actual font files have to be dropped
 * into this project at these exact paths for the block below to compile:
 *
 *   app/src/main/res/font/agharti_black.ttf        (or .otf)
 *   app/src/main/res/font/gc_sublime_regular.ttf   (or .otf)
 *
 * (Android resource filenames must be lowercase letters/digits/underscores —
 * rename the source files to exactly this if they arrive named differently.)
 *
 * Until those two files exist, `R.font.agharti_black` / `R.font.gc_sublime_regular`
 * don't exist either (that's what "Unresolved reference: font" means), so both
 * families fall back to the system default here to keep the project building.
 * Once the files are in place, delete the two `= FontFamily.Default` lines
 * below and uncomment the real ones — nothing else in the app needs to change.
 */

// import androidx.compose.ui.text.font.Font
// import androidx.compose.ui.text.font.FontWeight
// import com.resonant.app.R
// val AghartiBlack: FontFamily = FontFamily(Font(R.font.agharti_black, FontWeight.Black))
// val GcSublimeRegular: FontFamily = FontFamily(Font(R.font.gc_sublime_regular, FontWeight.Normal))

val AghartiBlack: FontFamily = FontFamily.Default
val GcSublimeRegular: FontFamily = FontFamily.Default
