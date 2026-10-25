package com.resonant.app.livekit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class LiveErrorsHostedTest {

    private fun token(e: Throwable) = LiveErrors.describe(ConnectPhase.TOKEN, e, hosted = true)
    private fun room(e: Throwable) = LiveErrors.describe(ConnectPhase.ROOM, e, hosted = true)

    @Test
    fun no_network_says_the_phone_is_offline_whatever_the_cause() {
        for (e in listOf(SocketTimeoutException("t"), ConnectException("c"), UnknownHostException("h"))) {
            assertEquals("No internet", token(e).short)
            assertEquals("No internet", room(e).short)
        }
        assertTrue(token(SocketTimeoutException("t")).spoken.contains("phone is online"))
    }

    @Test
    fun a_refused_key_says_to_update_the_app_not_to_fix_a_key() {
        val e = token(TokenServerException(401))
        assertEquals("Not allowed", e.short)
        assertTrue(e.spoken.contains("Update the app"))
        assertEquals("Not allowed", token(TokenServerException(403)).short)
    }

    @Test
    fun a_moved_server_and_a_server_problem_are_told_apart() {
        assertEquals("Update needed", token(TokenServerException(404)).short)
        val e = token(TokenServerException(503))
        assertEquals("Server problem", e.short)
        assertTrue(e.spoken.contains("try again in a few minutes"))
    }

    @Test
    fun a_refused_live_service_says_to_try_again_later() {
        val e = room(RuntimeException("401 Unauthorized"))
        assertEquals("Live service refused", e.short)
        assertTrue(e.spoken.contains("few minutes"))
    }

    @Test
    fun anything_else_gets_the_plain_generic_line() {
        assertEquals(LiveErrors.HOSTED_GENERIC_SPOKEN, token(RuntimeException("?")).spoken)
        assertEquals(LiveErrors.GENERIC_SHORT, room(RuntimeException("?")).short)
    }

    @Test
    fun a_hosted_message_never_mentions_a_pc_an_address_a_key_or_wifi() {
        val all = listOf(
            token(SocketTimeoutException("t")), token(TokenServerException(401)), token(TokenServerException(404)),
            token(TokenServerException(500)), token(RuntimeException("?")), room(RuntimeException("401")),
            room(ConnectException("c")), room(RuntimeException("?"))
        )
        for (e in all) {
            val text = (e.spoken + " " + e.short).lowercase()
            for (word in listOf("pc", "computer", "address", "key", "wi-fi", "server setup", "terminal")) {
                assertFalse("\"$word\" in: $text", Regex("\\b" + Regex.escape(word) + "\\b").containsMatchIn(text))
            }
        }
    }

    @Test
    fun the_default_is_still_the_own_pc_wording() {
        assertEquals("PC not reachable", LiveErrors.describe(ConnectPhase.TOKEN, SocketTimeoutException("t")).short)
    }

    @Test
    fun the_other_lines_stop_naming_the_pc_when_hosted() {
        for (text in listOf(
            LiveWording.agentMissingSpoken(true), LiveWording.lostSpoken(true), LiveWording.notConnectedSpoken(true),
            LiveWording.statusAgentMissing(true), LiveWording.statusConnecting(true)
        )) assertFalse(text, text.contains("PC"))
        assertTrue(LiveWording.lostSpoken(false).contains("PC"))
        assertTrue(LiveWording.statusConnecting(false).contains("your PC"))
    }
}
