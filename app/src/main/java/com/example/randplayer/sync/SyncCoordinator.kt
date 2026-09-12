package com.example.randplayer.sync

import com.example.randplayer.data.local.entity.SourceType
import com.example.randplayer.data.local.entity.SyncLogEntity
import com.example.randplayer.data.local.entity.SyncStatus
import com.example.randplayer.data.local.entity.VideoEntity
import com.example.randplayer.data.local.entity.VideoSourceEntity
import com.example.randplayer.data.repository.SourceRepository
import com.example.randplayer.data.repository.VideoRepository
import com.example.randplayer.data.scanner.LocalVideoScanner
import com.example.randplayer.data.scanner.SmbVideoScanner
import com.example.randplayer.domain.scanner.ScanEvent
import com.example.randplayer.domain.scanner.VideoSourceScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncCoordinator @Inject constructor(
    private val sourceRepository: SourceRepository,
    private val videoRepository: VideoRepository,
    private val localScanner: LocalVideoScanner,
    private val smbScanner: SmbVideoScanner
) {
    private val syncMutex = Mutex()
    private val activeSyncs = mutableSetOf<String>()

    suspend fun syncSource(sourceId: String) = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            if (activeSyncs.contains(sourceId)) return@withContext
            activeSyncs.add(sourceId)
        }

        try {
            val source = sourceRepository.getSourceById(sourceId) ?: return@withContext
            if (!source.enabled) return@withContext

            sourceRepository.updateSyncStatus(sourceId, SyncStatus.SYNCING)

            val scanner: VideoSourceScanner = when (source.type) {
                SourceType.LOCAL -> localScanner
                SourceType.SMB -> smbScanner
            }

            val discoveredVideos = mutableListOf<VideoEntity>()
            var error: String? = null

            try {
                scanner.scan(source).collect { event ->
                    when (event) {
                        is ScanEvent.FileFound -> discoveredVideos.add(event.video)
                        is ScanEvent.Error -> error = event.message
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                error = e.toString()
            }

            if (error != null) {
                sourceRepository.updateSyncStatus(sourceId, SyncStatus.ERROR, error)
                sourceRepository.addSyncLog(SyncLogEntity(
                    sourceId = sourceId,
                    timestamp = System.currentTimeMillis(),
                    isError = true,
                    message = "Sync failed: $error"
                ))
            } else {
                updateDatabase(source, discoveredVideos)
                sourceRepository.updateSyncStatus(sourceId, SyncStatus.IDLE)
                sourceRepository.addSyncLog(SyncLogEntity(
                    sourceId = sourceId,
                    timestamp = System.currentTimeMillis(),
                    isError = false,
                    message = "Sync completed successfully. Found ${discoveredVideos.size} videos.",
                    itemsAdded = discoveredVideos.size
                ))
            }
        } finally {
            syncMutex.withLock {
                activeSyncs.remove(sourceId)
            }
        }
    }

    private suspend fun updateDatabase(source: VideoSourceEntity, discovered: List<VideoEntity>) {
        if (discovered.isEmpty()) {
            // Optional: Should we delete old videos if none found? 
            // For now, let's keep them unless we are sure the source is accessible and empty.
            return
        }
        videoRepository.insertVideos(discovered)
    }
}
