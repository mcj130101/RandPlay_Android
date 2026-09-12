package com.example.randplayer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randplayer.data.repository.SettingsRepository
import com.example.randplayer.data.repository.SourceRepository
import com.example.randplayer.data.repository.VideoRepository
import com.example.randplayer.domain.model.RandomMode
import com.example.randplayer.domain.random.RandomVideoSelector
import com.example.randplayer.domain.random.SmartShuffleSelector
import com.example.randplayer.domain.random.UniformRandomSelector
import com.example.randplayer.playback.PlaybackManager
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
    val lastPlayedVideo: com.example.randplayer.data.local.entity.VideoEntity? = null,
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
    
    val uiState: StateFlow<HomeUiState> = combine(
        videoRepository.getVideoCount(),
        sourceRepository.getSourceCount(),
        videoRepository.getLastPlayedVideo(),
        _isRolling
    ) { videoCount, sourceCount, lastPlayed, isRolling ->
        HomeUiState(
            videoCount = videoCount,
            sourceCount = sourceCount,
            lastPlayedVideo = lastPlayed,
            isRolling = isRolling
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    val playbackEvent = playbackManager.playbackEvent

    fun rollDice() {
        if (_isRolling.value) return
        
        viewModelScope.launch {
            _isRolling.value = true
            
            // Animation delay
            delay(1000)
            
            val settings = settingsRepository.settingsFlow.first()
            val selector: RandomVideoSelector = when (settings.randomMode) {
                RandomMode.SMART_SHUFFLE -> smartSelector
                else -> uniformSelector
            }
            
            val video = selector.selectVideo()
            if (video != null) {
                // Trigger playback via manager (handles history & play count)
                playbackManager.playVideo(video.id)
            }
            
            _isRolling.value = false
        }
    }

    fun onPlaybackLaunched() {
        playbackManager.onPlaybackLaunched()
    }

    fun toggleFavorite(videoId: String, currentFavorite: Boolean) {
        viewModelScope.launch {
            videoRepository.setFavorite(videoId, !currentFavorite)
        }
    }

    fun playVideo(videoId: String) {
        viewModelScope.launch {
            playbackManager.playVideo(videoId)
        }
    }
}
