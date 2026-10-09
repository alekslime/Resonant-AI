package com.resonant.app.ui.theme

import kotlin.math.roundToInt

/** Reading-screen text scale set by pinching. 1.0 is the normal size. Pure, so it is unit tested. */
object TextScale {
    const val MIN = 1f
    const val MAX = 2.5f
    const val STEP = 0.25f

    fun clamp(value: Float): Float = value.coerceIn(MIN, MAX)

    fun apply(current: Float, factor: Float): Float = clamp(current * factor)

    /** Which 0.25 step the scale is in. A haptic tick plays each time this changes. */
    fun stepOf(scale: Float): Int = (scale / STEP).toInt()

    fun percent(scale: Float): Int = (scale * 100).roundToInt()
}
