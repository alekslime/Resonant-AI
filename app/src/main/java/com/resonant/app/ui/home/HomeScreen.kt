package com.resonant.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.content.SemanticUnit
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.gestures.InteractionZone
import com.resonant.app.gestures.ResonantGesture
import com.resonant.app.gestures.SwipeDirection
import com.resonant.app.haptics.HapticPattern
import com.resonant.app.ui.components.GestureSurface
import com.resonant.app.ui.components.ResonantSurface
import com.resonant.app.ui.theme.LocalResonantColors
import com.resonant.app.ui.theme.ScreenHorizontalPadding

private data class HomeItem(val label: String, val route: String)

/** Route sentinel — handled by the nav host, not an actual destination. */
const val ROUTE_EXIT = "exit"

private val homeItems = listOf(
    HomeItem("Chat", "chat"),
    HomeItem("Lessons", "lessons"),
    HomeItem("Quiz", "quiz"),
    HomeItem("Settings", "settings"),
    HomeItem("Exit", ROUTE_EXIT)
)

/**
 * The menu type: very heavy, very tight, set large. Letter spacing is pulled in
 * negative so the words read as one stacked block rather than five separate
 * labels. Bumped up from the original scale as part of the low-vision pass —
 * this is the single biggest, most important text in the app.
 */
private val MenuTextStyle = TextStyle(
    fontWeight = FontWeight.Black,
    fontSize = 52.sp,
    lineHeight = 62.sp,
    letterSpacing = (-1).sp
)

/** Unfocused items: smaller and Normal weight — hierarchy comes from size and
 *  weight, never from a fainter color (see Color.kt), so it survives low
 *  contrast sensitivity and stays readable regardless of where it sits on
 *  the gradient. */
private val MenuUnfocusedTextStyle = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 38.sp,
    lineHeight = 50.sp,
    letterSpacing = (-0.5).sp
)

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    val colors = LocalResonantColors.current
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

    ResonantSurface {
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
            // Symmetric margins matching every other screen (ScreenHorizontalPadding on
            // both sides) — the rule bar and label column sit inside that, rather than
            // the old setup where the left margin (52.dp) and right margin (24.dp) were
            // two unrelated numbers with no shared basis.
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = ScreenHorizontalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // The rule is a sibling of the label column inside a Row measured
                // to its own intrinsic height, so it always spans exactly the menu
                // block — no hardcoded height to keep in sync with the item count.
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Spacer(
                        Modifier
                            .fillMaxHeight()
                            .width(6.dp)
                            .background(colors.text)
                    )
                    Column(
                        Modifier.padding(start = 24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        homeItems.forEachIndexed { i, item ->
                            val focused = i == index
                            val itemModifier = Modifier
                                .clickable {
                                    audio.jumpTo(i)
                                    open(i)
                                }
                                .semantics {
                                    contentDescription =
                                        item.label + if (focused) ", focused" else ""
                                }
                            if (focused) {
                                // Solid filled chip, not a color swap on the text — stays
                                // ~19.8:1 contrast no matter where it lands on the gradient.
                                Box(
                                    itemModifier
                                        .background(colors.focusedFill, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 14.dp, vertical = 4.dp)
                                ) {
                                    Text(text = item.label, style = MenuTextStyle, color = colors.focusedText)
                                }
                            } else {
                                Text(
                                    text = item.label,
                                    style = MenuUnfocusedTextStyle,
                                    color = colors.text,
                                    modifier = itemModifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
