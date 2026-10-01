package com.resonant.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.resonant.app.ResonantApp
import com.resonant.app.core.LocalDebugState
import com.resonant.app.ui.components.ResonantButton
import com.resonant.app.ui.components.ResonantScaffold
import com.resonant.app.ui.components.ResonantTextField
import com.resonant.app.ui.theme.LocalResonantColors

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

    ResonantScaffold(
        title = "Your name",
        subtitle = "Shown in the Chat greeting",
        onBack = onDone,
        backAnnouncement = "Back to Settings."
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            ResonantTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                label = "First name",
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { save() })
            )
            Spacer(Modifier.height(20.dp))
            ResonantButton(text = "Save", onClick = { save() })
            Spacer(Modifier.height(12.dp))
            ResonantButton(
                text = "Clear",
                filled = false,
                onClick = { name = ""; prefs.userName = null; onDone() }
            )
        }
    }
}
