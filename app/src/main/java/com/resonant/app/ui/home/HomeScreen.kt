package com.resonant.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.resonant.app.ui.theme.ResonantBlack
import com.resonant.app.ui.theme.ResonantWhite

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
 * labels.
 */
private val MenuTextStyle = TextStyle(
    fontWeight = FontWeight.Black,
    fontSize = 44.sp,
    lineHeight = 56.sp,
    letterSpacing = (-1).sp
)

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
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(start = 52.dp, end = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // The rule is a sibling of the label column inside a Row measured
                // to its own intrinsic height, so it always spans exactly the menu
                // block — no hardcoded height to keep in sync with the item count.
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Spacer(
                        Modifier
                            .fillMaxHeight()
                            .width(5.dp)
                            .background(ResonantBlack)
                    )
                    Column(
                        Modifier.padding(start = 22.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        homeItems.forEachIndexed { i, item ->
                            val focused = i == index
                            Text(
                                text = item.label,
                                style = MenuTextStyle,
                                color = if (focused) ResonantBlack else ResonantWhite,
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
                }
            }
        }
    }
}
