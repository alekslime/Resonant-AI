package com.resonant.app.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import com.resonant.app.core.ResonantPrefs
import com.resonant.app.haptics.HapticPattern

/**
 * Plays the [SoundCues] vocabulary. [com.resonant.app.haptics.HapticManager] calls
 * [play] for every haptic pattern, so any screen that already buzzes also sounds,
 * with no per-screen wiring.
 *
 * Design choices worth knowing about:
 *
 *  - No audio focus is requested. Cues are a few tens of milliseconds; taking focus
 *    would make the speech engine duck or stop for a beep.
 *  - Played on the media volume with a "sonification" content type, so the same volume
 *    keys that set the speech level set the cue level, and a presenter can turn both
 *    down together.
 *  - Silent while the app is not on screen ([inForeground]). Chat's "thinking" tick
 *    loops every couple of seconds and would otherwise keep ticking over another app.
 *  - Nothing here may ever crash the app: a failed cue is just a missing beep.
 */
class SoundCueManager(private val prefs: ResonantPrefs) {

    /** User setting, saved. Defaults to on. */
    var enabled: Boolean = prefs.soundCuesEnabled
        set(value) {
            field = value
            prefs.soundCuesEnabled = value
        }

    /** Set by the activity's onStart / onStop. False until the first onStart. */
    @Volatile
    var inForeground: Boolean = false

    private val handler = Handler(Looper.getMainLooper())

    // Rendered once. The set is tiny (a few thousand samples per cue), so this costs
    // milliseconds at startup and means the first cue of a session is as fast as any other.
    private val rendered: Map<HapticPattern, Rendered> =
        SoundCues.specs.mapValues { (_, spec) -> Rendered(ToneSynth.render(spec), ToneSynth.durationMs(spec)) }

    private class Rendered(val samples: ShortArray, val durationMs: Int)

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val format = AudioFormat.Builder()
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setSampleRate(ToneSynth.SAMPLE_RATE)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()

    fun play(pattern: HapticPattern) {
        if (!enabled || !inForeground) return
        val cue = rendered[pattern] ?: return
        try {
            // A fresh static track per cue: simpler and sturdier than reusing one, where
            // a cue fired mid-playback of the last would need stop/reload/replay handling.
            val track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(cue.samples.size * Short.SIZE_BYTES)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(cue.samples, 0, cue.samples.size)
            track.play()
            // Released shortly after it has finished. The slack covers start-up latency.
            handler.postDelayed({ releaseQuietly(track) }, (cue.durationMs + RELEASE_SLACK_MS).toLong())
        } catch (e: Exception) {
            // IllegalStateException / UnsupportedOperationException on odd audio stacks. A missed
            // cue is harmless; a crash in the middle of a gesture is not.
        }
    }

    private fun releaseQuietly(track: AudioTrack) {
        try {
            track.release()
        } catch (e: Exception) {
            // already released
        }
    }

    private companion object {
        const val RELEASE_SLACK_MS = 300
    }
}
