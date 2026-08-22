package com.sunflower.utilityproxy.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Все protocol-специфичные поля держим опциональными в одной таблице —
 * параметров у каждого протокола немного, а зоопарк из четырёх сабклассов
 * усложнил бы Room-связи без реальной выгоды. Источник полей — ParsedServer
 * в parsing/ (что распознал конкретный парсер, то здесь и оказывается).
 */
@Entity(
    tableName = "servers",
    foreignKeys = [
        ForeignKey(
            entity = SubscriptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["subscriptionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("subscriptionId")],
)
data class ServerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subscriptionId: Long?, // null = добавлен вручную (Local, не Subscription)
    val name: String,
    val protocol: String, // "vless" | "vmess" | "trojan" | "ss"
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
    val isFavorite: Boolean = false,
    val isReadOnly: Boolean = false, // true, если subscription.hideServerSettings
    val lastPingMs: Long? = null, // null = ещё не проверялся; см. TunnelManager.canRunBulkPingTest
)
