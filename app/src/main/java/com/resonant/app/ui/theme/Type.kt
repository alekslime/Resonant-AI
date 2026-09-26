package com.resonant.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Sizes below are baseline (1x font scale) and deliberately generous — this
 * pass is tuned for low-vision readability, not just visual weight. Defined
 * in `sp`, so they still scale with the user's system font size setting on
 * top of this already-large baseline.
 *
 * Line heights are ~1.3-1.55x font size (up from ~1.15-1.44x previously) —
 * tight leading gets hard to track line-to-line at these sizes for anyone
 * with central or peripheral field loss.
 *
 * There is no separate "small caption" role: the smallest text role
 * (labelMedium) is still 20sp, because nothing in this app should read as a
 * disposable caption to its actual audience.
 */
val ResonantTypography = Typography(
    displayLarge = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.Black, fontSize = 56.sp, lineHeight = 64.sp),
    headlineLarge = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 52.sp),
    headlineMedium = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 42.sp),
    headlineSmall = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp),
    titleLarge = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    bodyLarge = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.Normal, fontSize = 26.sp, lineHeight = 38.sp),
    bodyMedium = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 32.sp),
    labelLarge = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    labelMedium = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp)
)
