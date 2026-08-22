package com.sunflower.utilityproxy.parsing

sealed class ParseResult<out T> {
    data class Success<T>(val value: T) : ParseResult<T>()
    data class Failure(val reason: String) : ParseResult<Nothing>()
}
