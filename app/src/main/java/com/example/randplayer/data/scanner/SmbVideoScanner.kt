package com.example.randplayer.data.scanner

import com.example.randplayer.data.local.SecureCredentialStore
import com.example.randplayer.data.local.entity.VideoEntity
import com.example.randplayer.data.local.entity.VideoSourceEntity
import com.example.randplayer.data.repository.SettingsRepository
import com.example.randplayer.domain.scanner.ScanEvent
import com.example.randplayer.domain.scanner.VideoSourceScanner
import jcifs.context.SingletonContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbVideoScanner @Inject constructor(
    private val credentialStore: SecureCredentialStore,
    private val settingsRepository: SettingsRepository
) : VideoSourceScanner {

    override fun scan(source: VideoSourceEntity): Flow<ScanEvent> = flow {
        emit(ScanEvent.Started)

        val settings = settingsRepository.settingsFlow.first()
        val supportedExtensions = settings.supportedExtensions.toSet()

        if (source.location.isEmpty()) {
            emit(ScanEvent.Error("Invalid SMB location: empty"))
            return@flow
        }
        
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

        // SingletonContext.getInstance() can trigger network on main thread if not careful
        // but here we are inside flow which will be shifted to IO by flowOn
        val context = SingletonContext.getInstance().withCredentials(auth)
        
        // Ensure location doesn't have multiple leading slashes and starts with smb://
        val cleanLocation = source.location.trim('/').replace("//", "/")
        val rootUrl = "smb://$cleanLocation/"

        try {
            var scanned = 0
            var discovered = 0

            suspend fun walk(url: String, relativePath: String) {
                SmbFile(url, context).use { folder ->
                    val files = try {
                        folder.listFiles()
                    } catch (e: Exception) {
                        emit(ScanEvent.Progress(scanned, discovered)) // Just to show we are still alive
                        null
                    }
                    
                    files?.forEach { file ->
                        val name = file.name.removeSuffix("/")
                        if (name == "." || name == "..") return@forEach
                        
                        scanned++
                        val currentRelativePath = if (relativePath.isEmpty()) name else "$relativePath/$name"

                        if (file.isDirectory) {
                            if (source.recursive) {
                                walk(file.url.toString(), currentRelativePath)
                            }
                        } else {
                            val ext = name.substringAfterLast('.', "")?.lowercase() ?: ""
                            if (ext in supportedExtensions) {
                                discovered++
                                val video = VideoEntity(
                                    id = file.url.toString(),
                                    sourceId = source.id,
                                    path = currentRelativePath,
                                    fileName = name,
                                    folderPath = relativePath,
                                    extension = ext,
                                    sizeBytes = file.length(),
                                    modifiedTimestamp = file.lastModified(),
                                    durationMs = null,
                                    isFavorite = false,
                                    isExcluded = false,
                                    playCount = 0,
                                    lastPlayedAt = null,
                                    indexedAt = System.currentTimeMillis()
                                )
                                emit(ScanEvent.FileFound(video))
                            }
                        }

                        if (scanned % 20 == 0) { // More frequent updates for 2k+ files
                            emit(ScanEvent.Progress(scanned, discovered))
                        }
                    }
                }
            }

            walk(rootUrl, "")
            emit(ScanEvent.Completed(added = discovered, updated = 0, deleted = 0))
            
        } catch (e: Exception) {
            emit(ScanEvent.Error("SMB Scan Error: ${e.toString()}"))
        }
    }.flowOn(Dispatchers.IO)
}
