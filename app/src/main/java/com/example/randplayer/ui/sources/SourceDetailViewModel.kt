package com.example.randplayer.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randplayer.data.local.entity.VideoEntity
import com.example.randplayer.data.local.entity.VideoSourceEntity
import com.example.randplayer.data.repository.SourceRepository
import com.example.randplayer.data.repository.VideoRepository
import com.example.randplayer.sync.SyncCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SourceDetailUiState(
    val source: VideoSourceEntity? = null,
    val videos: List<VideoEntity> = emptyList(),
    val totalSizeMb: Float = 0f,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SourceDetailViewModel @Inject constructor(
    private val sourceRepository: SourceRepository,
    private val videoRepository: VideoRepository,
    private val syncCoordinator: SyncCoordinator
) : ViewModel() {

    private val _sourceId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SourceDetailUiState> = _sourceId.flatMapLatest { id ->
        if (id == null) {
            flowOf(SourceDetailUiState(isLoading = false))
        } else {
            combine(
                sourceRepository.observeSourceById(id),
                videoRepository.getVideosBySource(id)
            ) { source, videos ->
                val totalBytes = videos.fold(0L) { acc, video -> acc + (video.sizeBytes ?: 0L) }
                val sizeMb = totalBytes / (1024f * 1024f)
                SourceDetailUiState(
                    source = source,
                    videos = videos,
                    totalSizeMb = sizeMb,
                    isLoading = false
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SourceDetailUiState())

    fun initialize(sourceId: String) {
        _sourceId.value = sourceId
    }

    fun syncSource() {
        val id = _sourceId.value ?: return
        viewModelScope.launch {
            syncCoordinator.syncSource(id)
        }
    }

    fun toggleSourceEnabled(enabled: Boolean) {
        val id = _sourceId.value ?: return
        viewModelScope.launch {
            val source = sourceRepository.getSourceById(id) ?: return@launch
            sourceRepository.updateSource(source.copy(enabled = enabled))
        }
    }

    fun deleteSource(onDeleted: () -> Unit) {
        val id = _sourceId.value ?: return
        viewModelScope.launch {
            sourceRepository.deleteSource(id)
            onDeleted()
        }
    }
}
