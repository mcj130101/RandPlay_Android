package com.example.randplayer.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randplayer.data.local.dao.VideoHistoryItem
import com.example.randplayer.data.repository.PlaybackRepository
import com.example.randplayer.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import com.example.randplayer.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HistorySortOrder {
    RECENT, NAME, PLAY_COUNT
}

data class HistoryUiState(
    val recentItems: List<VideoHistoryItem> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: HistorySortOrder = HistorySortOrder.RECENT,
    val isLoading: Boolean = false
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    private val videoRepository: VideoRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(HistorySortOrder.RECENT)

    val uiState: StateFlow<HistoryUiState> = combine(
        playbackRepository.getHistoryItems(100),
        _searchQuery,
        _sortOrder
    ) { items, query, sort ->
        val filtered = if (query.isBlank()) {
            items
        } else {
            items.filter { it.fileName.contains(query, ignoreCase = true) }
        }

        val sorted = when (sort) {
            HistorySortOrder.RECENT -> filtered // Already sorted by date from DAO
            HistorySortOrder.NAME -> filtered.sortedBy { it.fileName }
            HistorySortOrder.PLAY_COUNT -> filtered.sortedByDescending { it.playCount }
        }

        HistoryUiState(
            recentItems = sorted,
            searchQuery = query,
            sortOrder = sort
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState(isLoading = true))

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: HistorySortOrder) {
        _sortOrder.value = order
    }

    fun clearHistory() {
        viewModelScope.launch {
            playbackRepository.clearHistory()
        }
    }

    fun removeItem(historyId: Long) {
        viewModelScope.launch {
            playbackRepository.deleteHistoryEntry(historyId)
        }
    }

    fun playVideo(videoId: String) {
        viewModelScope.launch {
            playbackManager.playVideo(videoId)
        }
    }

    fun toggleFavorite(videoId: String, currentFavorite: Boolean) {
        viewModelScope.launch {
            videoRepository.setFavorite(videoId, !currentFavorite)
        }
    }
}
