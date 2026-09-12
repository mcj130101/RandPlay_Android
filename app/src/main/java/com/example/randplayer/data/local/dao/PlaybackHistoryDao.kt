package com.example.randplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.randplayer.data.local.entity.PlaybackHistoryEntity
import com.example.randplayer.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

data class VideoHistoryItem(
    val historyId: Long,
    val videoId: String,
    val fileName: String,
    val playedAt: Long,
    val folderPath: String,
    val playCount: Int,
    val isFavorite: Boolean
)

@Dao
interface PlaybackHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PlaybackHistoryEntity)

    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<PlaybackHistoryEntity>>

    @Query("""
        SELECT MAX(h.id) as historyId, v.id as videoId, v.fileName, MAX(h.playedAt) as playedAt, v.folderPath, v.playCount, v.isFavorite
        FROM playback_history h
        INNER JOIN videos v ON h.videoId = v.id
        GROUP BY v.id
        ORDER BY playedAt DESC LIMIT :limit
    """)
    fun getRecentHistoryWithVideo(limit: Int): Flow<List<VideoHistoryItem>>

    @Query("""
        SELECT videos.* FROM videos 
        INNER JOIN playback_history ON videos.id = playback_history.videoId 
        ORDER BY playback_history.playedAt DESC LIMIT :limit
    """)
    fun getRecentVideos(limit: Int): Flow<List<VideoEntity>>

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()

    @Query("DELETE FROM playback_history WHERE id = :historyId")
    suspend fun deleteHistoryEntry(historyId: Long)

    @Query("DELETE FROM playback_history WHERE videoId = :videoId")
    suspend fun deleteHistoryForVideo(videoId: String)

    @Query("SELECT COUNT(*) FROM playback_history")
    fun getHistoryCount(): Flow<Int>
}
