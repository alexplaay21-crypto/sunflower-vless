package com.sunflower.utilityproxy.parsing

import android.util.Base64
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

/**
 * vmess://base64(JSON) — "стандартный" формат (v2rayN-style), пункт 9
 * промта: это самый распространённый вариант. Альтернативные исторические
 * схемы vmess-ссылок НЕ обрабатываются — промт прямо просит не строить
 * fake-парсер под все варианты, которые когда-либо существовали.
 */
class VmessUriParser @Inject constructor() : ServerUriParser {
    override val scheme = "vmess"

    override fun parse(uri: String): ParseResult<ParsedServer> {
        val payload = uri.removePrefix("vmess://").substringBefore("#")
        val decoded = try {
            String(Base64.decode(payload, Base64.DEFAULT))
        } catch (e: IllegalArgumentException) {
            return ParseResult.Failure("vmess://: не удалось декодировать base64 (${e.message})")
        }

        return try {
            val json = JSONObject(decoded)
            val host = json.optString("add")
            if (host.isBlank()) return ParseResult.Failure("vmess://: отсутствует поле \"add\" (host)")
            val port = json.optString("port").toIntOrNull()
                ?: return ParseResult.Failure("vmess://: некорректный или отсутствующий port")
            val uuid = json.optString("id")
            if (uuid.isBlank()) return ParseResult.Failure("vmess://: отсутствует поле \"id\" (UUID)")

            ParseResult.Success(
                ParsedServer(
                    name = json.optString("ps").ifBlank { host },
                    protocol = "vmess",
                    host = host,
                    port = port,
                    uuid = uuid,
                    alterId = json.optString("aid", "0").toIntOrNull() ?: 0,
                    network = json.optString("net").ifBlank { null },
                    security = if (json.optString("tls") == "tls") "tls" else "none",
                    sni = json.optString("sni").ifBlank { null },
                    path = json.optString("path").ifBlank { null },
                    headerHost = json.optString("host").ifBlank { null },
                    rawUri = uri,
                ),
            )
        } catch (e: JSONException) {
            ParseResult.Failure("vmess://: некорректный JSON (${e.message})")
        }
    }
}
