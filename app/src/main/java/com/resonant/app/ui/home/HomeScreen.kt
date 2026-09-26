package com.resonant.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.resonant.app.content.SemanticUnit
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.gestures.InteractionZone
import com.resonant.app.gestures.ResonantGesture
import com.resonant.app.gestures.SwipeDirection
import com.resonant.app.haptics.HapticPattern
import com.resonant.app.ui.components.GestureSurface
import com.resonant.app.ui.theme.BrandInk
import com.resonant.app.ui.theme.BrandOrange
import com.resonant.app.ui.theme.MetropolisBlack

private data class HomeItem(val label: String, val route: String)

/** Route sentinel — handled by the nav host, not an actual destination. */
const val ROUTE_EXIT = "exit"

private val homeItems = listOf(
    HomeItem("Chat", "chat"),
    HomeItem("Lessons", "lessons"),
    HomeItem("Quiz", "quiz_browser"),
    HomeItem("Settings", "settings"),
    HomeItem("Exit", ROUTE_EXIT)
)

/**
 * Figma redesign (file 7WUwiMrri25Z9motdBHDc7, node 2001:33). This screen is
 * now a fixed brand surface — flat [BrandOrange], not the adaptive
 * light/dark gradient every other screen sits on (see ResonantSurface) —
 * matching the Splash screen it now visually continues from.
 *
 * Decisions made turning this into code:
 *  - The two brand fonts (Agharti / GC Sublime) aren't in the project yet;
 *    see ui/theme/Fonts.kt for exactly where the files need to go.
 *  - Focus is shown by color alone (black on the focused item, white on the
 *    rest, on the [BrandOrange] background), per the reference design —
 *    matched here exactly as an explicit call after flagging that the
 *    white/orange pairing measures ~1.9:1 contrast, under WCAG AA and short
 *    of the deliberate contrast work described in Color.kt. If that ever
 *    needs revisiting, pairing the color with weight and/or an underline on
 *    the focused item (as this screen briefly did) restores a second,
 *    color-independent signal without changing the color scheme itself.
 *  - The mic/settings icons Figma exports as SVGs couldn't be downloaded in
 *    this sandbox (no network access to Figma's asset URLs) — see
 *    IconPlaceholders.kt for the placeholders standing in for them.
 */
private val WordmarkStyle = TextStyle(
    fontFamily = MetropolisBlack,
    fontWeight = FontWeight.Black,
    fontSize = 130.sp
)

private val MenuTextStyle = TextStyle(
    fontFamily = MetropolisBlack,
    fontWeight = FontWeight.Black,
    fontSize = 64.sp
)

private val AskBarStyle = TextStyle(fontFamily = MetropolisBlack, fontSize = 20.sp)

