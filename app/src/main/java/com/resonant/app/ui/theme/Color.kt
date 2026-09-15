package com.resonant.app.ui.theme

import androidx.compose.ui.graphics.Color

// The Resonant surface: a plain top-to-bottom gradient, sampled directly from
// the supplied background asset (uniform across every row — this is a vertical
// two-stop gradient, not diagonal). Every screen sits on the same wash so the
// app reads as one continuous surface.
val ResonantGradientTop = Color(0xFFF4E031)
val ResonantGradientBottom = Color(0xFFFABB22)

// The soft discs sit as a decorative layer on top of the wash — not part of the
// supplied background asset, kept as a separate design element. One fixed
// colour over a moving gradient: they read darker against the top of the wash
// and lighter toward the bottom.
val ResonantDisc = Color(0xFFF2A007)

// Flat midpoint of the surface gradient — used only as a Material fallback
// (background/surface) for any default component not wrapped in
// ResonantSurface. Every actual screen uses ResonantSurface's real gradient.
val ResonantSurfaceFallback = Color(0xFFF7CD29)

// Primary system colors — flat, confident, high-contrast.
val ResonantOrange = Color(0xFFFF5A1F)
val ResonantYellow = Color(0xFFFFD400)
val ResonantBlack = Color(0xFF0A0A0A)
val ResonantWhite = Color(0xFFFFFFFF)

// Supporting neutrals — kept minimal on purpose.
val ResonantGray = Color(0xFF6B6B6B)
val ResonantLightGray = Color(0xFFE8E6E1)

// Semantic feedback colors (never the *only* signal — always paired with text/haptics/audio).
val ResonantCorrectGreen = Color(0xFF1E8E3E)
val ResonantIncorrectRed = Color(0xFFD3312B)
