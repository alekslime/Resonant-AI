package com.resonant.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Background gradient. Flat, solid color bands — no grain, no blur. Replaces
// the earlier soft blurred-blob image (resonant_background.jpg) with a
// harsher, more confident palette. Every text/fill color below is checked
// against WCAG 2.1 contrast — and against BOTH ends of its gradient, not
// just wherever it happens to render.
val GradientLightTop = Color(0xFFFFE14D)
val GradientLightMid = Color(0xFFFFC12E)
val GradientLightBottom = Color(0xFFFF8A1E)

val GradientDarkTop = Color(0xFF0A0A0A)
val GradientDarkMid = Color(0xFF1A0E06)
val GradientDarkBottom = Color(0xFF3D1B05)

// ---------------------------------------------------------------------------
// Neutrals
val ResonantBlack = Color(0xFF0A0A0A)
val ResonantWhite = Color(0xFFFFFFFF)
val ResonantGray = Color(0xFF6B6B6B)
val ResonantLightGray = Color(0xFFE8E6E1)

// ---------------------------------------------------------------------------
// Accent (system controls, non-text uses; buttons, TalkBack notice, etc.)
val ResonantOrange = Color(0xFFFF5A1F)
val ResonantYellow = Color(0xFFFFD400)

// Dark-theme accent text. Lighter than ResonantOrange on purpose: FF5A1F
// only measures ~5:1 against the darkest gradient stop. FF9D3D clears 7:1
// (WCAG AAA) against every stop this app actually places text on.
val ResonantOrangeOnDark = Color(0xFFFF9D3D)

// ---------------------------------------------------------------------------
// Text. Solid, full-opacity colors — deliberately NOT alpha blends. The
// previous "unfocused" text used black/white at 45-54% alpha, which measured
// as low as ~2:1 against this gradient (WCAG AA needs 4.5:1 for body text).
// Hierarchy between focused/unfocused now comes entirely from size and
// weight, never from reduced contrast — every line of text on screen stays
// at or above AAA (7:1) against its background.
val ResonantTextOnLight = ResonantBlack   // 8.4–15.2:1 across the light gradient
val ResonantTextOnDark = ResonantWhite    // 15.5–19.8:1 across the dark gradient

// ---------------------------------------------------------------------------
// Focused-item chip. A solid filled block behind the current item, rather
// than swapping the text color. A color swap measured fine on one end of the
// gradient and as low as 1.3:1 on the other, since it depended on WHERE the
// item sat. A solid fill is position-independent: always ~19.8:1 no matter
// what's behind it.
val ResonantFocusedFillLight = ResonantBlack
val ResonantFocusedTextLight = ResonantWhite
val ResonantFocusedFillDark = ResonantWhite
val ResonantFocusedTextDark = ResonantBlack

// ---------------------------------------------------------------------------
// Feedback (correct/incorrect). Never the only signal — always paired with
// text, haptics, and audio (see HapticPattern). Rendered as a solid chip
// like the focused state, not as colored text directly on the gradient
// (plain green/red text on this background measured under 2.5:1). Light and
// dark theme use different fills — a fill that reads against the light
// gradient nearly vanishes against the dark one, and vice versa.
val ResonantCorrectFillLight = Color(0xFF0F5C26)   // white text: 8.1:1
val ResonantIncorrectFillLight = Color(0xFF8F1D1D) // white text: 8.9:1
val ResonantCorrectFillDark = Color(0xFF4CBE6C)    // black text: 8.4:1
val ResonantIncorrectFillDark = Color(0xFFFF6B66)  // black text: 7.1:1
