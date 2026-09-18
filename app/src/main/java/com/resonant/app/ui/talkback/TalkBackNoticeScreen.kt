package com.resonant.app.ui.talkback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resonant.app.ui.components.ResonantSurface
import com.resonant.app.ui.theme.ResonantBlack

private const val EXPLANATION =
    "TalkBack is turned on. Resonant has its own built-in gesture and voice " +
    "system, designed to work without a screen reader running underneath it " +
    "— with TalkBack active, the two fight over your touches and none of " +
    "Resonant's gestures will work correctly. Turn off TalkBack in " +
    "Accessibility settings, then come back to Resonant."

/**
 * Deliberately NOT built on GestureSurface/the custom gesture system — this
 * is the one screen in the app that has to work correctly *while TalkBack is
 * on*, so it uses plain Compose Buttons, which TalkBack already knows how to
 * announce and activate (double-tap) without any help from us.
 */
@Composable
fun TalkBackNoticeScreen(
    onOpenAccessibilitySettings: () -> Unit,
    onRecheck: () -> Unit,
    onContinueAnyway: () -> Unit
) {
    // TalkBack will announce this screen's own text via its normal reading
    // order — we deliberately do NOT also call our own TTS here, to avoid two
    // voices talking over each other.
    ResonantSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "TalkBack is on",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = ResonantBlack
            )
            Spacer(Modifier.height(20.dp))
            Text(
                EXPLANATION,
                style = MaterialTheme.typography.bodyLarge,
                color = ResonantBlack
            )
            Spacer(Modifier.height(40.dp))
            Button(onClick = onOpenAccessibilitySettings) {
                Text("Open Accessibility settings")
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRecheck) {
                Text("I've turned it off — continue")
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onContinueAnyway) {
                Text("Use Resonant anyway")
            }
        }
    }
}
