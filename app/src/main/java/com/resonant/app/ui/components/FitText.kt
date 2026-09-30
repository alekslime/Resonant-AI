package com.resonant.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * Text that shrinks until it fits the height it is given (put it in a bounded, e.g. weighted,
 * slot). For content that must stay fully on screen at any system font size, since the gesture
 * surface around it can't be scrolled by touch. It only ever shrinks from [style], and stops
 * at [minSize]; text that still overflows there is clipped.
 */
@Composable
fun FitText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    minSize: TextUnit = 16.sp
) {
    var fontSize by remember(text, style.fontSize) { mutableStateOf(style.fontSize) }
    val ratio = fontSize.value / style.fontSize.value
    Text(
        text = text,
        style = style.copy(
            fontSize = fontSize,
            lineHeight = if (style.lineHeight.isSpecified) style.lineHeight * ratio else style.lineHeight
        ),
        color = color,
        modifier = modifier,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && fontSize > minSize) fontSize = (fontSize.value * .9f).sp
        }
    )
}
