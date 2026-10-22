package com.resonant.app.ui.profile

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/** The numbers on a Profile line climbing from 0 to their value. Display only: speech always gets the final text. */
internal object CountUp {
    private val NUMBER = Regex("\\d+")

    /** [label] with every whole number scaled to [progress] 0..1. At 1 the label comes back untouched. */
    fun apply(label: String, progress: Float): String {
        if (progress >= 1f) return label
        val p = progress.coerceIn(0f, 1f)
        return NUMBER.replace(label) { m ->
            val n = m.value.toLongOrNull() ?: return@replace m.value
            (n * p).roundToInt().toString()
        }
    }
}

/** How a braille cell appears: its raised dots light one after another, each with a small swell. Plain numbers. */
internal object AvatarMotion {
    const val CELL_MS = 520
    const val CELL_GAP_MS = 110L
    const val DOT_STAGGER = 0.1f
    const val DOT_WINDOW = 0.5f
    const val SWELL = 0.4f

    /** 0..1 progress of dot number [dot] (1..6) when its cell is [cellProgress] of the way in. */
    fun dotProgress(cellProgress: Float, dot: Int): Float =
        ((cellProgress - (dot - 1) * DOT_STAGGER) / DOT_WINDOW).coerceIn(0f, 1f)

    /** Radius multiplier while a dot arrives: grows, then settles back to exactly 1. */
    fun swell(dotProgress: Float): Float = 1f + SWELL * sin(PI.toFloat() * dotProgress.coerceIn(0f, 1f))

    /** The faint resting dots of a cell fade in quickly at the start. */
    fun restingAlpha(cellProgress: Float): Float = 0.15f * (cellProgress * 4f).coerceIn(0f, 1f)
}
