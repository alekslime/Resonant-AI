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
    CORRECT,
    INCORRECT,
    ERROR,
    NEXT,
    PREVIOUS,
    SECTION_CHANGE,
    SPEED_UP,
    SPEED_DOWN,
    LISTENING,

    /** Right-edge hold has crossed the threshold; dragging now adjusts speed. */
    HOLD_ENGAGED,

    /** Soft repeating pulse while waiting on the AI, so silence never feels like a freeze. */
    THINKING,

    /** Bumped the start or end of a list — there is nothing further that way. */
    EDGE,
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
        HapticPattern.CORRECT to longArrayOf(0, 40, 60, 40, 60, 40),
        HapticPattern.INCORRECT to longArrayOf(0, 220),
        HapticPattern.ERROR to longArrayOf(0, 150, 100, 150),
        HapticPattern.NEXT to longArrayOf(0, 50),
        HapticPattern.PREVIOUS to longArrayOf(0, 40, 70, 40),
        HapticPattern.SECTION_CHANGE to longArrayOf(0, 40, 70, 160, 70, 40),
        HapticPattern.SPEED_UP to longArrayOf(0, 30, 40, 45, 40, 65),
        HapticPattern.SPEED_DOWN to longArrayOf(0, 65, 40, 45, 40, 30),
        HapticPattern.LISTENING to longArrayOf(0, 30, 60, 30),
        HapticPattern.HOLD_ENGAGED to longArrayOf(0, 15, 30, 15, 30, 15),
        HapticPattern.THINKING to longArrayOf(0, 12),
        HapticPattern.EDGE to longArrayOf(0, 20, 50, 20),
        // Answer-option tactile identifiers (also configurable independently of
        // the general vocabulary above, per the spec's "make these configurable").
        // Deliberately a separate family from every navigation pattern: LONG pulses
        // (90 ms) counted A=1 ... D=4. The old set reused the NEXT / PREVIOUS /
        // SECTION_CHANGE timings, so in the quiz "I moved" and "this is option B"
        // were the same buzz. HapticPatternsTest guards against that coming back.
        HapticPattern.OPTION_A to longArrayOf(0, 90),
        HapticPattern.OPTION_B to longArrayOf(0, 90, 80, 90),
        HapticPattern.OPTION_C to longArrayOf(0, 90, 80, 90, 80, 90),
        HapticPattern.OPTION_D to longArrayOf(0, 90, 80, 90, 80, 90, 80, 90)
    )
}
