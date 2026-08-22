package com.sunflower.utilityproxy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SubscriptionEntity::class, ServerEntity::class],
    version = 1,
    exportSchema = false, // до первой миграции — включим экспорт схемы, когда version > 1 станет реальной
)
abstract class SunflowerDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun serverDao(): ServerDao
}
