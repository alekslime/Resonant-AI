package com.resonant.app.sound

import com.resonant.app.haptics.HapticPattern

/**
 * Where each cue sits between the ears when headphones are on. The voice is never moved: it
 * stays in the middle, and every cue sits a little to one side of it, so a chime is not
 * heard as part of the sentence being spoken.
 *
 * One rule: back, slower and wrong go left; everything else goes right. Changing a cue's
 * side means editing [LEFT] and nothing else.
 *
 * "A little" is deliberate. The far ear is turned down to 65%, not muted, so someone wearing
 * a single earbud, or hearing better on one side, still hears every cue (about 4 dB quieter
 * at worst). [OFFSET] is the one number to retune by ear.
 */
object CuePan {

    /** How far off centre, 0..1. 0 is the middle, 1 would be one ear only. */
    const val OFFSET = 0.35f

    private val LEFT = setOf(
        HapticPattern.PREVIOUS,
        HapticPattern.BACK,
        HapticPattern.SPEED_DOWN,
        HapticPattern.INCORRECT,
        HapticPattern.ERROR
    )

    /** -OFFSET (left) or +OFFSET (right). Never 0: the middle belongs to the voice. */
    fun panFor(pattern: HapticPattern): Float = if (pattern in LEFT) -OFFSET else OFFSET

    /** Left and right gain for a pan in -1..1. The near ear stays at full level. */
    fun gains(pan: Float): Pair<Float, Float> {
        val p = pan.coerceIn(-1f, 1f)
        return Pair(1f - maxOf(p, 0f), 1f - maxOf(-p, 0f))
    }
}
