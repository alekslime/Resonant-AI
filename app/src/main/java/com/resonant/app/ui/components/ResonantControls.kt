package com.resonant.app.ui.components

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.runtime.remember
import com.resonant.app.ui.theme.MinTouchTarget
import com.resonant.app.ui.theme.ResonantOnButton
import com.resonant.app.ui.theme.ResonantButton
import com.resonant.app.ui.theme.controlBorder
import com.resonant.app.ui.theme.cardBorder
import com.resonant.app.ui.theme.cardShape
import com.resonant.app.ui.theme.controlShape
import com.resonant.app.ui.theme.ResonantOnInk
import com.resonant.app.ui.theme.ResonantCard
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
    val pressSource = remember { MutableInteractionSource() }
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .alpha(if (enabled) 1f else 0.5f)
            .pressScale(pressSource)
            .clip(controlShape())
            .background(if (filled) ResonantButton else ResonantCard)
            .then(if (filled) Modifier.controlBorder(controlShape()) else Modifier.cardBorder(controlShape()))
            .semantics { role = Role.Button }
            .clickable(interactionSource = pressSource, indication = LocalIndication.current, enabled = enabled) { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
            color = if (filled) ResonantOnButton else BrandInk,
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
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    /** Focus the field and open the keyboard as soon as it appears, so nobody has to find it by touch. */
    autoFocus: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
    val valueStyle = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, color = BrandInk)
    Column(
        modifier
            .fillMaxWidth()
            .clip(cardShape())
            .background(ResonantCard)
            .cardBorder(cardShape())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
            color = BrandInk
        )
        Box(Modifier.padding(top = 6.dp)) {
            if (value.isEmpty() && placeholder != null) {
                Text(placeholder, style = valueStyle, color = BrandInk.copy(alpha = 0.6f), maxLines = 1)
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
                    .focusRequester(focusRequester)
                    .semantics { contentDescription = label }
            )
        }
    }
}
