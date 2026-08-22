package com.sunflower.utilityproxy.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Пункт 33 промта: логи приложения. In-memory, не logcat — так проще
 * гарантировать редактирование чувствительных данных (пункт 33: НИКОГДА
 * не показывать PrivateKey/пароли/токены/HWID/cookies с credentials),
 * потому что мы полностью контролируем, что в буфер попадает и как
 * фильтруется, а не парсим системный logcat постфактум.
 */
object AppLogger {

    data class Entry(val timestamp: Long, val tag: String, val message: String)

    private const val MAX_ENTRIES = 500
    private val sensitiveKeys = listOf(
        "password", "token", "secretkey", "privatekey", "authorization", "cookie",
    )

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    fun log(tag: String, message: String) {
        val entry = Entry(System.currentTimeMillis(), tag, redact(message))
        _entries.update { (it + entry).takeLast(MAX_ENTRIES) }
    }

    fun clear() {
        _entries.value = emptyList()
    }

    fun exportText(): String = _entries.value.joinToString("\n") { "${it.timestamp} [${it.tag}] ${it.message}" }

    private fun redact(message: String): String {
        var result = message
        for (key in sensitiveKeys) {
            result = Regex("(?i)($key)([\"']?\\s*[:=]\\s*[\"']?)([^\\s,\"'}]+)").replace(result) { m ->
                "${m.groupValues[1]}${m.groupValues[2]}***"
            }
        }
        return result
    }
}
