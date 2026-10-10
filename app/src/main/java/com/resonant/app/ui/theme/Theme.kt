package com.resonant.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush

private val ResonantLightScheme = lightColorScheme(
    primary = ResonantOrange,
    onPrimary = ResonantWhite,
    secondary = ResonantYellow,
    onSecondary = ResonantBlack,
    background = GradientLightBottom,
    onBackground = ResonantTextOnLight,
    surface = GradientLightBottom,
    onSurface = ResonantTextOnLight,
    error = ResonantIncorrectFillLight,
    onError = ResonantWhite,
    outline = ResonantGray
)

private val ResonantDarkScheme = darkColorScheme(
    primary = ResonantOrangeOnDark,
    onPrimary = ResonantBlack,
    secondary = ResonantYellow,
    onSecondary = ResonantBlack,
    background = GradientDarkBottom,
    onBackground = ResonantTextOnDark,
    surface = GradientDarkBottom,
    onSurface = ResonantTextOnDark,
    error = ResonantIncorrectFillDark,
    onError = ResonantBlack,
    outline = ResonantGray
)

/**
 * Picks light/dark by system setting and provides [LocalResonantColors]
 * alongside standard MaterialTheme — screens read gradient/chip/feedback
 * colors from that local instead of branching on dark mode themselves.
 */
@Composable
fun ResonantTheme(content: @Composable () -> Unit) {
    // Figma only defines the flat-orange / black-ink look, so dark mode is off for now.
    // To bring it back: val dark = isSystemInDarkTheme()
    val dark = false
    val palette = ActivePalette.current
    val extended = remember(palette) {
        if (dark) DarkResonantColors
        else if (palette.id == Palettes.DEFAULT.id) LightResonantColors
        else LightResonantColors.copy(
            gradient = Brush.verticalGradient(listOf(palette.background, palette.background)),
            text = palette.ink,
            focusedFill = palette.ink,
            focusedText = palette.onInk,
            statusBar = palette.background
        )
    }
    val scheme = remember(palette) {
        if (dark) ResonantDarkScheme
        else if (palette.id == Palettes.DEFAULT.id) ResonantLightScheme
        else ResonantLightScheme.copy(
            primary = palette.ink,
            onPrimary = palette.onInk,
            secondary = palette.ink,
            onSecondary = palette.onInk,
            background = palette.background,
            onBackground = palette.ink,
            surface = palette.background,
            onSurface = palette.ink,
            outline = palette.ink
        )
    }
    CompositionLocalProvider(LocalResonantColors provides extended) {
        MaterialTheme(
            colorScheme = scheme,
            typography = ResonantTypography,
            content = content
        )
    }
}
