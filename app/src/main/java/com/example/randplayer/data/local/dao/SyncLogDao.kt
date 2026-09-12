package com.example.randplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.randplayer.data.local.entity.SyncLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SyncLogEntity)

    @Query("SELECT * FROM sync_logs WHERE sourceId = :sourceId ORDER BY timestamp DESC")
    fun getLogsBySource(sourceId: String): Flow<List<SyncLogEntity>>

    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<SyncLogEntity>>

    @Query("DELETE FROM sync_logs WHERE sourceId = :sourceId")
    suspend fun deleteLogsBySource(sourceId: String)

    @Query("DELETE FROM sync_logs WHERE timestamp < :threshold")
    suspend fun deleteOldLogs(threshold: Long)
}
