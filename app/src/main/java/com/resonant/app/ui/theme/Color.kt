package com.resonant.app.ui.theme

import androidx.compose.ui.graphics.Color

// The Resonant surface is a single warm gradient running top-left (gold) to
// bottom-right (deep orange). These three stops are the whole palette — every
// screen sits on the same wash so the app reads as one continuous surface.
val ResonantGradientTop = Color(0xFFFFC90A)
val ResonantGradientMid = Color(0xFFF9A81B)
val ResonantGradientBottom = Color(0xFFF68B1F)

// The soft discs behind the content. One fixed colour over a moving gradient:
// they read darker against the gold at the top and lighter against the orange
// at the bottom, which is exactly the effect in the reference.
val ResonantDisc = Color(0xFFF2A007)

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
