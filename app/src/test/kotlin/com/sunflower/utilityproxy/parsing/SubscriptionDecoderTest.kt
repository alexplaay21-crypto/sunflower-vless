package com.sunflower.utilityproxy.parsing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * base64-фикстура — реальный base64 трёх строк (vless, trojan и заведомо
 * неизвестная схема), вычислен и провалидирован через Python перед
 * вставкой сюда (см. чат).
 */
@RunWith(RobolectricTestRunner::class)
class SubscriptionDecoderTest {

    private val decoder = SubscriptionDecoder(
        setOf(VlessUriParser(), VmessUriParser(), TrojanUriParser(), ShadowsocksUriParser()),
    )

    private val threeLineBase64 = "dmxlc3M6Ly81NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDBAZXhhbXBsZS5jb206NDQzP3NlY3VyaXR5PW5vbmUjU2VydmVyMQp0cm9qYW46Ly9teXBhc3N3b3JkQGV4YW1wbGUuY29tOjg0NDMjU2VydmVyMgpub3QtYS1rbm93bi1zY2hlbWU6Ly9nYXJiYWdl"

    @Test
    fun `decodes base64 subscription and skips unknown scheme without failing the batch`() {
        val result = decoder.decode(threeLineBase64)

        assertTrue("expected Success but got $result", result is ParseResult.Success)
        val servers = (result as ParseResult.Success).value
        // 3 строки на входе, но одна - неизвестная схема - должна быть тихо пропущена,
        // а не завалить весь батч
        assertEquals(2, servers.size)
        assertEquals("Server1", servers[0].name)
        assertEquals("Server2", servers[1].name)
    }

    @Test
    fun `decodes plain-text (non-base64) URI list directly`() {
        val plain = "vless://550e8400-e29b-41d4-a716-446655440000@example.com:443#PlainServer"

        val result = decoder.decode(plain)

        assertTrue(result is ParseResult.Success)
        val servers = (result as ParseResult.Success).value
        assertEquals(1, servers.size)
        assertEquals("PlainServer", servers[0].name)
    }

    @Test
    fun `fails on empty input`() {
        val result = decoder.decode("")

        assertTrue(result is ParseResult.Failure)
    }

    @Test
    fun `fails on unrecognizable format`() {
        val result = decoder.decode("this is just plain prose, not a subscription at all")

        assertTrue(result is ParseResult.Failure)
    }
}
