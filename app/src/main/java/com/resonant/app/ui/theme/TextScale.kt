package com.resonant.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt

/** What a pinch should do besides resize: which tick to play. */
enum class SizeCue { NONE, EDGE, LARGER, SMALLER }

/** The new scale after a pinch, and the cue to play for it. */
data class PinchOutcome(val scale: Float, val cue: SizeCue)

/** The app-wide text size set by pinching, 75% to 250%. 1.0 is the normal size. Pure, so it is unit tested. */
object TextScale {
    const val MIN = 0.75f
    const val MAX = 2.5f
    const val STEP = 0.25f

    fun clamp(value: Float): Float = value.coerceIn(MIN, MAX)

    fun apply(current: Float, factor: Float): Float = clamp(current * factor)

    /** Which 0.25 step the scale is in. A haptic tick plays each time this changes. */
    fun stepOf(scale: Float): Int = (scale / STEP).toInt()

    fun percent(scale: Float): Int = (scale * 100).roundToInt()

    /** The sizes offered when choosing instead of pinching (the Settings row and the setup step). */
    val PRESETS: List<Float> = listOf(0.75f, 1f, 1.25f, 1.5f, 2f, 2.5f)

    /** The preset closest to a size, so a pinched-to size still lands on one. */
    fun presetIndexOf(scale: Float): Int =
        PRESETS.indices.minByOrNull { kotlin.math.abs(PRESETS[it] - scale) } ?: 0

    fun presetLabel(index: Int): String = "${percent(PRESETS[index])} percent"

    /** The next size up from [scale], or the smallest once past the largest. Used by a tap on the Settings row. */
    fun nextPreset(scale: Float): Float = PRESETS.firstOrNull { it > scale + 0.001f } ?: PRESETS.first()

    /**
     * One movement of a pinch. [factor] is the change in finger distance since the last movement. A tick plays
     * when a new quarter step is crossed, an edge bump when the limit is reached, nothing when already there.
     */
    fun pinch(current: Float, factor: Float): PinchOutcome {
        val next = apply(current, factor)
        if (next == current) return PinchOutcome(current, SizeCue.NONE)
        val atLimit = next == MAX || next == MIN
        val before = stepOf(current)
        val after = stepOf(next)
        val cue = when {
            atLimit -> SizeCue.EDGE
            after > before -> SizeCue.LARGER
            after < before -> SizeCue.SMALLER
            else -> SizeCue.NONE
        }
        return PinchOutcome(next, cue)
    }
}

/** The text size in use right now, a snapshot state like [ActivePalette]. Everything drawn in sp follows it. */
object ActiveTextScale {
    var current: Float by mutableFloatStateOf(1f)
}

/** Applies [ActiveTextScale] on top of the system font size, for everything inside. Used once, at the theme. */
@Composable
fun TextScaleProvider(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val scale = ActiveTextScale.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * scale),
        content = content
    )
}

/**
 * Keeps the text inside from growing past [max] times normal, for the few places whose words are already huge and
 * would break their layout (the Home menu, a score). The text size still goes down as far as the person likes.
 */
@Composable
fun CappedTextScale(max: Float, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val scale = ActiveTextScale.current
    if (scale <= max) {
        content()
        return
    }
    // fontScale already holds [scale]; swap it for the cap.
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale / scale * max),
        content = content
    )
}
