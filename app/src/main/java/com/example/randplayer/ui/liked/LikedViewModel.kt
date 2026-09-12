package com.example.randplayer.ui.liked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randplayer.data.local.entity.VideoEntity
import com.example.randplayer.data.repository.VideoRepository
import com.example.randplayer.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOrder {
    NAME, LAST_PLAYED, PLAY_COUNT
}

data class LikedUiState(
    val videos: List<VideoEntity> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.LAST_PLAYED,
    val isLoading: Boolean = false
)

@HiltViewModel
class LikedViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.LAST_PLAYED)

    val uiState: StateFlow<LikedUiState> = combine(
        videoRepository.getFavoriteVideos(),
        _searchQuery,
        _sortOrder
    ) { videos, query, sort ->
        val filtered = if (query.isBlank()) {
            videos
        } else {
            videos.filter { it.fileName.contains(query, ignoreCase = true) }
        }

        val sorted = when (sort) {
            SortOrder.NAME -> filtered.sortedBy { it.fileName }
            SortOrder.LAST_PLAYED -> filtered.sortedByDescending { it.lastPlayedAt ?: 0L }
            SortOrder.PLAY_COUNT -> filtered.sortedByDescending { it.playCount }
        }

        LikedUiState(
            videos = sorted,
            searchQuery = query,
            sortOrder = sort
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LikedUiState(isLoading = true))

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
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
