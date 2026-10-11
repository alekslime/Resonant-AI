package com.resonant.app.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import com.resonant.app.ui.theme.cardShape
import com.resonant.app.ui.theme.TextSpacings
import com.resonant.app.ui.theme.ActiveMotion
import com.resonant.app.ui.theme.ActiveSpacing
import com.resonant.app.ui.theme.AppFonts
import com.resonant.app.ui.theme.ActiveFont
import com.resonant.app.ui.theme.cardBorder
import com.resonant.app.ui.theme.ActivePalette
import com.resonant.app.ui.theme.Palettes
import com.resonant.app.ui.theme.ResonantOnInk
import com.resonant.app.ui.theme.ResonantCard
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resonant.app.ResonantApp
import com.resonant.app.audio.AudioManager
import com.resonant.app.content.ChatHistoryStore
import com.resonant.app.content.SemanticUnit
import com.resonant.app.core.LocalAudioManager
import com.resonant.app.core.LocalDebugState
import com.resonant.app.core.LocalHapticManager
import com.resonant.app.core.ResonantPrefs
import com.resonant.app.gestures.InteractionZone
import com.resonant.app.gestures.ResonantGesture
import com.resonant.app.gestures.SwipeDirection
import com.resonant.app.haptics.HapticPattern
import com.resonant.app.speech.whisper.WhisperModelManager
import com.resonant.app.ui.components.GestureSurface
import com.resonant.app.ui.components.ResonantScaffold
import com.resonant.app.ui.theme.BrandInk
import com.resonant.app.ui.theme.LocalResonantColors
import com.resonant.app.ui.theme.MetropolisBlack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val REPLAY_TUTORIAL = "Replay Tutorial"
private const val YOUR_NAME = "Your name"
private const val SOUND_CUES = "Sound cues"
private const val COLOR_THEME = "Color theme"
private const val FONT = "Font"
private const val TEXT_SPACING = "Text spacing"
private const val REDUCE_MOTION = "Reduce motion"
private const val VOICE_ENGINE = "Voice engine"
private const val LIVE_VOICE = "Live voice"
private const val LIVE_SPEED = "Live speed"
private const val LIVE_MIC = "Live mic"
private const val OFFLINE_VOICE_MODEL = "Offline voice model"
private const val CLEAR_HISTORY = "Clear chat history"
private const val DEBUG_MODE = "Debug Mode"

private val settingsItems = listOf(REPLAY_TUTORIAL, YOUR_NAME, SOUND_CUES, COLOR_THEME, FONT, TEXT_SPACING, REDUCE_MOTION, VOICE_ENGINE, LIVE_VOICE, LIVE_SPEED, LIVE_MIC, OFFLINE_VOICE_MODEL, CLEAR_HISTORY, DEBUG_MODE)

/** What is shown and spoken for an item. The toggle/state carries its own text, never a guess. */
private fun liveVoiceLabel(id: String) = ResonantPrefs.LIVE_VOICES.firstOrNull { it.id == id }?.label ?: id

/** Rows you can hold and swipe through, with the screen showing each option as a preview. */
private val previewable = listOf(COLOR_THEME, FONT, TEXT_SPACING)

private const val SETTINGS_HINT =
    "Swipe down with two fingers for the next item, tap the middle to choose, hold the middle on a theme, font or spacing row to preview it, or hold three fingers to hear where you are."
private const val PREVIEW_HINT =
    "Swipe left or right to preview, tap the middle to keep, or hold the right edge to cancel."

/** [start] is what was in use when the preview opened, so cancelling can put it back. */
private data class Pick(val item: String, val start: Int, val index: Int)

private fun optionCount(item: String) = when (item) {
    COLOR_THEME -> Palettes.ALL.size
    FONT -> AppFonts.ALL.size
    else -> TextSpacings.ALL.size
}

private fun optionLabel(item: String, i: Int) = when (item) {
    COLOR_THEME -> Palettes.ALL[i].label
    FONT -> AppFonts.ALL[i].label
    else -> TextSpacings.ALL[i].label
}

