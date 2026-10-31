package com.mcj130101.randplayer.data.repository

import com.mcj130101.randplayer.data.local.dao.SyncLogDao
import com.mcj130101.randplayer.data.local.dao.VideoSourceDao
import com.mcj130101.randplayer.data.local.entity.SyncLogEntity
import com.mcj130101.randplayer.data.local.entity.SyncStatus
import com.mcj130101.randplayer.data.local.entity.VideoSourceEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceRepository @Inject constructor(
    private val sourceDao: VideoSourceDao,
    private val syncLogDao: SyncLogDao
) {
    fun getAllSources(): Flow<List<VideoSourceEntity>> = sourceDao.getAllSources()
    
    fun observeSourceById(id: String): Flow<VideoSourceEntity?> = sourceDao.observeSourceById(id)
    
    suspend fun getSourceById(id: String): VideoSourceEntity? = sourceDao.getSourceById(id)
    
    suspend fun insertSource(source: VideoSourceEntity) = sourceDao.insertSource(source)
    
    suspend fun updateSource(source: VideoSourceEntity) = sourceDao.updateSource(source)
    
    suspend fun deleteSource(id: String) = sourceDao.deleteSource(id)
    
    suspend fun updateSyncStatus(id: String, status: SyncStatus, error: String? = null) {
        sourceDao.updateSyncStatus(id, System.currentTimeMillis(), status, error)
    }

    suspend fun addSyncLog(log: SyncLogEntity) = syncLogDao.insertLog(log)

    fun getLogsBySource(sourceId: String): Flow<List<SyncLogEntity>> = syncLogDao.getLogsBySource(sourceId)
    
    suspend fun clearLogsForSource(sourceId: String) = syncLogDao.deleteLogsBySource(sourceId)
    
    fun getSourceCount(): Flow<Int> = sourceDao.getSourceCount()
}
