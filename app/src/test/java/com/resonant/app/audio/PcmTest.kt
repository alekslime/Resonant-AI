package com.resonant.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class PcmTest {

    private fun le16(o: ByteArrayOutputStream, v: Int) { o.write(v and 0xFF); o.write((v shr 8) and 0xFF) }
    private fun le32(o: ByteArrayOutputStream, v: Int) { le16(o, v and 0xFFFF); le16(o, (v shr 16) and 0xFFFF) }

    private fun wav(samples: ShortArray, rate: Int = 22050, channels: Int = 1, bits: Int = 16, dataSize: Int? = null): ByteArray {
        val o = ByteArrayOutputStream()
        val data = samples.size * 2
        o.write("RIFF".toByteArray()); le32(o, 36 + data); o.write("WAVE".toByteArray())
        o.write("fmt ".toByteArray()); le32(o, 16); le16(o, 1); le16(o, channels); le32(o, rate)
        le32(o, rate * channels * bits / 8); le16(o, channels * bits / 8); le16(o, bits)
        o.write("data".toByteArray()); le32(o, dataSize ?: data)
        samples.forEach { le16(o, it.toInt()) }
        return o.toByteArray()
    }

    @Test fun parsesMono16() {
        val w = parseWav16(wav(shortArrayOf(1, -2, 300, -32768)))
        assertNotNull(w)
        assertEquals(22050, w!!.sampleRate)
        assertEquals(1, w.channels)
        assertEquals(listOf<Short>(1, -2, 300, -32768), w.samples.toList())
    }

    @Test fun streamingHeaderSizeMeansToEndOfFile() {
        assertEquals(4, parseWav16(wav(ShortArray(4) { 5 }, dataSize = 0))!!.samples.size)
        assertEquals(4, parseWav16(wav(ShortArray(4) { 5 }, dataSize = -1))!!.samples.size)
    }

    @Test fun rejectsNonPcm16AndGarbage() {
        assertNull(parseWav16(wav(ShortArray(4), bits = 8)))
        assertNull(parseWav16(ByteArray(10)))
        assertNull(parseWav16("not a wav file at all".toByteArray()))
    }

    @Test fun levelsAreNormalisedAndSilenceIsZero() {
        val loud = ShortArray(100) { if (it % 2 == 0) 20000 else -20000 }
        val quiet = ShortArray(100) { if (it % 2 == 0) 2000 else -2000 }
        val silent = ShortArray(100)
        val lv = windowLevels(WavPcm(1000, 1, loud + quiet + silent), 100)
        assertEquals(3, lv.size)
        assertEquals(1f, lv[0], 0.001f)      // loudest window is exactly 1
        assertTrue(lv[1] > 0f && lv[1] < lv[0])
        assertEquals(0f, lv[2], 0f)          // silence gated to 0
    }

    @Test fun quietVoiceStillReachesFullScale() {
        val quiet = ShortArray(200) { if (it % 2 == 0) 400 else -400 }
        assertEquals(1f, windowLevels(WavPcm(1000, 1, quiet), 100).max(), 0.001f)
    }
}
