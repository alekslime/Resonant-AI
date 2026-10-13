package com.resonant.app.livekit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class LiveErrorsTest {

    private fun token(e: Throwable) = LiveErrors.describe(ConnectPhase.TOKEN, e)
    private fun room(e: Throwable) = LiveErrors.describe(ConnectPhase.ROOM, e)

    @Test
    fun a_timeout_reaching_the_pc_says_the_pc_cannot_be_reached() {
        val e = token(SocketTimeoutException("failed to connect to /192.168.0.8 (port 8787) after 5000ms"))
        assertEquals("PC not reachable", e.short)
        assertTrue(e.spoken.contains("same Wi-Fi"))
        assertTrue(e.spoken.contains("address may have changed"))
    }

    @Test
    fun no_route_to_the_pc_is_the_same_as_a_timeout() {
        assertEquals("PC not reachable", token(NoRouteToHostException("no route")).short)
    }

    @Test
    fun a_refused_connection_says_the_servers_are_not_running() {
        val e = token(ConnectException("Connection refused"))
        assertEquals("Servers not running", e.short)
        assertTrue(e.spoken.contains("aren't running"))
    }

    @Test
    fun an_unknown_host_says_the_address_is_wrong() {
        assertEquals("Bad PC address", token(UnknownHostException("pc.local")).short)
    }

    @Test
    fun the_token_server_refusing_the_key_says_the_keys_do_not_match() {
        val e = token(TokenServerException(401))
        assertEquals("Wrong token key", e.short)
        assertTrue(e.spoken.contains("don't match"))
        assertEquals("Wrong token key", token(TokenServerException(403)).short)
    }

    @Test
    fun a_missing_token_page_says_the_address_is_wrong() {
        assertEquals("Bad token address", token(TokenServerException(404)).short)
    }

    @Test
    fun a_server_error_points_at_the_pc_terminal() {
        val e = token(TokenServerException(500))
        assertEquals("PC server error", e.short)
        assertTrue(e.spoken.contains("terminal"))
    }

    @Test
    fun the_real_error_is_found_inside_a_wrapper() {
        val wrapped = RuntimeException("failed", SocketTimeoutException("slow"))
        assertEquals("PC not reachable", token(wrapped).short)
    }

    @Test
    fun an_unknown_token_failure_is_generic() {
        val e = token(IOException("something odd"))
        assertEquals(LiveErrors.GENERIC_SHORT, e.short)
        assertEquals(LiveErrors.GENERIC_SPOKEN, e.spoken)
    }

    @Test
    fun no_internet_when_joining_the_room_says_so() {
        val e = room(UnknownHostException("resonant.livekit.cloud"))
        assertEquals("No internet", e.short)
        assertTrue(e.spoken.contains("needs internet"))
    }

    @Test
    fun a_service_that_refuses_the_token_points_at_the_pc_clock() {
        val e = room(IllegalStateException("Could not fetch region settings: 401"))
        assertEquals("Live service refused", e.short)
        assertTrue(e.spoken.contains("clock"))
    }

    @Test
    fun an_unknown_room_failure_is_generic() {
        assertEquals(LiveErrors.GENERIC_SHORT, room(IllegalStateException("odd")).short)
    }

    @Test
    fun every_message_is_short_enough_to_listen_to() {
        val all = listOf(
            token(SocketTimeoutException("x")), token(ConnectException("x")), token(UnknownHostException("x")),
            token(TokenServerException(401)), token(TokenServerException(404)), token(TokenServerException(500)),
            token(IOException("x")), room(UnknownHostException("x")), room(IllegalStateException("401")),
            room(IllegalStateException("x"))
        )
        all.forEach { assertTrue(it.spoken, it.spoken.length <= 220 && it.short.length <= 24) }
    }
}
