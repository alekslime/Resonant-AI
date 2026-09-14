package com.resonant.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ResonantColorScheme = lightColorScheme(
    primary = ResonantOrange,
    onPrimary = ResonantWhite,
    secondary = ResonantYellow,
    onSecondary = ResonantBlack,
    background = ResonantWhite,
    onBackground = ResonantBlack,
    surface = ResonantWhite,
    onSurface = ResonantBlack,
    error = ResonantIncorrectRed,
    onError = ResonantWhite,
    outline = ResonantGray
)

@Composable
fun ResonantTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ResonantColorScheme,
        typography = ResonantTypography,
        content = content
    )
}
