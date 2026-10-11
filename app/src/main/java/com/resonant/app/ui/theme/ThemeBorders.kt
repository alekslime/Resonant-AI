package com.resonant.app.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

/** The theme's outline around cards. Nothing at all in themes that have none. */
fun Modifier.cardBorder(shape: Shape): Modifier {
    val p = ActivePalette.current
    return p.cardOutline?.let { this.border(p.outlineWidth, it, shape) } ?: this
}

/** The theme's outline around bars and buttons. Nothing at all in themes that have none. */
fun Modifier.controlBorder(shape: Shape): Modifier {
    val p = ActivePalette.current
    return p.controlOutline?.let { this.border(p.outlineWidth, it, shape) } ?: this
}

/** The theme's corner shape for cards. */
fun cardShape(): Shape = RoundedCornerShape(ActivePalette.current.cardRadius)

/** The theme's corner shape for bars and buttons. */
fun controlShape(): Shape = RoundedCornerShape(ActivePalette.current.controlRadius)
