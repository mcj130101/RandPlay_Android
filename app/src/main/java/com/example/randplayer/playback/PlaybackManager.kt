package com.example.randplayer.playback

import com.example.randplayer.data.repository.SourceRepository
import com.example.randplayer.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackManager @Inject constructor(
    private val videoRepository: VideoRepository,
    private val sourceRepository: SourceRepository,
    private val localPlaybackProvider: LocalPlaybackProvider,
    private val smbPlaybackProvider: SmbPlaybackProvider
) {
    private val _playbackEvent = MutableStateFlow<PlaybackSource?>(null)
    val playbackEvent = _playbackEvent.asStateFlow()

    suspend fun playVideo(videoId: String) {
        val video = videoRepository.getVideoById(videoId) ?: return
        val source = sourceRepository.getSourceById(video.sourceId)
        
        val provider: VideoPlaybackProvider = if (source?.type?.name == "SMB") {
            smbPlaybackProvider
        } else {
            localPlaybackProvider
        }

        try {
            val playbackSource = provider.prepare(video)
            _playbackEvent.value = playbackSource
        } catch (e: Exception) {
            // Error handling could be added here (e.g. another StateFlow for errors)
        }
    }

    fun onPlaybackLaunched() {
        _playbackEvent.value = null
    }
}
