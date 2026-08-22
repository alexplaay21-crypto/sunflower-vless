package com.sunflower.utilityproxy.parsing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Фикстуры base64 вычислены и провалидированы round-trip'ом через Python
 * перед вставкой сюда (см. чат) — не набраны руками, чтобы не поймать
 * опечатку в base64, которая тихо сломает тест.
 */
@RunWith(RobolectricTestRunner::class)
class ShadowsocksUriParserTest {

    private val parser = ShadowsocksUriParser()

    // base64url(no padding) от "aes-256-gcm:test123"
    private val userInfoUrlSafe = "YWVzLTI1Ni1nY206dGVzdDEyMw"

    // тот же payload, но стандартный base64 с паддингом — проверяет,
    // что decodeUserInfo() реально перебирает оба варианта, а не только один
    private val userInfoStandardPadded = "YWVzLTI1Ni1nY206dGVzdDEyMw=="

    @Test
    fun `parses SIP002 URI with URL-safe base64 userinfo`() {
        val uri = "ss://$userInfoUrlSafe@example.com:8388#MyShadowsocks"

        val result = parser.parse(uri)

        assertTrue(result is ParseResult.Success)
        val server = (result as ParseResult.Success).value
        assertEquals("ss", server.protocol)
        assertEquals("aes-256-gcm", server.method)
        assertEquals("test123", server.password)
        assertEquals("example.com", server.host)
        assertEquals(8388, server.port)
        assertEquals("MyShadowsocks", server.name)
    }

    @Test
    fun `parses SIP002 URI with standard padded base64 userinfo too`() {
        // URI-компонент userinfo не может содержать "=" без percent-encoding,
        // поэтому кодируем его для этого теста, как это сделал бы реальный
        // генератор ссылок, использующий стандартный (не url-safe) base64.
        val encodedUserInfo = java.net.URLEncoder.encode(userInfoStandardPadded, "UTF-8")
        val uri = "ss://$encodedUserInfo@example.com:8388"

        val result = parser.parse(uri)

        assertTrue("expected Success but got $result", result is ParseResult.Success)
        val server = (result as ParseResult.Success).value
        assertEquals("aes-256-gcm", server.method)
        assertEquals("test123", server.password)
    }

    @Test
    fun `fails on invalid base64 userinfo`() {
        val result = parser.parse("ss://%%%not-base64%%%@example.com:8388")

        assertTrue(result is ParseResult.Failure)
    }

    @Test
    fun `fails when host is missing`() {
        val result = parser.parse("ss://$userInfoUrlSafe@")

        assertTrue(result is ParseResult.Failure)
    }
}
