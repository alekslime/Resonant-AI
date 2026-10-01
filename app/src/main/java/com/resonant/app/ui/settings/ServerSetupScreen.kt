package com.resonant.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.ResonantApp
import com.resonant.app.core.LocalDebugState
import com.resonant.app.network.OllamaConfig
import com.resonant.app.network.checkOllamaConnection
import com.resonant.app.network.describeCheck
import com.resonant.app.network.normalizeBaseUrl
import com.resonant.app.ui.components.ResonantButton
import com.resonant.app.ui.components.ResonantScaffold
import com.resonant.app.ui.components.ResonantTextField
import com.resonant.app.ui.theme.BrandInk
import com.resonant.app.ui.theme.LocalResonantColors
import com.resonant.app.ui.theme.MetropolisBlack
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

    ResonantScaffold(
        title = "Server setup",
        subtitle = "Where Chat finds its AI",
        onBack = onDone,
        backAnnouncement = "Back to Debug."
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
        ) {
            ResonantTextField(
                value = address,
                onValueChange = { address = it; status = null },
                label = "Server address",
                placeholder = "192.168.1.50:11434",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
            Spacer(Modifier.height(16.dp))
            ResonantTextField(
                value = model,
                onValueChange = { model = it; status = null },
                label = "Model",
                placeholder = "llama3.2",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            Spacer(Modifier.height(24.dp))
            ResonantButton(
                text = "Test connection",
                enabled = !checking,
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
                }
            )

            status?.let {
                Text(
                    it,
                    style = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
                    color = BrandInk,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
            ResonantButton(
                text = "Save",
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
                }
            )

            Spacer(Modifier.height(12.dp))
            ResonantButton(
                text = "Use build defaults",
                filled = false,
                onClick = {
                    prefs.ollamaBaseUrl = null
                    prefs.ollamaModel = null
                    OllamaConfig.applyOverrides(null, null)
                    address = OllamaConfig.BASE_URL
                    model = OllamaConfig.MODEL
                    status = "Back to the build defaults."
                }
            )

            Text(
                "Plain http:// only works in debug builds. Ollama listens only on its own " +
                    "machine unless started with OLLAMA_HOST=0.0.0.0.",
                style = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
                color = BrandInk,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}
