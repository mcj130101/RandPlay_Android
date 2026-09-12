package com.example.randplayer.data.repository

import com.example.randplayer.data.local.dao.VideoDao
import com.example.randplayer.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepository @Inject constructor(
    private val videoDao: VideoDao
) {
    fun getAllVideos(): Flow<List<VideoEntity>> = videoDao.getAllVideos()
    
    fun getVideosBySource(sourceId: String): Flow<List<VideoEntity>> = videoDao.getVideosBySource(sourceId)
    
    suspend fun getVideoById(id: String): VideoEntity? = videoDao.getVideoById(id)

    fun getVideoByIdFlow(id: String): Flow<VideoEntity?> = videoDao.getVideoByIdFlow(id)

    fun getFavoriteVideos(): Flow<List<VideoEntity>> = videoDao.getFavoriteVideos()

    fun getLastPlayedVideo(): Flow<VideoEntity?> = videoDao.getLastPlayedVideo()
    
    suspend fun insertVideos(videos: List<VideoEntity>) = videoDao.insertVideos(videos)
    
    suspend fun updateVideo(video: VideoEntity) = videoDao.updateVideo(video)
    
    suspend fun setFavorite(id: String, isFavorite: Boolean) = videoDao.setFavorite(id, isFavorite)
    
    suspend fun setExcluded(id: String, isExcluded: Boolean) = videoDao.setExcluded(id, isExcluded)
    
    suspend fun recordPlay(id: String) = videoDao.recordPlay(id, System.currentTimeMillis())
    
    suspend fun getRandomEligibleVideo(): VideoEntity? = videoDao.getRandomEligibleVideo()

    fun getVideoCount(): Flow<Int> = videoDao.getVideoCount()
}
