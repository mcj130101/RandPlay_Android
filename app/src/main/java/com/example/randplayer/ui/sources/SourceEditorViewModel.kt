package com.example.randplayer.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.randplayer.data.local.SecureCredentialStore
import com.example.randplayer.data.local.entity.SourceType
import com.example.randplayer.data.local.entity.SyncStatus
import com.example.randplayer.data.local.entity.VideoSourceEntity
import com.example.randplayer.data.repository.SourceRepository
import com.example.randplayer.data.scanner.DiscoveredServer
import com.example.randplayer.data.scanner.SmbBrowser
import com.example.randplayer.data.scanner.SmbDiscoveryManager
import com.example.randplayer.data.scanner.SmbFileItem
import com.hierynomus.smbj.auth.AuthenticationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class SmbAddState {
    object Discovering : SmbAddState()
    data class Authenticating(val server: String, val isConnecting: Boolean = false) : SmbAddState()
    data class PickingShare(val server: String, val shares: List<String>, val auth: AuthenticationContext?) : SmbAddState()
    data class PickingFolder(
        val server: String,
        val share: String,
        val currentPath: String,
        val folders: List<SmbFileItem>,
        val auth: AuthenticationContext?
    ) : SmbAddState()
    data class Error(val message: String) : SmbAddState()
}

