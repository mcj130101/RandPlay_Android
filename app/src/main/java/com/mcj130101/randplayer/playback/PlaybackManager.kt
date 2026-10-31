package com.mcj130101.randplayer.playback

import com.mcj130101.randplayer.data.local.entity.PlaybackHistoryEntity
import com.mcj130101.randplayer.data.local.entity.SourceType
import com.mcj130101.randplayer.data.local.entity.SyncLogEntity
import com.mcj130101.randplayer.data.local.entity.SyncStatus
import com.mcj130101.randplayer.data.repository.PlaybackRepository
import com.mcj130101.randplayer.data.repository.SourceRepository
import com.mcj130101.randplayer.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackManager @Inject constructor(
    private val videoRepository: VideoRepository,
    private val sourceRepository: SourceRepository,
    private val playbackRepository: PlaybackRepository,
    private val localPlaybackProvider: LocalPlaybackProvider,
    private val smbPlaybackProvider: SmbPlaybackProvider
) {
    private val _playbackEvent = MutableStateFlow<PlaybackSource?>(null)
    val playbackEvent = _playbackEvent.asStateFlow()

    suspend fun preparePlayback(videoId: String, recordHistory: Boolean = true): PlaybackSource? {
        val video = videoRepository.getVideoById(videoId) ?: return null
        val source = sourceRepository.getSourceById(video.sourceId)
        
        if (source == null || !source.enabled || source.lastSyncStatus == SyncStatus.OFFLINE) {
            return null
        }

        val provider: VideoPlaybackProvider = if (source.type == SourceType.SMB) {
            smbPlaybackProvider
        } else {
            localPlaybackProvider
        }

        return try {
            val playbackSource = provider.prepare(video)
            
            if (recordHistory) {
                playbackRepository.addHistory(
                    PlaybackHistoryEntity(
                        videoId = videoId,
                        playedAt = System.currentTimeMillis(),
                        completionStatus = null
                    )
                )
                videoRepository.recordPlay(videoId)
            }
            
            playbackSource
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Source offline"
            sourceRepository.updateSyncStatus(source.id, SyncStatus.OFFLINE, errorMsg)
            sourceRepository.addSyncLog(
                SyncLogEntity(
                    sourceId = source.id,
                    timestamp = System.currentTimeMillis(),
                    isError = true,
                    message = "Playback check failed: $errorMsg"
                )
            )
            null
        }
    }

    suspend fun playVideo(videoId: String, recordHistory: Boolean = true) {
        val playbackSource = preparePlayback(videoId, recordHistory)
        if (playbackSource != null) {
            _playbackEvent.value = playbackSource
        }
    }
    
    fun playRaw(playbackSource: PlaybackSource) {
        _playbackEvent.value = playbackSource
    }

    fun onPlaybackLaunched() {
        _playbackEvent.value = null
    }
}
