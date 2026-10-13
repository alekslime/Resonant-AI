package com.resonant.app.ui.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.ResonantApp
import com.resonant.app.audio.AudioManager
import com.resonant.app.content.MaterialStore
import com.resonant.app.content.QuizData
import com.resonant.app.content.QuizProgressStore
import com.resonant.app.content.SemanticUnit
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.gestures.InteractionZone
import com.resonant.app.gestures.ResonantGesture
import com.resonant.app.gestures.SwipeDirection
import com.resonant.app.haptics.HapticPattern
import com.resonant.app.ui.components.DotsState
import com.resonant.app.ui.components.GestureSurface
import com.resonant.app.ui.components.ResonantDots
import com.resonant.app.ui.components.ResonantScaffold
import com.resonant.app.ui.components.focusColor
import com.resonant.app.ui.theme.ActiveFont
import com.resonant.app.ui.theme.ActiveMotion
import com.resonant.app.ui.theme.ActivePalette
import com.resonant.app.ui.theme.BrandInk
import com.resonant.app.ui.theme.MetropolisBlack
import com.resonant.app.ui.theme.ResonantCard
import com.resonant.app.ui.theme.ResonantOnInk
import com.resonant.app.ui.theme.cardBorder
import com.resonant.app.ui.theme.cardShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val PROFILE_HINT =
    "Swipe down with two fingers for the next item, tap the middle to open it, or hold the right edge to go back."

