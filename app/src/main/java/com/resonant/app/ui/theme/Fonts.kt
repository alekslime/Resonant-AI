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
 * Neither ships with Android, so the actual font files have to be dropped
 * into this project at these exact paths before it will build:
 *
 *   app/src/main/res/font/agharti_black.ttf        (or .otf)
 *   app/src/main/res/font/gc_sublime_regular.ttf   (or .otf)
 *
 * (Android resource filenames must be lowercase letters/digits/underscores —
 * rename the source files to exactly this if they arrive named differently.)
 * Once both files exist, `R.font.agharti_black` / `R.font.gc_sublime_regular`
 * resolve automatically and everything below just works — nothing else in
 * the app needs to change.
 */
val AghartiBlack: FontFamily = FontFamily(Font(R.font.agharti_black, FontWeight.Black))

val GcSublimeRegular: FontFamily = FontFamily(Font(R.font.gc_sublime_regular, FontWeight.Normal))
