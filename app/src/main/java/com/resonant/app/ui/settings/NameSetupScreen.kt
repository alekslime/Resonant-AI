package com.resonant.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.resonant.app.ResonantApp
import com.resonant.app.core.LocalDebugState
import com.resonant.app.ui.components.ResonantScaffold
import com.resonant.app.ui.theme.LocalResonantColors
import com.resonant.app.ui.theme.ScreenHorizontalPadding

/** The first name used in the Chat greeting ("Hi Marc!"). Blank = the generic greeting. */
@Composable
fun NameSetupScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val prefs = (context.applicationContext as ResonantApp).container.prefs
    val debug = LocalDebugState.current
    val colors = LocalResonantColors.current

    var name by remember { mutableStateOf(prefs.userName.orEmpty()) }
    LaunchedEffect(Unit) { debug.setScreen("Your name") }

    fun save() {
        prefs.userName = name
        onDone()
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.text,
        unfocusedTextColor = colors.text,
        focusedBorderColor = colors.text,
        unfocusedBorderColor = colors.text,
        focusedLabelColor = colors.text,
        unfocusedLabelColor = colors.text,
        cursorColor = colors.text
    )

    ResonantScaffold(title = "Your name", subtitle = "Shown in the Chat greeting") {
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = ScreenHorizontalPadding, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                label = { Text("First name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { save() }),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { save() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.focusedFill,
                    contentColor = colors.focusedText
                ),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save") }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { name = ""; prefs.userName = null; onDone() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.text),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Clear") }
        }
    }
}
