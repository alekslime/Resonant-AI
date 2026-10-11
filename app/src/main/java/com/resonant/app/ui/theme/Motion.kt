package com.resonant.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * One answer to "should this animate?". True when the listener turned Reduce motion on in
 * Settings, or Android's "remove animations" is on. A snapshot state, like ActivePalette.
 */
object ActiveMotion {
    var userReduced: Boolean by mutableStateOf(false)
    var systemReduced: Boolean by mutableStateOf(false)

    val reduce: Boolean get() = userReduced || systemReduced

    /** A duration in ms that becomes 0 when motion is reduced. */
    fun ms(base: Int): Int = if (reduce) 0 else base
}
