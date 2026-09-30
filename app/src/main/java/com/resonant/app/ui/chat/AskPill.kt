package com.resonant.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.ui.home.MicPlaceholderIcon
import com.resonant.app.ui.theme.BrandInk
import com.resonant.app.ui.theme.MetropolisBlack

private val PillColor = Color(0xFF0F0F0F)
private val PillText = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)

/**
 * "Ask anything…" — type a question, or tap the mic to ask by voice (same path as tapping the
 * center of the screen). Lives outside the GestureSurface on purpose: the gesture detector
 * consumes touches, so a text field inside it could never be focused.
 */
@Composable
fun AskPill(
    listening: Boolean,
    onSubmit: (String) -> Unit,
    onMic: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current

    fun submit() {
        val q = text.trim()
        if (q.isEmpty()) return
        text = ""
        focus.clearFocus()
        onSubmit(q)
    }

    Row(
        modifier = modifier
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .fillMaxWidth()
            .heightIn(min = 64.dp) // grows with the system font size
            .clip(RoundedCornerShape(percent = 50))
            .background(PillColor)
            .padding(start = 22.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (text.isEmpty()) Text(
                "Ask anything...",
                style = PillText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = PillText,
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Type a question, then press send" }
            )
        }
        Box(
            Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.White)
                .semantics {
                    role = Role.Button
                    contentDescription = if (listening) "Listening" else "Ask by voice"
                }
                .clickable { onMic() },
            contentAlignment = Alignment.Center
        ) {
            MicPlaceholderIcon(tint = BrandInk, modifier = Modifier.size(26.dp))
        }
    }
}
