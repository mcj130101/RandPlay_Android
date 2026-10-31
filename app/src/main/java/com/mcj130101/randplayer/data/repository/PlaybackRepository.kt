package com.mcj130101.randplayer.data.repository

import com.mcj130101.randplayer.data.local.dao.PlaybackHistoryDao
import com.mcj130101.randplayer.data.local.dao.VideoHistoryItem
import com.mcj130101.randplayer.data.local.entity.PlaybackHistoryEntity
import com.mcj130101.randplayer.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackRepository @Inject constructor(
    private val historyDao: PlaybackHistoryDao
) {
    fun getRecentHistory(limit: Int): Flow<List<PlaybackHistoryEntity>> = historyDao.getRecentHistory(limit)
    
    fun getRecentVideos(limit: Int): Flow<List<VideoEntity>> = historyDao.getRecentVideos(limit)

    fun getHistoryItems(limit: Int): Flow<List<VideoHistoryItem>> = historyDao.getRecentHistoryWithVideo(limit)
    
    suspend fun addHistory(history: PlaybackHistoryEntity) = historyDao.insertHistory(history)
    
    suspend fun clearHistory() = historyDao.clearHistory()

    suspend fun deleteHistoryEntry(historyId: Long) = historyDao.deleteHistoryEntry(historyId)

    suspend fun deleteHistoryForVideo(videoId: String) = historyDao.deleteHistoryForVideo(videoId)
}
