package com.sunflower.utilityproxy.parsing

import java.net.URI
import javax.inject.Inject

/**
 * vless://uuid@host:port?params#remark — параметры (security/type/sni/fp/
 * pbk/sid/flow) основаны на публичной, стабильной схеме VLESS-ссылок
 * (пункт 10 промта). Какие из них реально поймёт наш Xray JSON generator —
 * отдельный вопрос при сборке конфига, не здесь.
 */
class VlessUriParser @Inject constructor() : ServerUriParser {
    override val scheme = "vless"

    override fun parse(uri: String): ParseResult<ParsedServer> {
        return try {
            val parsed = URI(uri)
            val uuid = parsed.userInfo
                ?: return ParseResult.Failure("vless://: отсутствует UUID перед @")
            val host = parsed.host
                ?: return ParseResult.Failure("vless://: отсутствует host")
            val port = parsed.port.takeIf { it != -1 }
                ?: return ParseResult.Failure("vless://: отсутствует port")
            val params = parseQueryParams(parsed.rawQuery)
            val remark = parsed.rawFragment?.let { urlDecode(it) } ?: host

            ParseResult.Success(
                ParsedServer(
                    name = remark,
                    protocol = "vless",
                    host = host,
                    port = port,
                    uuid = uuid,
                    security = params["security"],
                    network = params["type"],
                    sni = params["sni"],
                    fingerprint = params["fp"],
                    publicKey = params["pbk"],
                    shortId = params["sid"],
                    flow = params["flow"],
                    path = params["path"],
                    headerHost = params["host"],
                    rawUri = uri,
                ),
            )
        } catch (e: Exception) {
            ParseResult.Failure("vless://: не удалось разобрать ссылку (${e.message})")
        }
    }
}
