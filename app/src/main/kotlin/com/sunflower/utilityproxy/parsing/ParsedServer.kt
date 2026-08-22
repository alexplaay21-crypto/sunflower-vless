package com.sunflower.utilityproxy.parsing

import com.sunflower.utilityproxy.data.local.ServerEntity

/**
 * Модель, в которую любой парсер (VLESS/VMess/Trojan/SS) приводит ссылку,
 * прежде чем она станет ServerEntity. Поля, не нужные протоколу, остаются
 * null — не заполняем то, чего в исходной ссылке не было.
 */
data class ParsedServer(
    val name: String,
    val protocol: String,
    val host: String,
    val port: Int,
    val uuid: String? = null,
    val password: String? = null,
    val method: String? = null,
    val alterId: Int? = null,
    val security: String? = null,
    val network: String? = null,
    val sni: String? = null,
    val fingerprint: String? = null,
    val publicKey: String? = null,
    val shortId: String? = null,
    val flow: String? = null,
    val path: String? = null,
    val headerHost: String? = null,
    val rawUri: String,
)

fun ParsedServer.toEntity(subscriptionId: Long?, readOnly: Boolean = false) = ServerEntity(
    subscriptionId = subscriptionId,
    name = name,
    protocol = protocol,
    host = host,
    port = port,
    uuid = uuid,
    password = password,
    method = method,
    alterId = alterId,
    security = security,
    network = network,
    sni = sni,
    fingerprint = fingerprint,
    publicKey = publicKey,
    shortId = shortId,
    flow = flow,
    path = path,
    headerHost = headerHost,
    rawUri = rawUri,
    isReadOnly = readOnly,
)
