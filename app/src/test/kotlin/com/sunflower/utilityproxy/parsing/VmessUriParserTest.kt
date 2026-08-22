package com.sunflower.utilityproxy.parsing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Все base64-фикстуры вычислены и провалидированы round-trip'ом через
 * Python перед вставкой сюда (см. чат), не набраны руками — один из
 * первых черновиков поймал именно на этом расхождение (пробелы в JSON
 * дают другой base64, хоть и семантически эквивалентный).
 */
@RunWith(RobolectricTestRunner::class)
class VmessUriParserTest {

    private val parser = VmessUriParser()

    private val validPayload = "eyJ2IjogIjIiLCAicHMiOiAiTXlWbWVzcyIsICJhZGQiOiAiZXhhbXBsZS5jb20iLCAicG9ydCI6ICI0NDMiLCAiaWQiOiAiNTUwZTg0MDAtZTI5Yi00MWQ0LWE3MTYtNDQ2NjU1NDQwMDAwIiwgImFpZCI6ICIwIiwgIm5ldCI6ICJ3cyIsICJ0eXBlIjogIm5vbmUiLCAiaG9zdCI6ICJleGFtcGxlLmNvbSIsICJwYXRoIjogIi93cyIsICJ0bHMiOiAidGxzIiwgInNuaSI6ICJleGFtcGxlLmNvbSJ9"

    @Test
    fun `parses valid VMess base64-JSON payload`() {
        val result = parser.parse("vmess://$validPayload")

        assertTrue("expected Success but got $result", result is ParseResult.Success)
        val server = (result as ParseResult.Success).value
        assertEquals("vmess", server.protocol)
        assertEquals("MyVmess", server.name)
        assertEquals("example.com", server.host)
        assertEquals(443, server.port)
        assertEquals("550e8400-e29b-41d4-a716-446655440000", server.uuid)
        assertEquals("ws", server.network)
        assertEquals("tls", server.security)
        assertEquals("/ws", server.path)
        assertEquals("example.com", server.headerHost)
        assertEquals("example.com", server.sni)
    }

    @Test
    fun `fails on invalid base64`() {
        val result = parser.parse("vmess://not-valid-base64-!!!")

        assertTrue(result is ParseResult.Failure)
    }

    @Test
    fun `fails on valid base64 that is not JSON`() {
        // base64("hello world") — декодируется без ошибки, но это не JSON
        val result = parser.parse("vmess://aGVsbG8gd29ybGQ=")

        assertTrue(result is ParseResult.Failure)
    }

    @Test
    fun `fails when required id field is missing`() {
        // base64({"add": "example.com", "port": "443"}) — валидный JSON, но без "id"
        val result = parser.parse("vmess://eyJhZGQiOiAiZXhhbXBsZS5jb20iLCAicG9ydCI6ICI0NDMifQ==")

        assertTrue(result is ParseResult.Failure)
    }
}
