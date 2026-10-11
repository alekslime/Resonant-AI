package com.resonant.app.ui.settings

/** The pure parts of the Settings preview (hold a row, swipe through its options). Unit tested. */
internal object OptionPicker {

    /** The option [delta] steps from [index], or null at either end of the list (no wrapping). */
    fun move(index: Int, delta: Int, count: Int): Int? {
        val to = index + delta
        return if (count <= 0 || to < 0 || to >= count) null else to
    }

    /** What is spoken for the option now showing, e.g. "Warm, 7 of 12". */
    fun describe(label: String, index: Int, count: Int): String = "$label, ${index + 1} of $count"
}