private val HomeHorizontalPadding = 24.dp
private val AskBarPillColor = Color(0xFF2B2A2A)

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    var index by remember { mutableIntStateOf(0) }

    // Single effect: setQueue must land before collection starts, or the first
    // collected value could be the pre-queue default instead of the real start.
    LaunchedEffect(Unit) {
        debug.setScreen("Home")
        audio.setQueue(
            homeItems.mapIndexed { i, item -> SemanticUnit("home_$i", item.label) },
            startIndex = 0,
            autoAdvance = false
        )
        audio.index.collect { idx ->
            index = idx.coerceIn(0, (homeItems.size - 1).coerceAtLeast(0))
        }
    }

    fun open(i: Int) {
        val item = homeItems[i]
        haptics.play(HapticPattern.SELECT)
        if (item.route == ROUTE_EXIT) {
            audio.announce("Exiting Resonant.")
        } else {
            audio.announce("Opening ${item.label}.")
        }
        onNavigate(item.route)
    }

    // Direct-tap shortcuts from the Ask-anything bar route through the exact
    // same open() as the matching menu item — same haptic, same announce,
    // same audio-focus update — rather than a second, parallel nav path.
    fun openByRoute(route: String) {
        val i = homeItems.indexOfFirst { it.route == route }
        if (i >= 0) {
            audio.jumpTo(i)
            open(i)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BrandOrange)
    ) {
        GestureSurface(onGesture = { gesture ->
            when (gesture) {
                is ResonantGesture.Swipe -> if (gesture.zone == InteractionZone.CENTER) {
                    when (gesture.direction) {
                        SwipeDirection.UP -> if (audio.next()) haptics.play(HapticPattern.NEXT)
                        SwipeDirection.DOWN -> if (audio.previous()) haptics.play(HapticPattern.PREVIOUS)
                        else -> {}
                    }
                }
                is ResonantGesture.Tap -> when (gesture.zone) {
                    InteractionZone.CENTER -> open(index)
                    InteractionZone.LEFT_EDGE -> {
                        audio.togglePause()
                        haptics.play(HapticPattern.CONFIRM)
                    }
                    InteractionZone.RIGHT_EDGE -> {}
                }
                is ResonantGesture.DoubleTap -> if (gesture.zone == InteractionZone.LEFT_EDGE) {
                    audio.repeatCurrent()
                }
                is ResonantGesture.LongPress -> if (gesture.zone == InteractionZone.RIGHT_EDGE) {
                    haptics.play(HapticPattern.BACK)
                    audio.announce("You are already on the Home screen.")
                }
                ResonantGesture.ThreeFingerTap -> audio.repeatCurrent()
                ResonantGesture.ThreeFingerHold -> audio.announce(
                    "You are on the Home screen. Currently focused: ${homeItems[index].label}."
                )
                ResonantGesture.HoldSpeedUp -> {
                    if (audio.increaseSpeed()) haptics.play(HapticPattern.SPEED_UP)
                    else haptics.play(HapticPattern.ERROR)
                }
                ResonantGesture.HoldSpeedDown -> {
                    if (audio.decreaseSpeed()) haptics.play(HapticPattern.SPEED_DOWN)
                    else haptics.play(HapticPattern.ERROR)
                }
                else -> {}
            }
        }) {
            Column(Modifier.fillMaxSize()) {
                // Wordmark. Auto-shrinks to whatever size actually fits this
                // font's real character widths, rather than a hardcoded 130sp
                // tuned against a different (fallback) font's metrics — that
                // was the actual bug: 130sp fit the system font's proportions,
                // not Agharti's. Top-aligned with no crop for the same reason:
                // the -14dp "crop to match Figma" offset was tuned against the
                // fallback font's line-height, which is why swapping in the
                // real font made it crop far more than intended. Once this is
                // visible with the real font, dial the crop back in as an
                // actual measured Modifier.height + clip if you still want it,
                // rather than another guessed offset.
                var wordmarkSize by remember { mutableStateOf(130.sp) }
                Text(
                    text = "RESONANT",
                    style = WordmarkStyle.copy(fontSize = wordmarkSize),
                    color = BrandInk,
                    maxLines = 1,
                    softWrap = false,
                    onTextLayout = { result ->
                        if (result.didOverflowWidth && wordmarkSize > 40.sp) {
                            wordmarkSize *= 0.95f
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HomeHorizontalPadding)
                )

                Spacer(Modifier.height(144.dp))

                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    homeItems.forEachIndexed { i, item ->
                        val focused = i == index
                        Text(
                            text = item.label,
                            style = MenuTextStyle,
                            color = if (focused) BrandInk else Color.White,
                            modifier = Modifier
                                .clickable {
                                    audio.jumpTo(i)
                                    open(i)
                                }
                                .semantics {
                                    contentDescription =
                                        item.label + if (focused) ", focused" else ""
                                }
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                AskBar(
                    onOpenChat = { openByRoute("chat") },
                    onOpenSettings = { openByRoute("settings") }
                )

                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun AskBar(onOpenChat: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HomeHorizontalPadding)
            .height(67.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(AskBarPillColor)
            .clickable { onOpenChat() }
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(55.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { onOpenChat() },
            contentAlignment = Alignment.Center
        ) {
            MicPlaceholderIcon(tint = BrandInk, modifier = Modifier.size(26.dp))
        }

        Text(
            text = "Ask anything",
            style = AskBarStyle,
            color = Color.White,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        )

        Box(
            Modifier
                .size(55.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { onOpenSettings() },
            contentAlignment = Alignment.Center
        ) {
            SettingsPlaceholderIcon(tint = BrandInk, modifier = Modifier.size(26.dp))
        }
    }
}
