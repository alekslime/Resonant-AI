package com.resonant.app.ui.profile

import com.resonant.app.ui.components.focusLift
import com.resonant.app.ui.components.enterStagger
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
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
import com.resonant.app.content.ActivityText
import com.resonant.app.content.LessonLibrary
import com.resonant.app.content.MaterialStore
import com.resonant.app.content.QuizData
import com.resonant.app.content.QuizProgressStore
import com.resonant.app.content.SemanticUnit
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.gestures.ListAction
import com.resonant.app.gestures.ListGestures
import com.resonant.app.haptics.HapticPattern
import com.resonant.app.ui.components.DotsState
import com.resonant.app.ui.components.GestureSurface
import com.resonant.app.ui.components.ResonantDots
import com.resonant.app.ui.components.ResonantScaffold
import com.resonant.app.ui.components.focusColor
import com.resonant.app.ui.theme.ActiveFont
import com.resonant.app.ui.theme.AppFonts
import com.resonant.app.ui.theme.Palettes
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
import java.time.LocalDate
import kotlinx.coroutines.withContext

private const val PROFILE_HINT =
    "Swipe down with two fingers for the next item, tap the middle to open or change it, or hold the right edge to go back."

/** One line on the screen. [action] is what a tap does; null means the line is only read. */
private class ProfileRow(val label: String, val counts: Boolean = false, val action: (() -> Unit)? = null)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    onOpenName: () -> Unit,
    onBack: () -> Unit
) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    val context = LocalContext.current
    val container = remember { (context.applicationContext as ResonantApp).container }
    val prefs = container.prefs
    val name = prefs.userName?.trim()?.takeIf { it.isNotEmpty() }
    val speedIndex by audio.speedIndex.collectAsState()

    var stats by remember { mutableStateOf<ProfileStats?>(null) }
    // Saved, so coming back from Name or Settings lands on the row you left.
    var index by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        debug.setScreen("Profile")
        stats = withContext(Dispatchers.IO) {
            val progress = QuizProgressStore.loadAll(context)
            val store = MaterialStore.forContext(context)
            ProfileSummary.stats(
                progress = progress,
                quizIds = QuizData.allQuizSets.map { it.id },
                materials = store.list().size,
                activity = container.activity.snapshot(),
                today = LocalDate.now().toEpochDay().toInt(),
                lessonIds = LessonLibrary.items(store).map { it.id }
            )
        }
    }

    val loaded = stats
    val rows: List<ProfileRow> = if (loaded == null) emptyList() else buildList {
        add(ProfileRow("Your name: ${name ?: "not set"}", action = onOpenName))
        add(ProfileRow("How am I doing?") {
            audio.announce(ProfileSummary.spoken(name, loaded))
        })
        add(ProfileRow(ActivityText.streakLine(loaded.streak), counts = true))
        if (loaded.longestStreak > loaded.streak) add(ProfileRow(ActivityText.longestLine(loaded.longestStreak), counts = true))
        if (loaded.lessonsTotal > 0) add(ProfileRow(ProfileSummary.lessonsLine(loaded), counts = true))
        add(ProfileRow(ProfileSummary.quizLine(loaded), counts = true))
        ProfileSummary.averageLine(loaded)?.let { add(ProfileRow(it, counts = true)) }
        add(ProfileRow(ProfileSummary.materialsLine(loaded), counts = true))
        add(ProfileRow(ActivityText.listeningLine(loaded.listeningSeconds)))
        // Theme and font change right here, one step per tap, as they do in Settings. Changing the label
        // re-queues the row, which speaks the new value.
        add(ProfileRow("Color theme: ${ActivePalette.current.label}") {
            val next = Palettes.next(ActivePalette.current)
            ActivePalette.current = next
            prefs.colorTheme = next.id
            haptics.play(HapticPattern.CONFIRM)
        })
        add(ProfileRow("Font: ${ActiveFont.current.label}") {
            val next = AppFonts.next(ActiveFont.current)
            ActiveFont.current = next
            prefs.font = next.id
            haptics.play(HapticPattern.CONFIRM)
        })
        add(ProfileRow("Speech speed: ${audio.speedLabel(AudioManager.SPEEDS[speedIndex])}") {
            audio.announce("Hold the right edge, then drag up to speak faster or down to slow down.")
        })
    }
    val labels = rows.map { it.label }

    // The numbers climb once, when they first arrive. Instant with Reduce motion.
    val countProgress = remember { Animatable(if (ActiveMotion.reduce) 1f else 0f) }
    LaunchedEffect(loaded != null) {
        if (loaded == null || countProgress.value >= 1f) return@LaunchedEffect
        countProgress.animateTo(1f, tween(ActiveMotion.ms(900), easing = FastOutSlowInEasing))
    }

    val labelsNow by rememberUpdatedState(labels)

    // Queue first, then follow focus, in one effect: collecting before the queue is set would read the
    // previous screen's position (Home's) as this screen's focus. Waits for the numbers, so nothing is
    // read before there is something true to say.
    LaunchedEffect(loaded != null) {
        if (loaded == null) return@LaunchedEffect
        val now = labelsNow
        audio.setQueue(
            now.mapIndexed { i, s -> SemanticUnit("profile_$i", s) },
            startIndex = index.coerceIn(0, now.lastIndex),
            autoAdvance = false
        )
        audio.index.collect { idx -> index = idx.coerceIn(0, (labelsNow.size - 1).coerceAtLeast(0)) }
    }

    // A line changed after the first queueing (the speech speed, changed from this screen). Re-queue where
    // focus is, behind the speed announcement, so the spoken line is never stale.
    var queuedLabels by remember { mutableStateOf<List<String>?>(null) }
    LaunchedEffect(labels) {
        if (loaded == null) return@LaunchedEffect
        if (queuedLabels == null) { queuedLabels = labels; return@LaunchedEffect }
        if (labels == queuedLabels) return@LaunchedEffect
        queuedLabels = labels
        audio.setQueue(
            labels.mapIndexed { i, s -> SemanticUnit("profile_$i", s) },
            startIndex = index.coerceIn(0, labels.lastIndex),
            autoAdvance = false,
            queueBehindAnnouncement = true
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
                when (ListGestures.resolve(gesture)) {
                    ListAction.Next -> if (audio.next()) haptics.play(HapticPattern.NEXT)
                    ListAction.Previous -> if (audio.previous()) haptics.play(HapticPattern.PREVIOUS)
                    ListAction.Activate -> activate(index)
                    ListAction.TogglePause -> { audio.togglePause(); haptics.play(HapticPattern.CONFIRM) }
                    ListAction.Repeat -> audio.repeatCurrent()
                    ListAction.Back -> {
                        haptics.play(HapticPattern.BACK)
                        audio.announce("Back to Home.")
                        onBack()
                    }
                    ListAction.WhereAmI -> audio.announce(
                        "Profile. Currently focused: ${labels.getOrNull(index) ?: "loading"}."
                    )
                    ListAction.FasterSpeech -> haptics.play(if (audio.increaseSpeed()) HapticPattern.SPEED_UP else HapticPattern.ERROR)
                    ListAction.SlowerSpeech -> haptics.play(if (audio.decreaseSpeed()) HapticPattern.SPEED_DOWN else HapticPattern.ERROR)
                    ListAction.None -> Unit
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
                            .enterStagger(i)
                            .focusLift(focused)
                            .clip(cardShape())
                            .background(focusColor(if (focused) BrandInk else ResonantCard))
                            .cardBorder(cardShape())
                            .clickable {
                                audio.jumpTo(i)
                                // A line that only reads has been read by jumping to it; reading it twice is noise.
                                if (row.action != null) activate(i) else haptics.play(HapticPattern.SELECT)
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
                                if (row.counts) CountUp.apply(shown, countProgress.value) else shown,
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
        while (shown < cells.size) { delay(AvatarMotion.CELL_GAP_MS); shown += 1 }
    }
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // One cell per letter. They shrink, gaps included, until the whole name fits the width.
        val cellW = BrailleAvatar.cellWidthDp(cells.size, maxWidth.value).dp
        Row(
            Modifier.semantics { contentDescription = "Your name in braille dots" },
            horizontalArrangement = Arrangement.spacedBy(cellW * (16f / 44f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            cells.forEachIndexed { i, dots ->
                val progress by animateFloatAsState(
                    targetValue = if (i < shown) 1f else 0f,
                    animationSpec = tween(ActiveMotion.ms(AvatarMotion.CELL_MS)),
                    label = "avatarCell"
                )
                BrailleCell(dots, progress, cellW)
            }
        }
    }
}

@Composable
private fun BrailleCell(raised: List<Int>, progress: Float, width: androidx.compose.ui.unit.Dp) {
    val ink = BrandInk
    Canvas(Modifier.size(width = width, height = width * 2)) {
        val r = size.width * 0.17f
        val colX = listOf(size.width * 0.25f, size.width * 0.75f)
        val rowY = listOf(size.height * 0.15f, size.height * 0.5f, size.height * 0.85f)
        for (dot in 1..6) {
            val col = if (dot <= 3) 0 else 1
            val row = (dot - 1) % 3
            val on = dot in raised
            // Raised dots light one after another and swell a little as they arrive; the rest just fade in.
            val local = AvatarMotion.dotProgress(progress, dot)
            drawCircle(
                color = ink.copy(alpha = if (on) local else AvatarMotion.restingAlpha(progress)),
                radius = if (on) r * AvatarMotion.swell(local) else r,
                center = Offset(colX[col], rowY[row])
            )
        }
    }
}
