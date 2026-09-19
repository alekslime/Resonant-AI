package com.resonant.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Shared spacing so every screen's content sits inside the same symmetric
 * margins, regardless of which edge a screen happens to emphasize visually.
 * Horizontal padding is deliberately equal on both sides — the previous
 * per-screen values (32.dp/24.dp, or 52.dp/24.dp on Home) were left over
 * from a background image with visual weight on one side; the flat gradient
 * has none, so there's no reason for margins to be lopsided.
 */
val ScreenHorizontalPadding = 40.dp
val ScreenTopPadding = 48.dp
val ScreenBottomPadding = 40.dp

/** Left/right gesture-zone width. Matches [com.resonant.app.gestures]' edge
 *  widths, so the visual margin and the actual touch target line up — and
 *  wider than the old 48.dp, closer to a size that's forgiving to hit
 *  without precise aim. */
val EdgeZoneWidth = 64.dp