private fun currentOption(item: String) = when (item) {
    COLOR_THEME -> Palettes.ALL.indexOf(ActivePalette.current)
    FONT -> AppFonts.ALL.indexOf(ActiveFont.current)
    else -> TextSpacings.ALL.indexOf(ActiveSpacing.current)
}.coerceAtLeast(0)

/** Shows the option on screen. Does not save it. */
private fun showOption(item: String, i: Int) {
    when (item) {
        COLOR_THEME -> ActivePalette.current = Palettes.ALL[i]
        FONT -> ActiveFont.current = AppFonts.ALL[i]
        else -> ActiveSpacing.current = TextSpacings.ALL[i]
    }
}

private fun saveOption(prefs: ResonantPrefs, item: String, i: Int) {
    when (item) {
        COLOR_THEME -> prefs.colorTheme = Palettes.ALL[i].id
        FONT -> prefs.font = AppFonts.ALL[i].id
        else -> prefs.textSpacing = TextSpacings.ALL[i].id
    }
}

private fun liveSpeedLabel(speed: Float) = "${speed}x"

private fun labelFor(
    item: String,
    soundCuesOn: Boolean,
    voiceModelState: WhisperModelManager.State,
    liveVoiceId: String,
    liveSpeed: Float,
    liveHold: Boolean,
    engineLabel: String
) = when (item) {
    COLOR_THEME -> "$COLOR_THEME: ${ActivePalette.current.label}"
    FONT -> "$FONT: ${ActiveFont.current.label}"
    TEXT_SPACING -> "$TEXT_SPACING: ${ActiveSpacing.current.label}"
    REDUCE_MOTION -> "$REDUCE_MOTION: ${if (ActiveMotion.userReduced) "on" else "off"}"
    VOICE_ENGINE -> "$VOICE_ENGINE: $engineLabel"
    LIVE_VOICE -> "$LIVE_VOICE: ${liveVoiceLabel(liveVoiceId)}"
    LIVE_SPEED -> "$LIVE_SPEED: ${liveSpeedLabel(liveSpeed)}"
    LIVE_MIC -> "$LIVE_MIC: ${if (liveHold) "hold to talk" else "always open"}"
    SOUND_CUES -> "$SOUND_CUES: ${if (soundCuesOn) "on" else "off"}"
    OFFLINE_VOICE_MODEL -> "$OFFLINE_VOICE_MODEL: ${voiceModelStatusText(voiceModelState)}"
    else -> item
}

private fun voiceModelStatusText(state: WhisperModelManager.State): String = when (state) {
    is WhisperModelManager.State.NotDownloaded -> "not downloaded"
    is WhisperModelManager.State.Downloading -> {
        val percent = if (state.totalBytes > 0) (state.bytesDownloaded * 100 / state.totalBytes).toInt() else null
        if (percent != null) "downloading, $percent percent" else "downloading"
    }
    is WhisperModelManager.State.Ready -> "ready"
    is WhisperModelManager.State.Failed -> "download failed, tap to retry"
}

/** Coarse category only — used to decide when the voice model's line is worth re-announcing.
 *  A percentage tick during download is not; NotDownloaded -> Downloading -> Ready/Failed is. */
