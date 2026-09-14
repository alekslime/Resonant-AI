package com.resonant.app.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView

/**
 * The custom gesture/audio/haptic system is the primary non-visual interface,
 * but Resonant still plays nicely with standard Android accessibility services
 * (TalkBack in particular). This gives screens a simple way to push a live
 * announcement through the platform's accessibility layer when something
 * changes that a screen-reader user should hear even if they're not using
 * Resonant's own audio system at that moment.
 */
@Composable
fun rememberAccessibilityAnnouncer(): (String) -> Unit {
    val view = LocalView.current
    return { message -> view.announceForAccessibility(message) }
}

/** Consistent content-description phrasing for quiz options, used by both the
 *  visual UI (for TalkBack) and nowhere else — the spoken system uses AudioManager. */
object AccessibilityLabels {
    fun quizOption(letter: Char, text: String, isSelected: Boolean, isFocused: Boolean): String {
        val state = when {
            isSelected -> "selected"
            isFocused -> "focused"
            else -> ""
        }
        return "Option $letter. $text. $state".trim()
    }
}
