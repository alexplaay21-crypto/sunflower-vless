package com.sunflower.utilityproxy.parsing

import java.net.URLDecoder

internal fun parseQueryParams(rawQuery: String?): Map<String, String> {
    if (rawQuery.isNullOrEmpty()) return emptyMap()
    return rawQuery.split("&")
        .mapNotNull { pair ->
            val idx = pair.indexOf("=")
            if (idx == -1) null else urlDecode(pair.substring(0, idx)) to urlDecode(pair.substring(idx + 1))
        }
        .toMap()
}

internal fun urlDecode(value: String): String =
    URLDecoder.decode(value, Charsets.UTF_8.name())
