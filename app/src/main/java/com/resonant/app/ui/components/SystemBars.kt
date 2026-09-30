package com.resonant.app.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Paints the status bar and navigation bar the same color as the screen behind them, so the
 * screen reads as one full-height surface instead of a colored panel between two black strips.
 * Icons flip between dark and light to stay readable on whatever color this is.
 *
 * Every screen calls this with its own background. Layout is untouched: content still sits
 * below the status bar, only the bar's color changes.
 */
@Composable
fun SystemBarsColor(color: Color) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        @Suppress("DEPRECATION")
        window.statusBarColor = color.toArgb()
        @Suppress("DEPRECATION")
        window.navigationBarColor = color.toArgb()
        val lightBackground = color.luminance() > 0.5f
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightBackground
            isAppearanceLightNavigationBars = lightBackground
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
