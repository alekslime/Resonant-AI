package com.resonant.app.ui.theme

/** The font and text spacing a theme starts with, by id. Plain strings, so the table is unit tested. */
internal class TypeDefault(val fontId: String, val spacingId: String)

/**
 * Part of what makes a theme feel like its own place: the type it starts with. Only a starting point: the moment
 * the person picks a font or a spacing themselves, their choice wins in every theme.
 */
internal object ThemeTypeDefaults {
    private val STANDARD = TypeDefault("metropolis", "normal")

    fun forTheme(themeId: String?): TypeDefault = when (themeId) {
        // Letters made to be told apart, with room around them.
        "low_vision" -> TypeDefault("atkinson", "roomy")
        "cataracts" -> TypeDefault("atkinson", "extra_roomy")
        "handsfree" -> TypeDefault("atkinson", "normal")
        // Calm, easy reading for light-sensitive eyes and the warm paper themes.
        "photophobia" -> TypeDefault("lexend", "roomy")
        "night_red" -> TypeDefault("lexend", "normal")
        "warm_ambiance", "soft_cream" -> TypeDefault("lexend", "roomy")
        "paper_gray" -> TypeDefault("lexend", "normal")
        else -> STANDARD
    }
}
