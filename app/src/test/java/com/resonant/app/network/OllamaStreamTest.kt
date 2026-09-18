package com.resonant.app.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class OllamaStreamTest {

    @Test
    fun parses_a_content_delta() {
        val chunk = parseStreamLine(
            """{"model":"llama3.2","message":{"role":"assistant","content":"Hel"},"done":false}"""
        )!!
        assertEquals("Hel", chunk.delta)
        assertFalse(chunk.done)
    }

    @Test
    fun parses_the_final_line() {
        val chunk = parseStreamLine("""{"message":{"role":"assistant","content":""},"done":true}""")!!
        assertEquals("", chunk.delta)
        assertTrue(chunk.done)
    }

    @Test
    fun a_line_without_a_message_is_an_empty_delta() {
        val chunk = parseStreamLine("""{"done":true}""")!!
        assertEquals("", chunk.delta)
        assertTrue(chunk.done)
    }

    @Test
    fun blank_lines_are_skipped() {
        assertNull(parseStreamLine(""))
        assertNull(parseStreamLine("   "))
    }

    @Test
    fun a_mid_stream_error_becomes_an_exception() {
        try {
            parseStreamLine("""{"error":"model 'nope' not found"}""")
            fail("expected an IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("not found"))
        }
    }
}
