package com.resonant.app.audio

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/** 16-bit PCM decoded from a WAV file. [samples] is interleaved when [channels] is 2. */
internal class WavPcm(val sampleRate: Int, val channels: Int, val samples: ShortArray) {
    val frames: Int get() = samples.size / channels
}

private fun le16(b: ByteArray, i: Int) = (b[i].toInt() and 0xFF) or ((b[i + 1].toInt() and 0xFF) shl 8)
private fun le32(b: ByteArray, i: Int) = le16(b, i) or (le16(b, i + 2) shl 16)

/**
 * Parses the WAV that `TextToSpeech.synthesizeToFile` writes. Only 16-bit PCM, mono or
 * stereo, is accepted — anything else returns null and the caller falls back to plain TTS.
 * Streaming writers sometimes leave the data-chunk size as 0 or 0xFFFFFFFF; both are read as
 * "to end of file".
 */
internal fun parseWav16(b: ByteArray): WavPcm? {
    if (b.size < 12) return null
    if (String(b, 0, 4, Charsets.US_ASCII) != "RIFF" || String(b, 8, 4, Charsets.US_ASCII) != "WAVE") return null
    var pos = 12
    var fmt = 0; var channels = 0; var rate = 0; var bits = 0
    while (pos + 8 <= b.size) {
        val id = String(b, pos, 4, Charsets.US_ASCII)
        var size = le32(b, pos + 4)
        val body = pos + 8
        if (id == "fmt ") {
            if (body + 16 > b.size) return null
            fmt = le16(b, body); channels = le16(b, body + 2); rate = le32(b, body + 4); bits = le16(b, body + 14)
        } else if (id == "data") {
            if ((fmt != 1 && fmt != 0xFFFE) || bits != 16 || channels !in 1..2 || rate <= 0) return null
            val avail = b.size - body
            if (size <= 0 || size > avail) size = avail
            val n = (size / 2 / channels) * channels
            if (n == 0) return null
            val samples = ShortArray(n) { ((b[body + 2 * it].toInt() and 0xFF) or (b[body + 2 * it + 1].toInt() shl 8)).toShort() }
            return WavPcm(rate, channels, samples)
        }
        if (size < 0) return null
        pos = body + size + (size and 1) // chunks are word-aligned
    }
    return null
}

/**
 * Loudness per [windowFrames]-frame window, 0..1. Normalised against this utterance's own
 * loudest window, so it is independent of the voice, the speech rate and the device volume —
 * a quiet voice still moves the dots fully. Near-silence is gated to exactly 0.
 */
internal fun windowLevels(w: WavPcm, windowFrames: Int): FloatArray {
    val n = max(1, (w.frames + windowFrames - 1) / windowFrames)
    val rms = FloatArray(n)
    for (i in 0 until n) {
        val start = i * windowFrames
        val end = min(w.frames, start + windowFrames)
        var sum = 0.0
        var count = 0
        for (f in start until end) for (c in 0 until w.channels) {
            val v = w.samples[f * w.channels + c] / 32768.0
            sum += v * v
            count++
        }
        rms[i] = if (count == 0) 0f else sqrt(sum / count).toFloat()
    }
    val peak = max(rms.max(), 0.01f)
    return FloatArray(n) {
        val r = rms[it] / peak
        if (r < .04f) 0f else r.pow(.8f)
    }
}
