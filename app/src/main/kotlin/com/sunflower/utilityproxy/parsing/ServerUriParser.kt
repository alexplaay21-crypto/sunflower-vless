package com.sunflower.utilityproxy.parsing

interface ServerUriParser {
    val scheme: String
    fun parse(uri: String): ParseResult<ParsedServer>
}
