package com.sunflower.utilityproxy.parsing

import java.net.URI
import javax.inject.Inject

/** trojan://password@host:port?params#remark — пункт 11 промта. */
class TrojanUriParser @Inject constructor() : ServerUriParser {
    override val scheme = "trojan"

    override fun parse(uri: String): ParseResult<ParsedServer> {
        return try {
            val parsed = URI(uri)
            val password = parsed.userInfo
                ?: return ParseResult.Failure("trojan://: отсутствует пароль перед @")
            val host = parsed.host
                ?: return ParseResult.Failure("trojan://: отсутствует host")
            val port = parsed.port.takeIf { it != -1 }
                ?: return ParseResult.Failure("trojan://: отсутствует port")
            val params = parseQueryParams(parsed.rawQuery)
            val remark = parsed.rawFragment?.let { urlDecode(it) } ?: host

            ParseResult.Success(
                ParsedServer(
                    name = remark,
                    protocol = "trojan",
                    host = host,
                    port = port,
                    password = password,
                    security = params["security"] ?: "tls",
                    network = params["type"],
                    sni = params["sni"],
                    rawUri = uri,
                ),
            )
        } catch (e: Exception) {
            ParseResult.Failure("trojan://: не удалось разобрать ссылку (${e.message})")
        }
    }
}
