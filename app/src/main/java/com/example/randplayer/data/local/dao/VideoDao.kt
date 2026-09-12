package com.example.randplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.randplayer.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    @Query("SELECT * FROM videos")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id")
    fun getVideoByIdFlow(id: String): Flow<VideoEntity?>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getVideoById(id: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE isFavorite = 1")
    fun getFavoriteVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC LIMIT 1")
    fun getLastPlayedVideo(): Flow<VideoEntity?>

    @Query("SELECT * FROM videos WHERE sourceId = :sourceId")
    fun getVideosBySource(sourceId: String): Flow<List<VideoEntity>>

    @Query("SELECT COUNT(*) FROM videos")
    fun getVideoCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM videos WHERE sourceId = :sourceId")
    suspend fun getVideoCountForSource(sourceId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideo(id: String)

    @Query("DELETE FROM videos WHERE sourceId = :sourceId")
    suspend fun deleteVideosBySource(sourceId: String)

    @Query("UPDATE videos SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE videos SET isExcluded = :isExcluded WHERE id = :id")
    suspend fun setExcluded(id: String, isExcluded: Boolean)

    @Query("UPDATE videos SET playCount = playCount + 1, lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun recordPlay(id: String, timestamp: Long)

    @Query("SELECT * FROM videos WHERE isExcluded = 0 AND sourceId IN (SELECT id FROM video_sources WHERE enabled = 1)")
    suspend fun getEligibleVideos(): List<VideoEntity>

    // Retrieve a random single video via SQL
    @Query("SELECT * FROM videos WHERE isExcluded = 0 AND sourceId IN (SELECT id FROM video_sources WHERE enabled = 1) ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomEligibleVideo(): VideoEntity?
}
