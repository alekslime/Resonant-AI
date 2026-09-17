package com.resonant.app.ui.theme

import androidx.compose.ui.graphics.Color

// Flat midpoint of the surface gradient — used only as a Material fallback
// (background/surface) for any default component not wrapped in
// ResonantSurface. Every actual screen uses ResonantSurface's baked
// background image (drawable-nodpi/resonant_background.jpg).
val ResonantSurfaceFallback = Color(0xFFF7CD29)

// Primary system colors — flat, confident, high-contrast.
val ResonantOrange = Color(0xFFFF5A1F)
val ResonantYellow = Color(0xFFFFD400)
val ResonantBlack = Color(0xFF0A0A0A)
val ResonantWhite = Color(0xFFFFFFFF)

// Captions, subtitles, and hints — anything secondary that needs to read as
// quieter than a focused element, without vanishing against the mostly-light
// background image (resonant_background.jpg fades to near-white for most of
// the screen; only translucent BLACK holds contrast there — translucent white
// disappears).
val ResonantCaption = Color(0x8A0A0A0A) // ResonantBlack at 54% alpha

// Unfocused-but-interactive list items (a lesson, a quiz option, a menu row
// not currently selected). Same reasoning as ResonantCaption above: this used
// to be flat white, which read fine on the old flat yellow/orange gradient but
// is invisible on the new background's near-white lower two-thirds.
val ResonantUnfocused = Color(0x730A0A0A) // ResonantBlack at 45% alpha

// Supporting neutrals — kept minimal on purpose.
val ResonantGray = Color(0xFF6B6B6B)
val ResonantLightGray = Color(0xFFE8E6E1)

// Semantic feedback colors (never the *only* signal — always paired with text/haptics/audio).
val ResonantCorrectGreen = Color(0xFF1E8E3E)
val ResonantIncorrectRed = Color(0xFFD3312B)
