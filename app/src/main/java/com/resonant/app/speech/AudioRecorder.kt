package com.resonant.app.speech

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlin.math.sqrt

/**
 * One-shot 16kHz mono PCM capture, formatted the way Whisper expects it:
 * float32 samples in [-1, 1] at exactly 16kHz.
 *
 * Auto-stops [SILENCE_TIMEOUT_MS] after speech trails off, or after
 * [MAX_DURATION_MS] regardless — a manual "tap again to stop" isn't
 * realistic for someone who is using this specifically because tapping
 * around a screen isn't their primary way of interacting with a phone.
 *
 * Blocking — call from a background thread/dispatcher, never the main one.
 */
class AudioRecorder {

    class Result(val samples: FloatArray, val sampleRate: Int)

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun record(): Result {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        require(minBufferSize > 0) { "This device can't record at ${SAMPLE_RATE}Hz." }

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize * 2
        )

        val readBuffer = ShortArray(minBufferSize)
        // ~8s of headroom for a typical short question; grows past that fine, just reallocates.
        val collected = ArrayList<Short>(SAMPLE_RATE * 8)

        var hasSpokenYet = false
        var silenceStartMs = -1L
        val startMs = System.currentTimeMillis()

        try {
            recorder.startRecording()
            while (System.currentTimeMillis() - startMs < MAX_DURATION_MS) {
                val read = recorder.read(readBuffer, 0, readBuffer.size)
                if (read <= 0) continue

                for (i in 0 until read) collected.add(readBuffer[i])

                val now = System.currentTimeMillis()
                if (rms(readBuffer, read) > SILENCE_RMS_THRESHOLD) {
                    hasSpokenYet = true
                    silenceStartMs = -1L
                } else if (hasSpokenYet) {
                    if (silenceStartMs < 0) silenceStartMs = now
                    if (now - silenceStartMs > SILENCE_TIMEOUT_MS) break
                }
            }
        } finally {
            recorder.stop()
            recorder.release()
        }

        val floats = FloatArray(collected.size) { i -> collected[i] / 32768f }
        return Result(floats, SAMPLE_RATE)
    }

    private fun rms(buffer: ShortArray, length: Int): Double {
        var sum = 0.0
        for (i in 0 until length) sum += buffer[i].toDouble() * buffer[i].toDouble()
        return sqrt(sum / length)
    }

    companion object {
        private const val SAMPLE_RATE = 16_000
        // Tuned by ear, not measurement — the honest starting point for a mic
        // opened right after "Listening." finishes speaking in a normal room.
        // Expect to adjust once this runs on a real device.
        private const val SILENCE_RMS_THRESHOLD = 500.0
        private const val SILENCE_TIMEOUT_MS = 1_200L
        private const val MAX_DURATION_MS = 15_000L
    }
}
