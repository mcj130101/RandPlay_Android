package com.example.randplayer.data.scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.randplayer.data.local.entity.VideoEntity
import com.example.randplayer.data.local.entity.VideoSourceEntity
import com.example.randplayer.data.repository.SettingsRepository
import com.example.randplayer.domain.scanner.ScanEvent
import com.example.randplayer.domain.scanner.VideoSourceScanner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalVideoScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : VideoSourceScanner {

    override fun scan(source: VideoSourceEntity): Flow<ScanEvent> = flow {
        emit(ScanEvent.Started)

        val rootUri = Uri.parse(source.location)
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri)
        if (rootDoc == null || !rootDoc.exists()) {
            emit(ScanEvent.Error("Could not access local folder: ${source.location}"))
            return@flow
        }

        val settings = settingsRepository.settingsFlow.first()
        val supportedExtensions = settings.supportedExtensions.toSet()

        var scanned = 0
        var discovered = 0

        suspend fun walk(directory: DocumentFile) {
            directory.listFiles().forEach { file ->
                scanned++
                if (file.isDirectory) {
                    if (source.recursive) {
                        walk(file)
                    }
                } else {
                    val ext = file.name?.substringAfterLast('.', "")?.lowercase() ?: ""
                    if (ext in supportedExtensions) {
                        discovered++
                        val video = VideoEntity(
                            id = file.uri.toString(),
                            sourceId = source.id,
                            path = file.uri.toString(),
                            fileName = file.name ?: "Unknown",
                            folderPath = directory.uri.toString(),
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
                
                if (scanned % 50 == 0) {
                    emit(ScanEvent.Progress(scanned, discovered))
                }
            }
        }
        
        try {
            walk(rootDoc)
            emit(ScanEvent.Completed(added = discovered, updated = 0, deleted = 0))
        } catch (e: Exception) {
            emit(ScanEvent.Error(e.message ?: "Unknown error during local scan"))
        }
    }
}
