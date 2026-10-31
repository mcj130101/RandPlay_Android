package com.mcj130101.randplayer.sync

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.mcj130101.randplayer.data.local.entity.SourceType
import com.mcj130101.randplayer.data.local.entity.SyncLogEntity
import com.mcj130101.randplayer.data.local.entity.SyncStatus
import com.mcj130101.randplayer.data.local.entity.VideoEntity
import com.mcj130101.randplayer.data.local.entity.VideoSourceEntity
import com.mcj130101.randplayer.data.repository.SourceRepository
import com.mcj130101.randplayer.data.repository.VideoRepository
import com.mcj130101.randplayer.data.scanner.LocalVideoScanner
import com.mcj130101.randplayer.data.scanner.SmbVideoScanner
import com.mcj130101.randplayer.data.local.SecureCredentialStore
import com.mcj130101.randplayer.domain.scanner.ScanEvent
import com.mcj130101.randplayer.domain.scanner.VideoSourceScanner
import dagger.hilt.android.qualifiers.ApplicationContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncCoordinator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val sourceRepository: SourceRepository,
    private val videoRepository: VideoRepository,
    private val localScanner: LocalVideoScanner,
    private val smbScanner: SmbVideoScanner,
    private val credentialStore: SecureCredentialStore
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
                // Determine if it's an offline/network error
                val status = if (isNetworkError(error)) SyncStatus.OFFLINE else SyncStatus.ERROR

                sourceRepository.updateSyncStatus(sourceId, status, error)
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

    suspend fun pingSource(sourceId: String) = withContext(Dispatchers.IO) {
        try {
            val source = sourceRepository.getSourceById(sourceId) ?: return@withContext
            if (source.type == SourceType.LOCAL) {
                try {
                    val rootUri = Uri.parse(source.location)
                    val rootDoc = DocumentFile.fromTreeUri(context, rootUri)
                        ?: DocumentFile.fromSingleUri(context, rootUri)
                    if (rootDoc != null && rootDoc.exists() && rootDoc.canRead()) {
                        sourceRepository.updateSyncStatus(sourceId, SyncStatus.IDLE)
                    } else {
                        sourceRepository.updateSyncStatus(sourceId, SyncStatus.OFFLINE, "Local storage unavailable")
                    }
                } catch (e: Exception) {
                    sourceRepository.updateSyncStatus(sourceId, SyncStatus.OFFLINE, e.localizedMessage ?: "Local storage error")
                }
                return@withContext
            }

            // Lightweight ping for SMB source
            val auth = if (source.credentialId != null) {
                val cred = credentialStore.getCredential(source.credentialId)
                if (cred != null) {
                    NtlmPasswordAuthenticator(cred.domain ?: "", cred.username, cred.password)
                } else {
                    NtlmPasswordAuthenticator()
                }
            } else {
                NtlmPasswordAuthenticator()
            }

            val prop = java.util.Properties()
            prop.setProperty("jcifs.smb.client.enableSMB2", "true")
            prop.setProperty("jcifs.smb.client.useSMB2Negotiation", "true")
            prop.setProperty("jcifs.resolveOrder", "DNS")
            val config = jcifs.config.PropertyConfiguration(prop)
            val baseContext = jcifs.context.BaseContext(config)
            val context = baseContext.withCredentials(auth)

            val cleanLocation = source.location.trim('/').replace("//", "/")
            val rootUrl = "smb://$cleanLocation/"

            try {
                SmbFile(rootUrl, context).use { folder ->
                    if (folder.exists()) {
                        folder.listFiles()
                        sourceRepository.updateSyncStatus(sourceId, SyncStatus.IDLE)
                    } else {
                        sourceRepository.updateSyncStatus(sourceId, SyncStatus.OFFLINE, "Share does not exist")
                    }
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: e.toString()
                val status = if (isNetworkError(errorMsg)) SyncStatus.OFFLINE else SyncStatus.ERROR
                sourceRepository.updateSyncStatus(sourceId, status, errorMsg)
            }
        } catch (e: Exception) {
            // Ignore
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

    private fun isNetworkError(errorMsg: String?): Boolean {
        if (errorMsg == null) return false
        return errorMsg.contains("Timeout", ignoreCase = true) ||
               errorMsg.contains("UnknownHost", ignoreCase = true) ||
               errorMsg.contains("Unreachable", ignoreCase = true) ||
               errorMsg.contains("Network is unreachable", ignoreCase = true) ||
               errorMsg.contains("failed to connect", ignoreCase = true)
    }
}
