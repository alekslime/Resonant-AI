package com.resonant.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Everything Material3's ColorScheme has no slot for: the gradient brush
 * itself, and the position-independent chip/feedback colors from Color.kt.
 * One instance per theme (light/dark), swapped by [ResonantTheme] based on
 * system dark mode — screens read this instead of branching on
 * isSystemInDarkTheme() themselves.
 */
data class ResonantExtendedColors(
    val gradient: Brush,
    val text: Color,
    val focusedFill: Color,
    val focusedText: Color,
    val correctFill: Color,
    val incorrectFill: Color,
    val feedbackText: Color
)

val LightResonantColors = ResonantExtendedColors(
    gradient = Brush.verticalGradient(listOf(GradientLightTop, GradientLightMid, GradientLightBottom)),
    text = ResonantTextOnLight,
    focusedFill = ResonantFocusedFillLight,
    focusedText = ResonantFocusedTextLight,
    correctFill = ResonantCorrectFillLight,
    incorrectFill = ResonantIncorrectFillLight,
    feedbackText = ResonantWhite
)

val DarkResonantColors = ResonantExtendedColors(
    gradient = Brush.verticalGradient(listOf(GradientDarkTop, GradientDarkMid, GradientDarkBottom)),
    text = ResonantTextOnDark,
    focusedFill = ResonantFocusedFillDark,
    focusedText = ResonantFocusedTextDark,
    correctFill = ResonantCorrectFillDark,
    incorrectFill = ResonantIncorrectFillDark,
    feedbackText = ResonantBlack
)

val LocalResonantColors = staticCompositionLocalOf { LightResonantColors }
