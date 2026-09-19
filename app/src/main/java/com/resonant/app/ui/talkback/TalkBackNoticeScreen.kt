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
import com.resonant.app.ui.theme.LocalResonantColors
import com.resonant.app.ui.theme.ScreenHorizontalPadding

private const val EXPLANATION =
    "TalkBack is turned on. While it runs, it takes over your touches before " +
    "Resonant can see them, so Resonant's own swipes, taps and holds will not work. " +
    "You have two choices. Turn TalkBack off and use Resonant's gestures. Or keep " +
    "TalkBack on: on any Resonant screen, open the TalkBack actions menu to get the " +
    "same controls — next, previous, select, continue, back, pause, repeat, where " +
    "am I, and speech speed. With TalkBack on, Resonant's voice and TalkBack's voice " +
    "can overlap."

/**
 * Two ways forward, not one: turn TalkBack off (full gesture grammar), or keep it
 * on and drive the same grammar through TalkBack's Actions menu (see the custom
 * actions GestureSurface exposes). Forcing a screen-reader user to switch their
 * screen reader off is a heavy ask, so it must never be the only route.
 *
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
    val colors = LocalResonantColors.current
    // Dark mode matters here too: with the old hardcoded ResonantBlack, this
    // screen would render black text on a black gradient in dark mode — one
    // of the few screens a sighted person setting the phone up actually
    // needs to read before TalkBack takes over.
    ResonantSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(ScreenHorizontalPadding),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "TalkBack is on",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = colors.text
            )
            Spacer(Modifier.height(20.dp))
            Text(
                EXPLANATION,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.text
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
                Text("Keep TalkBack on and use actions")
            }
        }
    }
}
