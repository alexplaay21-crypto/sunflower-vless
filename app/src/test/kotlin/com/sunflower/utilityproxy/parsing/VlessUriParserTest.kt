package com.sunflower.utilityproxy.parsing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VlessUriParserTest {

    private val parser = VlessUriParser()

    @Test
    fun `parses full VLESS URI with all params`() {
        val uri = "vless://550e8400-e29b-41d4-a716-446655440000@example.com:443" +
            "?security=reality&type=tcp&sni=example.com&fp=chrome&pbk=PUBKEY123&sid=SID1&flow=xtls-rprx-vision" +
            "#MyServer"

        val result = parser.parse(uri)

        assertTrue(result is ParseResult.Success)
        val server = (result as ParseResult.Success).value
        assertEquals("vless", server.protocol)
        assertEquals("550e8400-e29b-41d4-a716-446655440000", server.uuid)
        assertEquals("example.com", server.host)
        assertEquals(443, server.port)
        assertEquals("reality", server.security)
        assertEquals("tcp", server.network)
        assertEquals("example.com", server.sni)
        assertEquals("chrome", server.fingerprint)
        assertEquals("PUBKEY123", server.publicKey)
        assertEquals("SID1", server.shortId)
        assertEquals("xtls-rprx-vision", server.flow)
        assertEquals("MyServer", server.name)
    }

    @Test
    fun `parses minimal VLESS URI without query params or fragment`() {
        val uri = "vless://550e8400-e29b-41d4-a716-446655440000@example.com:443"

        val result = parser.parse(uri)

        assertTrue(result is ParseResult.Success)
        val server = (result as ParseResult.Success).value
        assertEquals("example.com", server.name) // без fragment имя = host
        assertNull(server.security)
        assertNull(server.flow)
    }

    @Test
    fun `fails when UUID is missing`() {
        val result = parser.parse("vless://")

        assertTrue(result is ParseResult.Failure)
        assertTrue((result as ParseResult.Failure).reason.contains("UUID"))
    }

    @Test
    fun `fails on garbage input instead of throwing`() {
        val result = parser.parse("not a uri at all $$$ ///")

        assertTrue(result is ParseResult.Failure)
    }
}