private fun voiceModelCategory(state: WhisperModelManager.State): String = when (state) {
    is WhisperModelManager.State.NotDownloaded -> "not_downloaded"
    is WhisperModelManager.State.Downloading -> "downloading"
    is WhisperModelManager.State.Ready -> "ready"
    is WhisperModelManager.State.Failed -> "failed"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    onOpenDebug: () -> Unit,
    onReplayTutorial: () -> Unit,
    onOpenName: () -> Unit,
    onBack: () -> Unit
) {
    val audio = LocalAudioManager.current
    val haptics = LocalHapticManager.current
    val debug = LocalDebugState.current
    val colors = LocalResonantColors.current
    val context = LocalContext.current
    val soundCues = (context.applicationContext as ResonantApp).container.soundCues
    var soundCuesOn by remember { mutableStateOf(soundCues.enabled) }
    val prefs = remember { ResonantPrefs(context) }
    var liveVoiceId by remember { mutableStateOf(prefs.liveVoice) }
    var liveSpeed by remember { mutableStateOf(prefs.liveSpeed) }
    var liveHold by remember { mutableStateOf(prefs.liveHoldToTalk) }
    var engineLabel by remember { mutableStateOf(audio.engineLabel()) }
    var index by remember { mutableIntStateOf(0) }
    var awaitingClearConfirm by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf<Pick?>(null) }
    // Lifting the finger after the hold that opened a preview also clicks the row under it.
    // Ignore that click, or the preview would be kept the moment it opened.
    var holdDown by remember { mutableStateOf(false) }
    var ignoreClickUntil by remember { mutableStateOf(0L) }
    val speedIndex by audio.speedIndex.collectAsState()
    val speed = AudioManager.SPEEDS[speedIndex]

    // Only the model manager is needed here — no need to also drag in
    // WhisperSpeechInputManager/SpeechInputManager just to reach it, so this
    // reaches into WhisperModelManager directly rather than through the
    // heavier VoiceInputController that ChatScreen uses for actual listening.
    val voiceModel = remember { WhisperModelManager(context.applicationContext) }
    val voiceModelState by voiceModel.state.collectAsState()
    val voiceModelCategory = voiceModelCategory(voiceModelState)
    // Downloads must outlive this screen (leaving Settings mid-download
    // shouldn't cancel it), so this is a plain unscoped launch tied to the
    // process, not rememberCoroutineScope() — the same reasoning as
    // VoiceInputController.downloadOfflineModel(), which this mirrors.
    val downloadScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    // Single effect: setQueue must land before collection starts, or the first
    // collected value could be the pre-queue default instead of the real start.
    LaunchedEffect(Unit) {
        debug.setScreen("Settings")
        audio.setQueue(
            settingsItems.mapIndexed { i, s -> SemanticUnit("settings_$i", labelFor(s, soundCuesOn, voiceModelState, liveVoiceId, liveSpeed, liveHold, engineLabel)) },
            startIndex = 0,
            autoAdvance = false
        )
        audio.index.collect { idx ->
            index = idx.coerceIn(0, (settingsItems.size - 1).coerceAtLeast(0))
        }
    }

    // Leaving mid-preview (system back, another screen) must not leave an unsaved option on screen.
    val pickingNow by rememberUpdatedState(picking)
    DisposableEffect(Unit) {
        onDispose { pickingNow?.let { showOption(it.item, it.start) } }
    }

    // Re-announce the voice-model line only when its category actually changes
    // (not downloaded -> downloading -> ready/failed), never on a percentage
    // tick — and only at whatever the current focus already is, so a
    // background download finishing doesn't yank focus away from wherever
    // the person actually is in this list.
    LaunchedEffect(voiceModelCategory) {
        audio.setQueue(
            settingsItems.mapIndexed { i, s -> SemanticUnit("settings_$i", labelFor(s, soundCuesOn, voiceModelState, liveVoiceId, liveSpeed, liveHold, engineLabel)) },
            startIndex = index,
            autoAdvance = false
        )
    }

    // One place for "the user chose this item", shared by the tap gesture and the on-screen
    // touch target, so the two can never disagree.
    fun activate(item: String) {
        when (item) {
            REPLAY_TUTORIAL -> onReplayTutorial()
            YOUR_NAME -> onOpenName()
            SOUND_CUES -> {
                val now = !soundCuesOn
                soundCues.enabled = now
                soundCuesOn = now
                // Re-queueing at the same position speaks the new state ("Sound cues: off").
                audio.setQueue(
                    settingsItems.mapIndexed { i, s -> SemanticUnit("settings_$i", labelFor(s, now, voiceModelState, liveVoiceId, liveSpeed, liveHold, engineLabel)) },
                    startIndex = settingsItems.indexOf(SOUND_CUES),
                    autoAdvance = false
                )
                // Turning them on: sound one, so the change is heard and not only announced.
                if (now) haptics.play(HapticPattern.CONFIRM)
            }
            COLOR_THEME -> {
                val next = Palettes.next(ActivePalette.current)
                ActivePalette.current = next
                prefs.colorTheme = next.id
                // Re-queueing at the same position speaks the new theme ("Color theme: Yellow on black").
                audio.setQueue(
                    settingsItems.mapIndexed { i, s -> SemanticUnit("settings_$i", labelFor(s, soundCuesOn, voiceModelState, liveVoiceId, liveSpeed, liveHold, engineLabel)) },
                    startIndex = settingsItems.indexOf(COLOR_THEME),
                    autoAdvance = false
                )
                haptics.play(HapticPattern.CONFIRM)
            }
            FONT -> {
                val next = AppFonts.next(ActiveFont.current)
                ActiveFont.current = next
                prefs.font = next.id
                // Re-queueing at the same position speaks the new font ("Font: Lexend").
                audio.setQueue(
                    settingsItems.mapIndexed { i, s -> SemanticUnit("settings_$i", labelFor(s, soundCuesOn, voiceModelState, liveVoiceId, liveSpeed, liveHold, engineLabel)) },
                    startIndex = settingsItems.indexOf(FONT),
                    autoAdvance = false
                )
                haptics.play(HapticPattern.CONFIRM)
            }
            TEXT_SPACING -> {
                val next = TextSpacings.next(ActiveSpacing.current)
                ActiveSpacing.current = next
                prefs.textSpacing = next.id
                audio.setQueue(
                    settingsItems.mapIndexed { i, s -> SemanticUnit("settings_$i", labelFor(s, soundCuesOn, voiceModelState, liveVoiceId, liveSpeed, liveHold, engineLabel)) },
                    startIndex = settingsItems.indexOf(TEXT_SPACING),
                    autoAdvance = false
                )
                haptics.play(HapticPattern.CONFIRM)
            }
            REDUCE_MOTION -> {
                val now = !ActiveMotion.userReduced
                ActiveMotion.userReduced = now
                prefs.reduceMotion = now
                // Re-queueing at the same position speaks the new state ("Reduce motion: on").
                audio.setQueue(
                    settingsItems.mapIndexed { i, s -> SemanticUnit("settings_$i", labelFor(s, soundCuesOn, voiceModelState, liveVoiceId, liveSpeed, liveHold, engineLabel)) },
                    startIndex = settingsItems.indexOf(REDUCE_MOTION),
                    autoAdvance = false
                )
                haptics.play(HapticPattern.CONFIRM)
            }
            VOICE_ENGINE -> {
                audio.cycleEngine()
                engineLabel = audio.engineLabel()
            }
            LIVE_VOICE -> {
                val voices = ResonantPrefs.LIVE_VOICES
                val next = voices[(voices.indexOfFirst { it.id == liveVoiceId } + 1) % voices.size].id
                prefs.liveVoice = next
                liveVoiceId = next
                audio.setQueue(
                    settingsItems.mapIndexed { i, s -> SemanticUnit("settings_$i", labelFor(s, soundCuesOn, voiceModelState, next, liveSpeed, liveHold, engineLabel)) },
                    startIndex = settingsItems.indexOf(LIVE_VOICE),
                    autoAdvance = false
                )
            }
            LIVE_SPEED -> {
                val speeds = ResonantPrefs.LIVE_SPEEDS
                val next = speeds[(speeds.indexOf(liveSpeed) + 1) % speeds.size]
                prefs.liveSpeed = next
                liveSpeed = next
                audio.setQueue(
                    settingsItems.mapIndexed { i, s -> SemanticUnit("settings_$i", labelFor(s, soundCuesOn, voiceModelState, liveVoiceId, next, liveHold, engineLabel)) },
                    startIndex = settingsItems.indexOf(LIVE_SPEED),
                    autoAdvance = false
                )
            }
            LIVE_MIC -> {
                val next = !liveHold
                prefs.liveHoldToTalk = next
                liveHold = next
                audio.setQueue(
                    settingsItems.mapIndexed { i, s -> SemanticUnit("settings_$i", labelFor(s, soundCuesOn, voiceModelState, liveVoiceId, liveSpeed, next, engineLabel)) },
                    startIndex = settingsItems.indexOf(LIVE_MIC),
                    autoAdvance = false
                )
            }
            OFFLINE_VOICE_MODEL -> when (voiceModelState) {
                is WhisperModelManager.State.NotDownloaded, is WhisperModelManager.State.Failed -> {
                    audio.announce("Downloading offline voice model. This is a one-time download of about 150 megabytes — best on Wi-Fi.")
                    downloadScope.launch { voiceModel.ensureDownloaded() }
                }
                is WhisperModelManager.State.Downloading -> {
                    audio.announce("Still ${voiceModelStatusText(voiceModelState)}.")
                }
                is WhisperModelManager.State.Ready -> {
                    audio.announce("Offline voice model is already downloaded and ready.")
                }
            }
            CLEAR_HISTORY -> {
                if (awaitingClearConfirm) {
                    downloadScope.launch(Dispatchers.IO) { ChatHistoryStore.clear(context) }
                    awaitingClearConfirm = false
                    haptics.play(HapticPattern.CONFIRM)
                    audio.announce("Chat history cleared.")
                } else {
                    awaitingClearConfirm = true
                    audio.announce("Clear all chat history? Tap again to confirm, or swipe away to cancel.")
                }
            }
            DEBUG_MODE -> onOpenDebug()
        }
    }

    fun requeueAt(item: String, behindAnnouncement: Boolean = false) {
        audio.setQueue(
            settingsItems.mapIndexed { i, s -> SemanticUnit("settings_$i", labelFor(s, soundCuesOn, voiceModelState, liveVoiceId, liveSpeed, liveHold, engineLabel)) },
            startIndex = settingsItems.indexOf(item),
            autoAdvance = false,
            queueBehindAnnouncement = behindAnnouncement
        )
    }

    fun startPick(item: String) {
        val at = currentOption(item)
        picking = Pick(item, start = at, index = at)
        holdDown = true
        awaitingClearConfirm = false
        haptics.play(HapticPattern.HOLD_ENGAGED)
        audio.announce("Preview. Swipe left or right, tap to keep, hold the right edge to cancel. " +
            OptionPicker.describe(optionLabel(item, at), at, optionCount(item)))
    }

    fun movePick(delta: Int) {
        val p = picking ?: return
        val count = optionCount(p.item)
        val to = OptionPicker.move(p.index, delta, count)
        if (to == null) {
            haptics.play(HapticPattern.EDGE)
            audio.announce((if (delta > 0) "Last. " else "First. ") + OptionPicker.describe(optionLabel(p.item, p.index), p.index, count))
            return
        }
        showOption(p.item, to)
        picking = p.copy(index = to)
        haptics.play(if (delta > 0) HapticPattern.NEXT else HapticPattern.PREVIOUS)
        audio.announce(OptionPicker.describe(optionLabel(p.item, to), to, count))
    }

    fun confirmPick() {
        val p = picking ?: return
        saveOption(prefs, p.item, p.index)
        picking = null
        haptics.play(HapticPattern.CONFIRM)
        // Re-queueing at the same row speaks the kept value ("Color theme: Warm").
        requeueAt(p.item)
    }

    fun cancelPick() {
        val p = picking ?: return
        showOption(p.item, p.start)
        picking = null
        haptics.play(HapticPattern.BACK)
        audio.announce("Cancelled.")
        requeueAt(p.item, behindAnnouncement = true)
    }

    ResonantScaffold(
        title = "Settings",
        subtitle = picking?.let { "Previewing: ${optionLabel(it.item, it.index)}. Tap to keep" } ?: "Speech speed: ${speed}x",
        onBack = { if (picking != null) cancelPick() else onBack() },
        backAnnouncement = if (picking != null) "Cancelled." else "Back to Home."
    ) {
        GestureSurface(
            twoFingerSwipe = true,
            ignoreChildTaps = true,
            hint = if (picking != null) PREVIEW_HINT else SETTINGS_HINT,
            onGesture = { gesture ->
            when (gesture) {
                is ResonantGesture.Swipe -> if (gesture.zone == InteractionZone.CENTER) {
                    if (picking != null) {
                        when (gesture.direction) {
                            SwipeDirection.RIGHT -> movePick(+1)
                            SwipeDirection.LEFT -> movePick(-1)
                            else -> {}
                        }
                    } else when (gesture.direction) {
                        SwipeDirection.UP -> if (audio.next()) { awaitingClearConfirm = false; haptics.play(HapticPattern.NEXT) }
                        SwipeDirection.DOWN -> if (audio.previous()) { awaitingClearConfirm = false; haptics.play(HapticPattern.PREVIOUS) }
                        else -> {}
                    }
                }
                is ResonantGesture.Tap -> when (gesture.zone) {
                    InteractionZone.CENTER -> {
                        if (picking != null) {
                            confirmPick()
                        } else {
                            haptics.play(HapticPattern.SELECT)
                            activate(settingsItems[index])
                        }
                    }
                    InteractionZone.LEFT_EDGE -> { audio.togglePause(); haptics.play(HapticPattern.CONFIRM) }
                    InteractionZone.RIGHT_EDGE -> {}
                }
                is ResonantGesture.DoubleTap -> if (gesture.zone == InteractionZone.LEFT_EDGE) audio.repeatCurrent()
                is ResonantGesture.LongPress -> when (gesture.zone) {
                    InteractionZone.RIGHT_EDGE -> if (picking != null) {
                        cancelPick()
                    } else {
                        awaitingClearConfirm = false
                        haptics.play(HapticPattern.BACK)
                        audio.announce("Back to Home.")
                        onBack()
                    }
                    InteractionZone.CENTER -> if (picking == null && settingsItems[index] in previewable) startPick(settingsItems[index])
                    InteractionZone.LEFT_EDGE -> {}
                }
                is ResonantGesture.LongPressEnd -> if (gesture.zone == InteractionZone.CENTER) {
                    holdDown = false
                    ignoreClickUntil = SystemClock.uptimeMillis() + 300
                }
                ResonantGesture.ThreeFingerTap -> audio.repeatCurrent()
                ResonantGesture.ThreeFingerHold -> audio.announce(
                    "Settings. Speaking at ${audio.speedLabel(speed)}. Hold the right edge and drag up to speed up, down to slow down. Currently focused: ${labelFor(settingsItems[index], soundCuesOn, voiceModelState, liveVoiceId, liveSpeed, liveHold, engineLabel)}."
                )
                ResonantGesture.HoldSpeedUp -> { if (audio.increaseSpeed()) haptics.play(HapticPattern.SPEED_UP) else haptics.play(HapticPattern.ERROR) }
                ResonantGesture.HoldSpeedDown -> { if (audio.decreaseSpeed()) haptics.play(HapticPattern.SPEED_DOWN) else haptics.play(HapticPattern.ERROR) }
                else -> {}
            }
        }) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Hold the right edge and drag up to speed up, down to slow down.",
                    style = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
                    color = BrandInk
                )
                settingsItems.forEachIndexed { i, item ->
                    val focused = i == index
                    val label = labelFor(item, soundCuesOn, voiceModelState, liveVoiceId, liveSpeed, liveHold, engineLabel)
                    // Keep the focused row on screen as focus moves down the list.
                    val inView = remember { BringIntoViewRequester() }
                    LaunchedEffect(focused) { if (focused) inView.bringIntoView() }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(inView)
                            .clip(cardShape())
                            // Focus is the same idea as Home: the current item is the inverted one.
                            .background(if (focused) BrandInk else ResonantCard)
                            .cardBorder(cardShape())
                            .clickable {
                                if (holdDown || SystemClock.uptimeMillis() < ignoreClickUntil) return@clickable
                                if (picking != null) {
                                    confirmPick()
                                    return@clickable
                                }
                                audio.jumpTo(i)
                                haptics.play(HapticPattern.SELECT)
                                activate(item)
                            }
                            .padding(horizontal = 20.dp, vertical = 22.dp)
                            .semantics { contentDescription = label + if (focused) ", focused" else "" }
                    ) {
                        Text(
                            label,
                            style = TextStyle(fontFamily = MetropolisBlack, fontWeight = FontWeight.Black, fontSize = 28.sp, lineHeight = 34.sp),
                            color = if (focused) ResonantOnInk else BrandInk
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}