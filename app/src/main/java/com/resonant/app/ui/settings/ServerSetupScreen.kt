package com.resonant.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.resonant.app.ResonantApp
import com.resonant.app.core.LocalDebugState
import com.resonant.app.network.OllamaConfig
import com.resonant.app.network.checkOllamaConnection
import com.resonant.app.network.describeCheck
import com.resonant.app.network.normalizeBaseUrl
import com.resonant.app.ui.components.ResonantScaffold
import com.resonant.app.ui.theme.LocalResonantColors
import com.resonant.app.ui.theme.ScreenHorizontalPadding
import kotlinx.coroutines.launch

/**
 * Where Chat finds its AI, editable on the phone. Built for the person setting the
 * app up (a sighted developer or presenter), so unlike every other screen it is
 * ordinary touch UI with text fields — no [com.resonant.app.ui.components.GestureSurface],
 * which would swallow the taps a text field needs. System back leaves it.
 *
 * Saving takes effect immediately (the next Chat question uses the new address) and
 * survives restarts. "Use build defaults" clears the override.
 */
@Composable
fun ServerSetupScreen(onDone: () -> Unit) {
    val container = (LocalContext.current.applicationContext as ResonantApp).container
    val prefs = container.prefs
    val debug = LocalDebugState.current
    val colors = LocalResonantColors.current
    val scope = rememberCoroutineScope()

    var address by remember { mutableStateOf(OllamaConfig.BASE_URL) }
    var model by remember { mutableStateOf(OllamaConfig.MODEL) }
    var status by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { debug.setScreen("Server setup") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.text,
        unfocusedTextColor = colors.text,
        focusedBorderColor = colors.text,
        unfocusedBorderColor = colors.text,
        focusedLabelColor = colors.text,
        unfocusedLabelColor = colors.text,
        cursorColor = colors.text
    )
    val primaryButton = ButtonDefaults.buttonColors(
        containerColor = colors.focusedFill,
        contentColor = colors.focusedText
    )
    val secondaryButton = ButtonDefaults.outlinedButtonColors(contentColor = colors.text)

    ResonantScaffold(title = "Server setup", subtitle = "Where Chat finds its AI") {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, bottom = 32.dp)
        ) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it; status = null },
                label = { Text("Server address") },
                placeholder = { Text("192.168.1.50:11434") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = model,
                onValueChange = { model = it; status = null },
                label = { Text("Model") },
                placeholder = { Text("llama3.2") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val candidate = normalizeBaseUrl(address)
                    if (candidate == null) {
                        status = "That doesn't look like an address. Try 192.168.1.50:11434."
                    } else if (!checking) {
                        checking = true
                        status = "Checking $candidate ..."
                        scope.launch {
                            val result = checkOllamaConnection(candidate, model)
                            status = describeCheck(result, model)
                            checking = false
                        }
                    }
                },
                enabled = !checking,
                colors = primaryButton,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Test connection") }

            status?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.text,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val candidate = normalizeBaseUrl(address)
                    if (candidate == null || model.isBlank()) {
                        status = "Enter an address and a model name first."
                    } else {
                        prefs.ollamaBaseUrl = candidate
                        prefs.ollamaModel = model.trim()
                        OllamaConfig.applyOverrides(candidate, model)
                        onDone()
                    }
                },
                colors = primaryButton,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save") }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    prefs.ollamaBaseUrl = null
                    prefs.ollamaModel = null
                    OllamaConfig.applyOverrides(null, null)
                    address = OllamaConfig.BASE_URL
                    model = OllamaConfig.MODEL
                    status = "Back to the build defaults."
                },
                colors = secondaryButton,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Use build defaults") }

            Text(
                "Plain http:// only works in debug builds. Ollama listens only on its own " +
                    "machine unless started with OLLAMA_HOST=0.0.0.0.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}
