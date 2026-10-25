package com.example.randplayer.ui.explorer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randplayer.data.browser.LocalFileBrowser
import com.example.randplayer.data.local.SecureCredentialStore
import com.example.randplayer.data.local.entity.SourceType
import com.example.randplayer.data.local.entity.VideoEntity
import com.example.randplayer.data.local.entity.VideoSourceEntity
import com.example.randplayer.data.repository.SourceRepository
import com.example.randplayer.data.repository.VideoRepository
import com.example.randplayer.data.scanner.SmbBrowser
import com.example.randplayer.domain.browser.BrowserFileItem
import com.example.randplayer.playback.PlaybackManager
import com.hierynomus.smbj.auth.AuthenticationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ExplorerUiState(
    val source: VideoSourceEntity? = null,
    val currentPath: String = "",
    val breadcrumbs: List<Pair<String, String>> = emptyList(),
    val files: List<BrowserFileItem> = emptyList(),
    val indexedVideos: List<VideoEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.NAME_ASC
)

enum class SortOrder {
    NAME_ASC, NAME_DESC, DATE_ASC, DATE_DESC, SIZE_ASC, SIZE_DESC
}

@HiltViewModel
class ExplorerViewModel @Inject constructor(
    private val sourceRepository: SourceRepository,
    private val videoRepository: VideoRepository,
    private val localBrowser: LocalFileBrowser,
    private val smbBrowser: SmbBrowser,
    private val credentialStore: SecureCredentialStore,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExplorerUiState())
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    private var authContext: AuthenticationContext? = null

    fun initialize(sourceId: String) {
        viewModelScope.launch {
            val source = sourceRepository.getSourceById(sourceId)
            if (source == null) {
                _uiState.update { it.copy(error = "Source not found") }
                return@launch
            }

            _uiState.update { it.copy(source = source, currentPath = source.location) }
            
            if (source.type == SourceType.SMB) {
                authContext = if (source.credentialId != null) {
                    val cred = credentialStore.getCredential(source.credentialId)
                    if (cred != null) {
                        AuthenticationContext(cred.username, cred.password.toCharArray(), cred.domain)
                    } else {
                        AuthenticationContext.anonymous()
                    }
                } else {
                    AuthenticationContext.anonymous()
                }
            }
            
            // Observe indexed videos for recursive search & metadata
            viewModelScope.launch {
                videoRepository.getVideosBySource(sourceId).collect { videos ->
                    _uiState.update { it.copy(indexedVideos = videos) }
                }
            }

            refreshFiles()
            updateBreadcrumbs(source.location)
        }
    }

    fun navigateTo(path: String) {
        _uiState.update { it.copy(currentPath = path) }
        refreshFiles()
        updateBreadcrumbs(path)
    }

    fun refreshFiles() {
        val state = _uiState.value
        val source = state.source ?: return
        val path = state.currentPath

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = if (source.type == SourceType.LOCAL) {
                localBrowser.listFiles(path)
            } else {
                val smbUrl = if (path.startsWith("smb://")) path else "smb://${path.trim('/')}/"
                smbBrowser.listFiles(smbUrl, authContext ?: AuthenticationContext.anonymous())
            }

            result.onSuccess { files ->
                _uiState.update { it.copy(files = files, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun playFile(file: BrowserFileItem) {
        val state = _uiState.value
        val source = state.source ?: return
        
        viewModelScope.launch {
            // Find existing video entity or create one so playback history is always recorded
            var video = videoRepository.getVideoById(file.id) ?: videoRepository.getVideoById(file.path)
            if (video == null) {
                val name = file.name
                val ext = name.substringAfterLast('.', "").lowercase()
                video = VideoEntity(
                    id = if (file.id.isNotEmpty()) file.id else file.path,
                    sourceId = source.id,
                    path = file.path,
                    fileName = name,
                    folderPath = state.currentPath,
                    extension = ext,
                    sizeBytes = file.size,
                    modifiedTimestamp = file.lastModified,
                    durationMs = null,
                    isFavorite = false,
                    isExcluded = false,
                    playCount = 0,
                    lastPlayedAt = null,
                    indexedAt = System.currentTimeMillis()
                )
                videoRepository.insertVideos(listOf(video))
            }
            playbackManager.playVideo(video.id, recordHistory = true)
        }
    }

    fun toggleFavorite(file: BrowserFileItem) {
        viewModelScope.launch {
            val video = videoRepository.getVideoById(file.id) ?: videoRepository.getVideoById(file.path) ?: return@launch
            videoRepository.setFavorite(video.id, !video.isFavorite)
        }
    }

    fun toggleExclude(file: BrowserFileItem) {
        viewModelScope.launch {
            val video = videoRepository.getVideoById(file.id) ?: videoRepository.getVideoById(file.path) ?: return@launch
            videoRepository.setExcluded(video.id, !video.isExcluded)
        }
    }

    fun deleteFile(file: BrowserFileItem) {
        val source = _uiState.value.source ?: return
        viewModelScope.launch {
            val result = if (source.type == SourceType.LOCAL) {
                localBrowser.delete(file.path)
            } else {
                smbBrowser.delete(file.path, authContext ?: AuthenticationContext.anonymous())
            }
            result.onSuccess { refreshFiles() }
                .onFailure { e -> _uiState.update { it.copy(error = "Delete failed: ${e.message}") } }
        }
    }

    fun renameFile(file: BrowserFileItem, newName: String) {
        val source = _uiState.value.source ?: return
        viewModelScope.launch {
            val result = if (source.type == SourceType.LOCAL) {
                localBrowser.rename(file.path, newName)
            } else {
                smbBrowser.rename(file.path, newName, authContext ?: AuthenticationContext.anonymous())
            }
            result.onSuccess { refreshFiles() }
                .onFailure { e -> _uiState.update { it.copy(error = "Rename failed: ${e.message}") } }
        }
    }

    fun createDirectory(name: String) {
        val state = _uiState.value
        val source = state.source ?: return
        viewModelScope.launch {
            val result = if (source.type == SourceType.LOCAL) {
                localBrowser.createDirectory(state.currentPath, name)
            } else {
                val smbUrl = if (state.currentPath.endsWith("/")) state.currentPath + name else "${state.currentPath}/$name"
                smbBrowser.createDirectory(smbUrl, authContext ?: AuthenticationContext.anonymous())
            }
            result.onSuccess { refreshFiles() }
                .onFailure { e -> _uiState.update { it.copy(error = "Create folder failed: ${e.message}") } }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
    }

    private fun updateBreadcrumbs(currentPath: String) {
        val source = _uiState.value.source ?: return
        
        val breadcrumbs = mutableListOf<Pair<String, String>>()
        
        if (source.type == SourceType.LOCAL) {
            breadcrumbs.add("Root" to source.location)
            if (currentPath != source.location) {
                breadcrumbs.add("Subfolder" to currentPath)
            }
        } else {
            val pathPart = currentPath.substringAfter("smb://").trim('/')
            val parts = pathPart.split('/').filter { it.isNotEmpty() }
            
            var runningPath = "smb://"
            parts.forEach { part ->
                runningPath += "$part/"
                breadcrumbs.add(part to runningPath)
            }
        }
        _uiState.update { it.copy(breadcrumbs = breadcrumbs) }
    }
}