@HiltViewModel
class SourceEditorViewModel @Inject constructor(
    private val sourceRepository: SourceRepository,
    private val credentialStore: SecureCredentialStore,
    private val discoveryManager: SmbDiscoveryManager,
    private val smbBrowser: SmbBrowser
) : ViewModel() {

    private val _discoveredServers = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    val discoveredServers: StateFlow<List<DiscoveredServer>> = _discoveredServers.asStateFlow()

    private val _smbAddState = MutableStateFlow<SmbAddState>(SmbAddState.Discovering)
    val smbAddState: StateFlow<SmbAddState> = _smbAddState.asStateFlow()

    init {
        viewModelScope.launch {
            discoveryManager.discoverServers().collectLatest {
                _discoveredServers.value = it
            }
        }
    }

    fun onServerSelected(server: DiscoveredServer) {
        _smbAddState.value = SmbAddState.Authenticating(server.ip ?: server.host)
    }

    fun onManualServerEntry(host: String) {
        _smbAddState.value = SmbAddState.Authenticating(host)
    }

    fun authenticateAndListShares(server: String, user: String?, pass: String?, domain: String?) {
        _smbAddState.value = SmbAddState.Authenticating(server, isConnecting = true)
        viewModelScope.launch {
            val auth = when {
                user == "Guest" && pass.isNullOrEmpty() -> {
                    // Specific Guest auth often works better than pure anonymous for some servers
                    AuthenticationContext("Guest", "".toCharArray(), domain ?: "")
                }
                !user.isNullOrEmpty() && pass != null -> {
                    AuthenticationContext(user, pass.toCharArray(), domain ?: "")
                }
                else -> {
                    AuthenticationContext.anonymous()
                }
            }

            smbBrowser.listShares(server, auth).fold(
                onSuccess = { shares ->
                    if (shares.isNotEmpty()) {
                        _smbAddState.value = SmbAddState.PickingShare(server, shares, auth)
                    } else {
                        _smbAddState.value = SmbAddState.Error("Connected to $server but no public shares found. Ensure sharing is enabled on the server.")
                    }
                },
                onFailure = { error ->
                    _smbAddState.value = SmbAddState.Error("Authentication failed for $server: ${error.localizedMessage ?: error.toString()}")
                }
            )
        }
    }

    fun addSmbSourceManual(name: String, server: String, shareAndPath: String, auth: AuthenticationContext?) {
        addSmbSource(name, server, shareAndPath, auth, true)
    }

    fun onShareSelected(server: String, share: String, auth: AuthenticationContext?) {
        viewModelScope.launch {
            _smbAddState.value = SmbAddState.PickingFolder(server, share, "", emptyList(), auth)
            browseFolders(server, share, "", auth)
        }
    }

    fun browseFolders(server: String, share: String, path: String, auth: AuthenticationContext?) {
        viewModelScope.launch {
            val actualAuth = auth ?: AuthenticationContext.anonymous()
            smbBrowser.listFolders(server, share, path, actualAuth).fold(
                onSuccess = { folders ->
                    _smbAddState.value = SmbAddState.PickingFolder(server, share, path, folders, auth)
                },
                onFailure = { error ->
                    _smbAddState.value = SmbAddState.Error("Failed to browse folders: ${error.message}")
                }
            )
        }
    }

    fun onFolderSelected(folder: SmbFileItem) {
        val currentState = _smbAddState.value as? SmbAddState.PickingFolder ?: return
        browseFolders(currentState.server, currentState.share, folder.path, currentState.auth)
    }

    fun onBackFromFolder() {
        val currentState = _smbAddState.value as? SmbAddState.PickingFolder ?: return
        if (currentState.currentPath.isEmpty()) {
            // Go back to share picking - need to re-list shares or keep them in state
            // For simplicity, re-list shares
            authenticateAndListShares(currentState.server, 
                if (currentState.auth?.isAnonymous == false) currentState.auth.username else null,
                if (currentState.auth?.isAnonymous == false) String(currentState.auth.password) else null,
                currentState.auth?.domain
            )
        } else {
            val parentPath = currentState.currentPath.substringBeforeLast('/', "").substringBeforeLast('/', "")
            val finalParent = if (currentState.currentPath.count { it == '/' } == 0) "" else currentState.currentPath.substringBeforeLast('/')
            // Wait, logic for parent path is tricky. 
            // folder.path is "sub1/sub2". Parent of "sub1/sub2" is "sub1". Parent of "sub1" is "".
            val parts = currentState.currentPath.split('/')
            val newPath = if (parts.size <= 1) "" else parts.dropLast(1).joinToString("/")
            browseFolders(currentState.server, currentState.share, newPath, currentState.auth)
        }
    }

    fun resetSmbState() {
        _smbAddState.value = SmbAddState.Discovering
    }

    fun addSmbSource(
        name: String,
        server: String,
        share: String,
        auth: AuthenticationContext?,
        recursive: Boolean
    ) {
        viewModelScope.launch {
            var credentialId: String? = null
            if (auth != null && !auth.isAnonymous) {
                credentialId = UUID.randomUUID().toString()
                credentialStore.saveCredential(
                    credentialId,
                    auth.username,
                    String(auth.password),
                    auth.domain
                )
            }

            val source = VideoSourceEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                type = SourceType.SMB,
                location = "$server/$share",
                credentialId = credentialId,
                recursive = recursive,
                enabled = true,
                createdAt = System.currentTimeMillis(),
                lastSyncAt = null,
                lastSyncStatus = SyncStatus.IDLE,
                lastSyncError = null
            )
            sourceRepository.insertSource(source)
        }
    }

    fun addLocalSource(name: String, uri: String, recursive: Boolean) {
        viewModelScope.launch {
            val source = VideoSourceEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                type = SourceType.LOCAL,
                location = uri,
                credentialId = null,
                recursive = recursive,
                enabled = true,
                createdAt = System.currentTimeMillis(),
                lastSyncAt = null,
                lastSyncStatus = SyncStatus.IDLE,
                lastSyncError = null
            )
            sourceRepository.insertSource(source)
        }
    }

    fun addSmbSource(
        name: String,
        server: String,
        share: String,
        username: String?,
        password: String?,
        domain: String?,
        recursive: Boolean
    ) {
        viewModelScope.launch {
            val credentialId = if (!username.isNullOrEmpty() && password != null) {
                val cid = UUID.randomUUID().toString()
                credentialStore.saveCredential(cid, username, password, domain)
                cid
            } else null

            val source = VideoSourceEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                type = SourceType.SMB,
                location = "$server/$share",
                credentialId = credentialId,
                recursive = recursive,
                enabled = true,
                createdAt = System.currentTimeMillis(),
                lastSyncAt = null,
                lastSyncStatus = SyncStatus.IDLE,
                lastSyncError = null
            )
            sourceRepository.insertSource(source)
        }
    }
}
