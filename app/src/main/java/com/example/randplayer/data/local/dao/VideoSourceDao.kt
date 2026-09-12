package com.example.randplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.randplayer.data.local.entity.SyncStatus
import com.example.randplayer.data.local.entity.VideoSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoSourceDao {

    @Query("SELECT * FROM video_sources")
    fun getAllSources(): Flow<List<VideoSourceEntity>>

    @Query("SELECT * FROM video_sources WHERE enabled = 1")
    fun getEnabledSources(): Flow<List<VideoSourceEntity>>
    
    @Query("SELECT * FROM video_sources WHERE enabled = 1")
    suspend fun getEnabledSourcesList(): List<VideoSourceEntity>

    @Query("SELECT * FROM video_sources WHERE id = :id")
    suspend fun getSourceById(id: String): VideoSourceEntity?
    
    @Query("SELECT * FROM video_sources WHERE id = :id")
    fun observeSourceById(id: String): Flow<VideoSourceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: VideoSourceEntity)

    @Update
    suspend fun updateSource(source: VideoSourceEntity)

    @Query("DELETE FROM video_sources WHERE id = :id")
    suspend fun deleteSource(id: String)

    @Query("UPDATE video_sources SET lastSyncAt = :timestamp, lastSyncStatus = :status, lastSyncError = :error WHERE id = :id")
    suspend fun updateSyncStatus(id: String, timestamp: Long?, status: SyncStatus, error: String?)
    
    @Query("SELECT COUNT(*) FROM video_sources")
    fun getSourceCount(): Flow<Int>
}
