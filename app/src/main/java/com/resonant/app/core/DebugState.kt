package com.resonant.app.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A small, centralized place to record "what just happened" so the Debug screen
 * (Settings > Debug Mode) can show it. Screens and the gesture layer report into
 * this; nothing reads raw system state directly for debug purposes.
 */
class DebugState {
    private val _currentScreen = MutableStateFlow("—")
    val currentScreen: StateFlow<String> = _currentScreen

    private val _currentZone = MutableStateFlow("—")
    val currentZone: StateFlow<String> = _currentZone

    private val _lastGesture = MutableStateFlow("—")
    val lastGesture: StateFlow<String> = _lastGesture

    private val _lastHaptic = MutableStateFlow("—")
    val lastHaptic: StateFlow<String> = _lastHaptic

    private val _selectedOption = MutableStateFlow("—")
    val selectedOption: StateFlow<String> = _selectedOption

    // Dots preview (Chat): forces a dots animation for on-device checking. In-memory only, so it
    // resets on every app start. State is held by name to keep core free of UI types.
    private val _dotsPreview = MutableStateFlow(false)
    val dotsPreview: StateFlow<Boolean> = _dotsPreview
    private val _dotsForced = MutableStateFlow<String?>(null)
    val dotsForced: StateFlow<String?> = _dotsForced
    private val _dotsLevel = MutableStateFlow<Float?>(null)
    val dotsLevel: StateFlow<Float?> = _dotsLevel

    fun toggleDotsPreview(): Boolean {
        val on = !_dotsPreview.value
        _dotsPreview.value = on
        if (!on) { _dotsForced.value = null; _dotsLevel.value = null }
        return on
    }
    fun forceDots(name: String?) { _dotsForced.value = name }
    fun setDotsLevel(level: Float?) { _dotsLevel.value = level }

    fun setScreen(name: String) { _currentScreen.value = name }
    fun setZone(name: String) { _currentZone.value = name }
    fun setGesture(name: String) { _lastGesture.value = name }
    fun setHaptic(name: String) { _lastHaptic.value = name }
    fun setSelectedOption(label: String) { _selectedOption.value = label }
}
