package com.example.randplayer.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randplayer.data.local.entity.SyncLogEntity
import com.example.randplayer.data.repository.SourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class LogsUiState(
    val logs: List<SyncLogEntity> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val sourceRepository: SourceRepository
) : ViewModel() {

    private val _selectedSourceId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LogsUiState> = _selectedSourceId.flatMapLatest { sourceId ->
        if (sourceId == null) {
            // Generic logs or empty
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            sourceRepository.getLogsBySource(sourceId)
        }
    }.map { LogsUiState(logs = it) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LogsUiState(isLoading = true))

    fun selectSource(sourceId: String?) {
        _selectedSourceId.value = sourceId
    }
}
