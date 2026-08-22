package com.sunflower.utilityproxy.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY isFavorite DESC, name ASC")
    fun observeAll(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE subscriptionId = :subscriptionId ORDER BY name ASC")
    fun observeBySubscription(subscriptionId: Long): Flow<List<ServerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(servers: List<ServerEntity>): List<Long>

    @Update
    suspend fun update(server: ServerEntity)

    @Delete
    suspend fun delete(server: ServerEntity)

    @Query("DELETE FROM servers")
    suspend fun deleteAll()

    @Query("UPDATE servers SET isFavorite = :isFavorite WHERE id = :serverId")
    suspend fun setFavorite(serverId: Long, isFavorite: Boolean)

    @Query("DELETE FROM servers WHERE subscriptionId = :subscriptionId")
    suspend fun deleteBySubscription(subscriptionId: Long)

    /**
     * Атомарная замена серверов подписки в одной Room-транзакции — пункты
     * 46/47 промта (Atomic Update): нельзя получить состояние "старые
     * серверы уже удалены, новые ещё не пришли".
     */
    @Transaction
    suspend fun replaceForSubscription(subscriptionId: Long, servers: List<ServerEntity>) {
        deleteBySubscription(subscriptionId)
        insertAll(servers)
    }
}
