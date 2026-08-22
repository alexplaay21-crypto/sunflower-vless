package com.sunflower.utilityproxy.parsing

import android.util.Base64
import java.net.URI
import javax.inject.Inject

/**
 * ss://base64(method:password)@host:port#remark — формат SIP002, актуальный
 * стандарт (пункт 12 промта). Старый полностью закодированный вариант
 * ss://base64(method:password@host:port) НЕ обрабатываем: промт прямо просит
 * поддерживать то, что реально используется, а не все исторические варианты.
 */
class ShadowsocksUriParser @Inject constructor() : ServerUriParser {
    override val scheme = "ss"

    override fun parse(uri: String): ParseResult<ParsedServer> {
        return try {
            val parsed = URI(uri)
            val host = parsed.host
                ?: return ParseResult.Failure("ss://: отсутствует host (поддерживается только формат SIP002)")
            val port = parsed.port.takeIf { it != -1 }
                ?: return ParseResult.Failure("ss://: отсутствует port")
            val userInfo = parsed.userInfo
                ?: return ParseResult.Failure("ss://: отсутствует method:password перед @")

            val decodedUserInfo = decodeUserInfo(userInfo)
                ?: return ParseResult.Failure("ss://: не удалось декодировать method:password")
            val sepIdx = decodedUserInfo.indexOf(":")
            if (sepIdx == -1) {
                return ParseResult.Failure("ss://: не удалось разобрать method:password")
            }
            val method = decodedUserInfo.substring(0, sepIdx)
            val password = decodedUserInfo.substring(sepIdx + 1)
            val remark = parsed.rawFragment?.let { urlDecode(it) } ?: host

            ParseResult.Success(
                ParsedServer(
                    name = remark,
                    protocol = "ss",
                    host = host,
                    port = port,
                    method = method,
                    password = password,
                    rawUri = uri,
                ),
            )
        } catch (e: Exception) {
            ParseResult.Failure("ss://: не удалось разобрать ссылку (${e.message})")
        }
    }

    /** Пробуем URL-safe и обычный Base64 — реальные генераторы ссылок используют оба варианта. */
    private fun decodeUserInfo(userInfo: String): String? {
        val variants = listOf(
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
            Base64.DEFAULT or Base64.NO_PADDING,
            Base64.DEFAULT,
        )
        for (flags in variants) {
            try {
                return String(Base64.decode(userInfo, flags))
            } catch (_: IllegalArgumentException) {
                // пробуем следующий вариант
            }
        }
        return null
    }
}
