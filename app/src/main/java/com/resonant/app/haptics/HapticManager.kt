package com.resonant.app.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The single point of contact with the device vibrator. No other class in the
 * app should call the Vibrator APIs directly — everything goes through
 * [HapticManager.play] with a named [HapticPattern].
 */
class HapticManager(context: Context) {

    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    private val _lastPattern = MutableStateFlow<HapticPattern?>(null)
    val lastPattern: StateFlow<HapticPattern?> = _lastPattern

    fun play(pattern: HapticPattern) {
        _lastPattern.value = pattern
        val timings = HapticPatterns.timings[pattern] ?: return
        if (!vibrator.hasVibrator()) return
        val effect = VibrationEffect.createWaveform(timings, -1)
        vibrator.vibrate(effect)
    }

    /** Tactile identifier for a quiz option letter, per the configurable option map. */
    fun playOption(letterIndex: Int) {
        val pattern = when (letterIndex) {
            0 -> HapticPattern.OPTION_A
            1 -> HapticPattern.OPTION_B
            2 -> HapticPattern.OPTION_C
            else -> HapticPattern.OPTION_D
        }
        play(pattern)
    }
}
