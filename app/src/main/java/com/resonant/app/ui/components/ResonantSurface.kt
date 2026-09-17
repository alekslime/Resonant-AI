package com.resonant.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.resonant.app.R

/**
 * The Resonant surface: a soft, grainy yellow-orange blob fading to white —
 * a baked-in image asset (drawable-nodpi/resonant_background.jpg) rather than
 * a flat Brush gradient, since the approved look is an irregular blurred
 * blend plus grain, not a clean two-stop wash.
 *
 * Every screen sits on this, so moving between screens never changes the
 * background — only the content on top of it.
 */
@Composable
fun ResonantSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.resonant_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        content()
    }
}