/** One line on the screen. [action] is what a tap does; null means the line is only read. */
private class ProfileRow(val label: String, val action: (() -> Unit)?)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    onOpenName: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    val context = LocalContext.current
    val prefs = remember { (context.applicationContext as ResonantApp).container.prefs }
    val name = prefs.userName?.trim()?.takeIf { it.isNotEmpty() }
    val speedIndex by audio.speedIndex.collectAsState()

    var stats by remember { mutableStateOf<ProfileStats?>(null) }
    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        debug.setScreen("Profile")
        stats = withContext(Dispatchers.IO) {
            val progress = QuizProgressStore.loadAll(context)
            ProfileSummary.stats(
                progress = progress,
                quizIds = QuizData.allQuizSets.map { it.id },
                materials = MaterialStore.forContext(context).list().size
            )
        }
    }

    val loaded = stats
    val rows: List<ProfileRow> = if (loaded == null) emptyList() else buildList {
        add(ProfileRow("Your name: ${name ?: "not set"}", onOpenName))
        add(ProfileRow("How am I doing?") {
            audio.announce(ProfileSummary.spoken(name, loaded))
        })
        add(ProfileRow(ProfileSummary.quizLine(loaded), null))
        ProfileSummary.averageLine(loaded)?.let { add(ProfileRow(it, null)) }
        add(ProfileRow(ProfileSummary.materialsLine(loaded), null))
        add(ProfileRow("Color theme: ${ActivePalette.current.label}", onOpenSettings))
        add(ProfileRow("Font: ${ActiveFont.current.label}", onOpenSettings))
        add(ProfileRow("Speech speed: ${audio.speedLabel(AudioManager.SPEEDS[speedIndex])}", onOpenSettings))
    }
    val labels = rows.map { it.label }

    // The focus index comes back from the audio queue, as on Home and Settings.
    LaunchedEffect(Unit) {
        audio.index.collect { idx -> index = idx.coerceIn(0, (labels.size - 1).coerceAtLeast(0)) }
    }
    // Set the queue once the numbers are in, so nothing is read before there is something true to say.
    LaunchedEffect(loaded != null, labels) {
        if (loaded == null) return@LaunchedEffect
        audio.setQueue(
            labels.mapIndexed { i, s -> SemanticUnit("profile_$i", s) },
            startIndex = index.coerceIn(0, labels.lastIndex),
            autoAdvance = false
        )
    }

    fun activate(i: Int) {
        val row = rows.getOrNull(i) ?: return
        haptics.play(HapticPattern.SELECT)
        if (row.action != null) row.action.invoke() else audio.repeatCurrent()
    }

    ResonantScaffold(
        title = "Profile",
        subtitle = name ?: "Your progress",
        onBack = onBack,
        backAnnouncement = "Back to Home."
    ) {
        GestureSurface(
            twoFingerSwipe = true,
            ignoreChildTaps = true,
            hint = PROFILE_HINT,
            onGesture = { gesture ->
                when (gesture) {
                    is ResonantGesture.Swipe -> if (gesture.zone == InteractionZone.CENTER) {
                        when (gesture.direction) {
                            SwipeDirection.UP -> if (audio.next()) haptics.play(HapticPattern.NEXT)
                            SwipeDirection.DOWN -> if (audio.previous()) haptics.play(HapticPattern.PREVIOUS)
                            else -> {}
                        }
                    }
                    is ResonantGesture.Tap -> when (gesture.zone) {
                        InteractionZone.CENTER -> activate(index)
                        InteractionZone.LEFT_EDGE -> { audio.togglePause(); haptics.play(HapticPattern.CONFIRM) }
                        InteractionZone.RIGHT_EDGE -> {}
                    }
                    is ResonantGesture.DoubleTap -> if (gesture.zone == InteractionZone.LEFT_EDGE) audio.repeatCurrent()
                    is ResonantGesture.LongPress -> if (gesture.zone == InteractionZone.RIGHT_EDGE) {
                        haptics.play(HapticPattern.BACK)
                        audio.announce("Back to Home.")
                        onBack()
                    }
                    ResonantGesture.ThreeFingerTap -> audio.repeatCurrent()
                    ResonantGesture.ThreeFingerHold -> audio.announce(
                        "Profile. Currently focused: ${labels.getOrNull(index) ?: "loading"}."
                    )
                    ResonantGesture.HoldSpeedUp -> { if (audio.increaseSpeed()) haptics.play(HapticPattern.SPEED_UP) else haptics.play(HapticPattern.ERROR) }
                    ResonantGesture.HoldSpeedDown -> { if (audio.decreaseSpeed()) haptics.play(HapticPattern.SPEED_DOWN) else haptics.play(HapticPattern.ERROR) }
                    else -> {}
                }
            }
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                Avatar(name)

                rows.forEachIndexed { i, row ->
                    val focused = i == index
                    val inView = remember { BringIntoViewRequester() }
                    LaunchedEffect(focused) { if (focused) inView.bringIntoView() }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(inView)
                            .clip(cardShape())
                            .background(focusColor(if (focused) BrandInk else ResonantCard))
                            .cardBorder(cardShape())
                            .clickable {
                                audio.jumpTo(i)
                                activate(i)
                            }
                            .padding(horizontal = 20.dp, vertical = 22.dp)
                            .semantics { contentDescription = row.label + if (focused) ", focused" else "" }
                    ) {
                        Crossfade(
                            targetState = row.label,
                            animationSpec = tween(ActiveMotion.ms(150)),
                            label = "profileRow"
                        ) { shown ->
                            Text(
                                shown,
                                style = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.Black, fontSize = 26.sp, lineHeight = 32.sp),
                                color = focusColor(if (focused) ResonantOnInk else BrandInk)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/**
 * The person's name in braille, one cell per letter, so it is unique without a photo. The cells appear one
 * after another (all at once with Reduce motion on). With no name yet, the Resonant mark stands in.
 */
@Composable
private fun Avatar(name: String?) {
    val cells = BrailleAvatar.cells(name)
    if (cells.isEmpty()) {
        Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            ResonantDots(state = DotsState.Idle, modifier = Modifier.fillMaxSize())
        }
        return
    }
    var shown by remember(name) { mutableIntStateOf(if (ActiveMotion.reduce) cells.size else 0) }
    LaunchedEffect(name) {
        if (ActiveMotion.reduce) { shown = cells.size; return@LaunchedEffect }
        while (shown < cells.size) { delay(110); shown += 1 }
    }
    Row(
        Modifier
            .height(120.dp)
            .semantics { contentDescription = "Your name in braille dots" },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        cells.forEachIndexed { i, dots ->
            val alpha by animateFloatAsState(
                targetValue = if (i < shown) 1f else 0f,
                animationSpec = tween(ActiveMotion.ms(200)),
                label = "avatarCell"
            )
            BrailleCell(dots, alpha)
        }
    }
}

@Composable
private fun BrailleCell(raised: List<Int>, alpha: Float) {
    val ink = BrandInk
    Canvas(Modifier.size(width = 44.dp, height = 88.dp)) {
        val r = size.width * 0.17f
        val colX = listOf(size.width * 0.25f, size.width * 0.75f)
        val rowY = listOf(size.height * 0.15f, size.height * 0.5f, size.height * 0.85f)
        for (dot in 1..6) {
            val col = if (dot <= 3) 0 else 1
            val row = (dot - 1) % 3
            val on = dot in raised
            drawCircle(
                color = ink.copy(alpha = (if (on) 1f else 0.15f) * alpha),
                radius = r,
                center = Offset(colX[col], rowY[row])
            )
        }
    }
}
