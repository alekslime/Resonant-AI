package com.resonant.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.ui.theme.BrandInk
import com.resonant.app.ui.theme.MetropolisBlack

/**
 * The pill button from the Figma "Continue lesson" card: black with white text, or (for the
 * less important choice on a screen) white with black text. Used by every screen that has
 * real buttons, so they all look like they belong with the Lessons and Quizzes screens.
 */
@Composable
fun ResonantButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    enabled: Boolean = true
) {
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(50))
            .background(if (filled) BrandInk else Color.White)
            .semantics { role = Role.Button }
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
            color = if (filled) Color.White else BrandInk,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * A text field drawn as one of the white rounded cards used everywhere else: a small label on
 * top, the typed text big and heavy underneath. The label is also the spoken description.
 */
@Composable
fun ResonantTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val valueStyle = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, color = BrandInk)
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
            color = Color(0xFF4A4A4A)
        )
        Box(Modifier.padding(top = 6.dp)) {
            if (value.isEmpty() && placeholder != null) {
                Text(placeholder, style = valueStyle, color = Color(0xFF6B6B6B), maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = valueStyle,
                cursorBrush = SolidColor(BrandInk),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = label }
            )
        }
    }
}
