package com.resonant.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.resonant.app.ui.theme.LocalResonantColors

/**
 * The Resonant surface: a flat, harsh two/three-stop gradient — no grain, no
 * blur. Replaces the earlier soft blurred-blob background image. Read from
 * [LocalResonantColors] so it automatically switches between the light and dark palette; screens
 * never branch on dark mode themselves.
 *
 * Every screen sits on this, so moving between screens never changes the
 * background — only the content on top of it.
 */
@Composable
fun ResonantSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LocalResonantColors.current
    Box(
        modifier
            .fillMaxSize()
            .background(colors.gradient),
        content = content
    )
}
