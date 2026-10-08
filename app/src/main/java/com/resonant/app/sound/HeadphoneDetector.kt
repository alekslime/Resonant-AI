package com.resonant.app.sound

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings

/**
 * Whether cues should be panned: only with headphones (wired, USB or Bluetooth) connected,
 * and not when Android's own "Mono audio" setting is on, which puts both channels in both
 * ears anyway. On the phone speaker everything stays in the middle.
 */
class HeadphoneDetector(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Volatile
    private var headphones: Boolean = check()

    init {
        try {
            audioManager.registerAudioDeviceCallback(object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) { headphones = check() }
                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) { headphones = check() }
            }, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            // No updates: the value read at start stays. Cues are only ever less panned, never broken.
        }
    }

    val panAllowed: Boolean
        get() = headphones && !monoAudioOn()

    private fun check(): Boolean = try {
        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in HEADPHONE_TYPES || isBleHeadset(it) }
    } catch (e: Exception) {
        false
    }

    private fun isBleHeadset(device: AudioDeviceInfo) =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && device.type == AudioDeviceInfo.TYPE_BLE_HEADSET

    private fun monoAudioOn(): Boolean = try {
        Settings.System.getInt(appContext.contentResolver, "master_mono", 0) == 1
    } catch (e: Exception) {
        false
    }

    private companion object {
        // Hearing aids are left out on purpose: they are one-sided, so the voice would not be "in the middle".
        val HEADPHONE_TYPES = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_USB_HEADSET
        )
    }
}
