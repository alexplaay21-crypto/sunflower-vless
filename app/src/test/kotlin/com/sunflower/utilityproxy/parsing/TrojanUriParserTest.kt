package com.sunflower.utilityproxy.parsing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrojanUriParserTest {

    private val parser = TrojanUriParser()

    @Test
    fun `parses full Trojan URI`() {
        val uri = "trojan://mypassword@example.com:443?sni=example.com&type=tcp#MyTrojan"

        val result = parser.parse(uri)

        assertTrue(result is ParseResult.Success)
        val server = (result as ParseResult.Success).value
        assertEquals("trojan", server.protocol)
        assertEquals("mypassword", server.password)
        assertEquals("example.com", server.host)
        assertEquals(443, server.port)
        assertEquals("example.com", server.sni)
        assertEquals("tcp", server.network)
        assertEquals("MyTrojan", server.name)
    }

    @Test
    fun `defaults security to tls when not specified`() {
        val uri = "trojan://mypassword@example.com:443"

        val result = parser.parse(uri)

        assertTrue(result is ParseResult.Success)
        assertEquals("tls", (result as ParseResult.Success).value.security)
    }

    @Test
    fun `fails when password is missing`() {
        val result = parser.parse("trojan://")

        assertTrue(result is ParseResult.Failure)
        assertTrue((result as ParseResult.Failure).reason.contains("пароль"))
    }
}
