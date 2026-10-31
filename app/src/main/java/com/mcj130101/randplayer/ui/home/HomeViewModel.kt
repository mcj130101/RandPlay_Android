package com.mcj130101.randplayer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcj130101.randplayer.data.repository.SettingsRepository
import com.mcj130101.randplayer.data.repository.SourceRepository
import com.mcj130101.randplayer.data.repository.VideoRepository
import com.mcj130101.randplayer.domain.model.RandomMode
import com.mcj130101.randplayer.domain.random.RandomVideoSelector
import com.mcj130101.randplayer.domain.random.SmartShuffleSelector
import com.mcj130101.randplayer.domain.random.UniformRandomSelector
import com.mcj130101.randplayer.playback.PlaybackManager
import com.mcj130101.randplayer.playback.PlaybackSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val videoCount: Int = 0,
    val sourceCount: Int = 0,
    val isRolling: Boolean = false,
    val lastPlayedVideo: com.mcj130101.randplayer.data.local.entity.VideoEntity? = null,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val sourceRepository: SourceRepository,
    private val settingsRepository: SettingsRepository,
    private val uniformSelector: UniformRandomSelector,
    private val smartSelector: SmartShuffleSelector,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _isRolling = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        videoRepository.getVideoCount(),
        sourceRepository.getSourceCount(),
        videoRepository.getLastPlayedVideo(),
        _isRolling,
        _error
    ) { videoCount, sourceCount, lastPlayed, isRolling, error ->
        HomeUiState(
            videoCount = videoCount,
            sourceCount = sourceCount,
            lastPlayedVideo = lastPlayed,
            isRolling = isRolling,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    val playbackEvent = playbackManager.playbackEvent

    fun rollDice() {
        if (_isRolling.value) return
        
        viewModelScope.launch {
            _isRolling.value = true
            _error.value = null

            // Animation delay
            delay(1000)
            
            val settings = settingsRepository.settingsFlow.first()
            val selector: RandomVideoSelector = when (settings.randomMode) {
                RandomMode.SMART_SHUFFLE -> smartSelector
                else -> uniformSelector
            }
            
            var attempts = 0
            var playbackSource: PlaybackSource? = null

            while (attempts < 5 && playbackSource == null) {
                val video = selector.selectVideo() ?: break
                playbackSource = playbackManager.preparePlayback(video.id)
                if (playbackSource == null) {
                    attempts++
                }
            }

            if (playbackSource != null) {
                playbackManager.playRaw(playbackSource)
            } else {
                _error.value = "No online video sources available"
            }
            
            _isRolling.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun onPlaybackLaunched() {
        playbackManager.onPlaybackLaunched()
    }

    fun toggleFavorite(videoId: String, currentFavorite: Boolean) {
        viewModelScope.launch {
            videoRepository.setFavorite(videoId, !currentFavorite)
        }
    }

    fun playVideo(videoId: String, recordHistory: Boolean = true) {
        viewModelScope.launch {
            playbackManager.playVideo(videoId, recordHistory)
        }
    }
}
