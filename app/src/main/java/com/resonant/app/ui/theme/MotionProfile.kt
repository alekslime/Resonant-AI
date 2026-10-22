package com.resonant.app.ui.theme

/**
 * How a theme moves and how it shows focus: its motion personality. Part of what makes a theme feel like its own
 * place and not only a color swap. Plain numbers, so they are unit tested. Reduce motion still wins over all of it.
 */
data class MotionProfile(
    val name: String,
    /** 0 = no focus ping, 1 = the standard ping. */
    val pingStrength: Float,
    /** 0 = flat background, 1 = the standard faint drift. */
    val driftStrength: Float,
    /** Multiplies the time of one drift lap. 1.5 = a third slower. */
    val driftSlowdown: Float,
    /** Springs are never bouncier than this (1 = no overshoot at all). */
    val minDamping: Float,
    /** A steady, thick ring around the focused card, in dp. 0 = none. Not motion, so it stays with Reduce motion. */
    val focusRingDp: Float
)

object MotionProfiles {
    /** The default look: full ping, full drift, light springs. */
    val STANDARD = MotionProfile("standard", pingStrength = 1f, driftStrength = 1f, driftSlowdown = 1f, minDamping = 0f, focusRingDp = 0f)

    /** Warm, slow themes: a gentler ping, a fainter and slower drift, calmer springs. */
    val GENTLE = MotionProfile("gentle", pingStrength = 0.7f, driftStrength = 0.8f, driftSlowdown = 1.5f, minDamping = 0.7f, focusRingDp = 0f)

    /** For light sensitivity: nothing flashes, spreads or drifts; springs settle without a bounce. */
    val QUIET = MotionProfile("quiet", pingStrength = 0f, driftStrength = 0f, driftSlowdown = 1f, minDamping = 0.9f, focusRingDp = 0f)

    /** For low vision and screen readers: flat contrast, no ping, and a thick steady ring shows focus. */
    val CLEAR = MotionProfile("clear", pingStrength = 0f, driftStrength = 0f, driftSlowdown = 1f, minDamping = 0.9f, focusRingDp = 4f)

    fun forTheme(id: String?): MotionProfile = when (id) {
        "photophobia", "night_red" -> QUIET
        "warm_ambiance", "soft_cream", "paper_gray" -> GENTLE
        "low_vision", "cataracts", "handsfree", "monochrome" -> CLEAR
        else -> STANDARD
    }
}
