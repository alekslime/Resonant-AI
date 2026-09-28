package com.resonant.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.SystemClock
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Plays a synthesized WAV through our own [AudioTrack] so we hold the actual PCM — that is the
 * only way to get a real loudness for the animated dots, since TextToSpeech never exposes what
 * it plays. The level is looked up from the track's playback-head position, so it lines up with
 * what is audible, not with what has merely been queued.
 *
 * [level] is safe from any thread. Callbacks are posted to [main].
 */
internal class PcmSpeaker(private val main: Handler) {

    private class Session(val wav: WavPcm, val levels: FloatArray, val window: Int) {
        @Volatile var cancelled = false
        @Volatile var started = false
        @Volatile var playedFrames = 0
        @Volatile var track: AudioTrack? = null
    }

    @Volatile private var session: Session? = null

    /**
     * Reads and deletes [file], then plays it on a background thread.
     * @return false if the file couldn't be decoded (caller should fall back to plain TTS).
     */
    fun play(file: File, onStarted: () -> Unit, onFinished: () -> Unit): Boolean {
        stop()
        val wav = try { parseWav16(file.readBytes()) } catch (e: Exception) { null }
        file.delete()
        if (wav == null) return false
        val window = max(1, wav.sampleRate * 30 / 1000) // 30 ms
        val s = Session(wav, windowLevels(wav, window), window)
        session = s
        Thread({ run(s, onStarted, onFinished) }, "resonant-pcm").start()
        return true
    }

    /** Loudness 0..1 right now, or null when nothing is playing. */
    fun level(): Float? {
        val s = session ?: return null
        if (!s.started) return null
        return s.levels[(s.playedFrames / s.window).coerceIn(0, s.levels.lastIndex)]
    }

    fun stop() {
        val s = session ?: return
        session = null
        s.cancelled = true
        // pause + flush frees the buffer, so a write() blocked on a full buffer returns.
        s.track?.let { runCatching { it.pause(); it.flush() } }
    }

    private fun run(s: Session, onStarted: () -> Unit, onFinished: () -> Unit) {
        var track: AudioTrack? = null
        var finished = false
        try {
            val wav = s.wav
            val mask = if (wav.channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
            val minBytes = AudioTrack.getMinBufferSize(wav.sampleRate, mask, AudioFormat.ENCODING_PCM_16BIT)
            val bytes = max(minBytes, wav.sampleRate * wav.channels * 2 / 10) // ~100 ms
            // USAGE_MEDIA + SPEECH: same routing/volume as the TextToSpeech default it replaces.
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(wav.sampleRate)
                        .setChannelMask(mask)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(bytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            s.track = track
            if (s.cancelled) return
            track.play()
            s.started = true
            main.post { if (!s.cancelled) onStarted() }

            val chunk = wav.sampleRate * wav.channels / 50 // 20 ms
            var off = 0
            while (off < wav.samples.size && !s.cancelled) {
                val n = min(chunk, wav.samples.size - off)
                val w = track.write(wav.samples, off, n)
                if (w < 0) return
                off += w
                s.playedFrames = track.playbackHeadPosition
            }
            // Let the buffered tail play out.
            val total = wav.frames
            val deadline = SystemClock.uptimeMillis() + total * 1000L / wav.sampleRate + 1500
            while (!s.cancelled && track.playbackHeadPosition < total && SystemClock.uptimeMillis() < deadline) {
                s.playedFrames = track.playbackHeadPosition
                Thread.sleep(10)
            }
            finished = !s.cancelled
        } catch (e: Exception) {
            // Track couldn't be built/played: end this utterance rather than hang the queue.
            finished = !s.cancelled
        } finally {
            runCatching { track?.stop() }
            runCatching { track?.release() }
            if (session === s) session = null
            if (finished) main.post { onFinished() }
        }
    }
}
