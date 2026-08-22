package com.sunflower.utilityproxy.tunnel

import com.sunflower.utilityproxy.data.local.ServerEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Строит Xray-конфиг под конкретный ServerEntity.
 *
 * ПОДТВЕРЖДЕНО поиском по официальной документации в этой сессии —
 * ВСЕ ЧЕТЫРЕ протокола используют плоскую форму settings (не vnext/users
 * и не servers[]), проверено по каждой странице outbound-протокола
 * отдельно, с датами обновления:
 *  - vless: settings.{address,port,id,encryption,flow}
 *    (xtls.github.io/en/config/outbounds/vless.html, обновлено 2 недели назад).
 *    ВАЖНО: форма с vnext[]/users[] тоже валидна — по source references
 *    core-tutorial.argsment.com (infra/conf/vless.go:251-259), упрощённая
 *    форма рерайтится во vnext внутри парсера конфига. Здесь взята
 *    плоская форма как каноническая по официальной странице.
 *  - vmess: settings.{address,port,id,security}. AlterId в актуальной
 *    документации (xtls.github.io/en/config/outbounds/vmess.html,
 *    обновлено ~месяц назад) как отдельное settings-поле НЕ фигурирует —
 *    ParsedServer/ServerEntity.alterId по-прежнему парсится и хранится
 *    (вдруг понадобится для отображения/отладки), но в outbound JSON не
 *    передаётся.
 *  - trojan: settings.{address,port,password}
 *    (xtls.github.io/en/config/outbounds/trojan.html, обновлено неделю назад).
 *    Более ранняя версия этого файла ошибочно использовала settings.servers[] —
 *    исправлено после проверки именно этой страницы, а не по аналогии с VLESS.
 *  - shadowsocks (классический): settings.{address,port,method,password}
 *    (xtls.github.io/en/config/outbounds/shadowsocks.html).
 *  - streamSettings.{network,security,tlsSettings,realitySettings,wsSettings}
 *    (XTLS/Xray-examples REALITY.ENG.md, samnet.dev Trojan+WS пример).
 *
 * ПОДТВЕРЖДЕНО (проверено отдельно, по исходнику именно tun-инбаунда —
 * github.com/XTLS/Xray-core/blob/main/proxy/tun/README.md и
 * xtls.github.io/en/config/inbounds/tun.html, а не по пересказу через
 * libXray):
 *  - Xray-core действительно имеет нативный inbound protocol="tun".
 *  - На Android/iOS сам fd передаётся НЕ через JSON-конфиг, а как
 *    настоящая переменная окружения процесса XRAY_TUN_FD (или её алиас
 *    xray.tun.fd) — это делает XrayTunnelEngine через android.system.Os
 *    прямо перед вызовом runXrayFromJson, см. его doc-комментарий.
 *    JSON здесь содержит только сам inbound-объект, без поля fd.
 *  - "tun" — низкоуровневый пакетный ридер: пакеты идут через встроенный
 *    userspace network stack и дальше через обычный Xray routing, как с
 *    любым другим инбаундом. При одном outbound и без явных routing-правил
 *    весь трафик по умолчанию идёт на единственный outbound — этого
 *    достаточно для MVP с одним сервером, без routing-блока.
 *  - gateway/dns/autoSystemRoutingTable в settings tun-инбаунда — для
 *    Linux/Windows/macOS, где Xray сам поднимает интерфейс; на Android
 *    адресацию и маршруты уже делает VpnService.Builder
 *    (SunflowerVpnService.establishTunnel), поэтому здесь не нужны.
 */
object XrayConfigBuilder {

    fun build(server: ServerEntity): String {
        val config = JSONObject().apply {
            put("log", JSONObject().put("loglevel", "warning"))
            put("inbounds", JSONArray().put(buildTunInbound()))
            put("outbounds", JSONArray().put(buildOutbound(server)))
        }
        return config.toString()
    }

    /**
     * port:0 указан намеренно — так выглядит официальный пример в
     * proxy/tun/README.md, хотя сам инбаунд его игнорирует (не слушает
     * порт). name — произвольный тег интерфейса для логов Xray, не имя
     * реального Android-устройства (тем управляет VpnService.Builder).
     */
    private fun buildTunInbound(): JSONObject = JSONObject().apply {
        put("tag", "tun-in")
        put("port", 0)
        put("protocol", "tun")
        put("settings", JSONObject().put("name", "sunflower-tun").put("mtu", 1500))
    }

    private fun buildOutbound(server: ServerEntity): JSONObject = when (server.protocol) {
        "vless" -> buildVless(server)
        "vmess" -> buildVmess(server)
        "trojan" -> buildTrojan(server)
        "ss" -> buildShadowsocks(server)
        else -> throw IllegalArgumentException("Неизвестный протокол: ${server.protocol}")
    }

    private fun buildVless(server: ServerEntity): JSONObject {
        val settings = JSONObject()
            .put("address", server.host)
            .put("port", server.port)
            .put("id", server.uuid)
            .put("encryption", "none")
        server.flow?.let { settings.put("flow", it) }
        return JSONObject().apply {
            put("protocol", "vless")
            put("settings", settings)
            put("streamSettings", buildStreamSettings(server))
        }
    }

    private fun buildVmess(server: ServerEntity): JSONObject {
        val settings = JSONObject()
            .put("address", server.host)
            .put("port", server.port)
            .put("id", server.uuid)
            .put("security", "auto")
        return JSONObject().apply {
            put("protocol", "vmess")
            put("settings", settings)
            put("streamSettings", buildStreamSettings(server))
        }
    }

    private fun buildTrojan(server: ServerEntity): JSONObject {
        val settings = JSONObject()
            .put("address", server.host)
            .put("port", server.port)
            .put("password", server.password)
        return JSONObject().apply {
            put("protocol", "trojan")
            put("settings", settings)
            put("streamSettings", buildStreamSettings(server, defaultSecurity = "tls"))
        }
    }

    private fun buildShadowsocks(server: ServerEntity): JSONObject {
        val settings = JSONObject()
            .put("address", server.host)
            .put("port", server.port)
            .put("method", server.method)
            .put("password", server.password)
        return JSONObject().apply {
            put("protocol", "shadowsocks")
            put("settings", settings)
            // Классический Shadowsocks обычно без streamSettings TLS поверх
            // себя — шифрование уже на уровне самого протокола.
        }
    }

    private fun buildStreamSettings(server: ServerEntity, defaultSecurity: String? = null): JSONObject {
        val network = server.network?.takeIf { it.isNotBlank() } ?: "tcp"
        val security = server.security?.takeIf { it.isNotBlank() } ?: defaultSecurity ?: "none"

        val stream = JSONObject().put("network", network).put("security", security)

        if (security == "tls") {
            stream.put(
                "tlsSettings",
                JSONObject().apply {
                    server.sni?.let { put("serverName", it) }
                    server.fingerprint?.let { put("fingerprint", it) }
                },
            )
        }
        if (security == "reality") {
            stream.put(
                "realitySettings",
                JSONObject().apply {
                    put("show", false)
                    server.fingerprint?.let { put("fingerprint", it) }
                    server.sni?.let { put("serverName", it) }
                    server.publicKey?.let { put("publicKey", it) }
                    server.shortId?.let { put("shortId", it) }
                },
            )
        }
        if (network == "ws") {
            stream.put(
                "wsSettings",
                JSONObject().apply {
                    server.path?.let { put("path", it) }
                    server.headerHost?.let { put("headers", JSONObject().put("Host", it)) }
                },
            )
        }
        return stream
    }
}
