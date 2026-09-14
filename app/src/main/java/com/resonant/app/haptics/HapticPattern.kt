package com.resonant.app.haptics

/**
 * The prototype tactile vocabulary. These are deliberately NOT final Resonant
 * haptic patterns — they're a starting vocabulary for the prototype, kept in one
 * place (see [HapticPatterns.timings]) so they're trivial to retune.
 */
enum class HapticPattern {
    CONFIRM,
    BACK,
    SELECT,
    SUBMIT,
    CORRECT,
    INCORRECT,
    ERROR,
    NEXT,
    PREVIOUS,
    SECTION_CHANGE,
    SPEED_UP,
    SPEED_DOWN,
    OPTION_A,
    OPTION_B,
    OPTION_C,
    OPTION_D
}

/**
 * Timing arrays for [android.os.VibrationEffect.createWaveform]: values alternate
 * OFF, ON, OFF, ON... starting with an initial (often zero) off-delay.
 * All durations are in milliseconds. Change values here — nowhere else references
 * raw millisecond numbers for haptics.
 */
object HapticPatterns {
    val timings: Map<HapticPattern, LongArray> = mapOf(
        HapticPattern.CONFIRM to longArrayOf(0, 50),
        HapticPattern.BACK to longArrayOf(0, 40, 70, 40),
        HapticPattern.SELECT to longArrayOf(0, 50, 90, 90),
        HapticPattern.SUBMIT to longArrayOf(0, 40, 70, 130),
        HapticPattern.CORRECT to longArrayOf(0, 40, 60, 40, 60, 40),
        HapticPattern.INCORRECT to longArrayOf(0, 220),
        HapticPattern.ERROR to longArrayOf(0, 150, 100, 150),
        HapticPattern.NEXT to longArrayOf(0, 50),
        HapticPattern.PREVIOUS to longArrayOf(0, 40, 70, 40),
        HapticPattern.SECTION_CHANGE to longArrayOf(0, 40, 70, 160, 70, 40),
        HapticPattern.SPEED_UP to longArrayOf(0, 30, 40, 45, 40, 65),
        HapticPattern.SPEED_DOWN to longArrayOf(0, 65, 40, 45, 40, 30),
        // Answer-option tactile identifiers (also configurable independently of
        // the general vocabulary above, per the spec's "make these configurable").
        HapticPattern.OPTION_A to longArrayOf(0, 50),
        HapticPattern.OPTION_B to longArrayOf(0, 40, 70, 40),
        HapticPattern.OPTION_C to longArrayOf(0, 40, 70, 160, 70, 40),
        HapticPattern.OPTION_D to longArrayOf(0, 40, 70, 40, 90, 160)
    )
}
