package com.sunflower.utilityproxy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val createdAt: Long,
    val updatedAt: Long,
    val hideServerSettings: Boolean = false,
    val encrypted: Boolean = false,
    val allowInsecure: Boolean = false,
    val sendHwidCookie: Boolean = false,
    val lastUpdateError: String? = null, // null = последнее обновление прошло успешно
)
