package com.postgrab.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE status = 'DOWNLOADED' AND savedUri IS NOT NULL ORDER BY updatedAt DESC LIMIT 12")
    fun observeRecent(): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insert(entity: HistoryEntity): Long

    @Query("UPDATE history SET status = :status, savedUri = :savedUri, error = :error, updatedAt = :now WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, savedUri: String?, error: String?, now: Long)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM history")
    suspend fun clear()
}
