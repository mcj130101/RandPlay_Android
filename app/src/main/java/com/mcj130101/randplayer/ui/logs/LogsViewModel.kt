package com.mcj130101.randplayer.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcj130101.randplayer.data.local.entity.SyncLogEntity
import com.mcj130101.randplayer.data.repository.SourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            sourceRepository.getLogsBySource(sourceId)
        }
    }.map { LogsUiState(logs = it) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LogsUiState(isLoading = true))

    fun selectSource(sourceId: String?) {
        _selectedSourceId.value = sourceId
    }

    fun clearLogs() {
        val sourceId = _selectedSourceId.value ?: return
        viewModelScope.launch {
            sourceRepository.clearLogsForSource(sourceId)
        }
    }
}
