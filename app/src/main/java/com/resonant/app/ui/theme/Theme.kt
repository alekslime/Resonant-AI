package com.resonant.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

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
    val dark = isSystemInDarkTheme()
    val extended = if (dark) DarkResonantColors else LightResonantColors
    CompositionLocalProvider(LocalResonantColors provides extended) {
        MaterialTheme(
            colorScheme = if (dark) ResonantDarkScheme else ResonantLightScheme,
            typography = ResonantTypography,
            content = content
        )
    }
}
