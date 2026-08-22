package com.sunflower.utilityproxy.parsing

import android.util.Base64
import javax.inject.Inject

private val KNOWN_SCHEMES = setOf("vless", "vmess", "trojan", "ss")

/**
 * Определяет формат ответа подписки (пункты 7/8 промта: Base64 / plain text /
 * список ссылок) и прогоняет каждую строку через нужный парсер по схеме.
 * Сырой Xray JSON-конфиг ({"inbounds":...,"outbounds":...}) сюда не
 * относится — это отдельная вкладка "JSON" при ручном добавлении
 * (пункт 5 промта), а не формат подписки.
 */
class SubscriptionDecoder @Inject constructor(
    private val parsers: Set<@JvmSuppressWildcards ServerUriParser>,
) {
    fun decode(rawBody: String): ParseResult<List<ParsedServer>> {
        val trimmed = rawBody.trim()
        if (trimmed.isEmpty()) {
            return ParseResult.Failure("Пустой ответ подписки.")
        }

        val text = if (looksLikeUriList(trimmed)) trimmed else decodeBase64(trimmed) ?: trimmed

        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.none { looksLikeUriList(it) }) {
            return ParseResult.Failure("Неизвестный формат подписки: ни Base64, ни список ссылок.")
        }

        val parsersByScheme = parsers.associateBy { it.scheme }
        val results = lines.mapNotNull { line ->
            val scheme = line.substringBefore("://", missingDelimiterValue = "")
            val parser = parsersByScheme[scheme] ?: return@mapNotNull null
            when (val result = parser.parse(line)) {
                is ParseResult.Success -> result.value
                is ParseResult.Failure -> null // одна битая строка не должна ронять всю подписку
            }
        }

        return ParseResult.Success(results)
    }

    private fun looksLikeUriList(text: String): Boolean =
        text.lineSequence().any { line ->
            line.trim().substringBefore("://", missingDelimiterValue = "") in KNOWN_SCHEMES
        }

    private fun decodeBase64(text: String): String? = try {
        String(Base64.decode(text, Base64.DEFAULT))
    } catch (e: IllegalArgumentException) {
        null
    }
}
