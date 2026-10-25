package com.example.randplayer.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randplayer.data.local.entity.VideoSourceEntity
import com.example.randplayer.data.repository.SourceRepository
import com.example.randplayer.sync.SyncCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SourcesUiState(
    val sources: List<VideoSourceEntity> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class SourcesViewModel @Inject constructor(
    private val sourceRepository: SourceRepository,
    private val syncCoordinator: SyncCoordinator
) : ViewModel() {

    val uiState: StateFlow<SourcesUiState> = sourceRepository.getAllSources()
        .map { SourcesUiState(sources = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SourcesUiState(isLoading = true))

    fun syncSource(sourceId: String) {
        viewModelScope.launch {
            syncCoordinator.syncSource(sourceId)
        }
    }

    fun deleteSource(sourceId: String) {
        viewModelScope.launch {
            sourceRepository.deleteSource(sourceId)
        }
    }

    fun toggleSourceEnabled(sourceId: String, enabled: Boolean) {
        viewModelScope.launch {
            val source = sourceRepository.getSourceById(sourceId) ?: return@launch
            sourceRepository.updateSource(source.copy(enabled = enabled))
        }
    }
    
    fun pingSources() {
        viewModelScope.launch {
            try {
                // Fetch the list of sources directly from the repository
                // instead of using uiState, which might not be initialized yet on app start
                val sources = sourceRepository.getAllSources().first()
                sources.forEach { source ->
                    if (source.enabled) {
                        syncCoordinator.pingSource(source.id)
                    }
                }
            } catch (e: Exception) {
                // Handle potential errors fetching sources
            }
        }
    }
}
