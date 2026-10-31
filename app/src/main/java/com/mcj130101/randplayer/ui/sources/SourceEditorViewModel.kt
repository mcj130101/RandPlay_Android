package com.mcj130101.randplayer.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcj130101.randplayer.data.local.SecureCredentialStore
import com.mcj130101.randplayer.data.local.entity.SourceType
import com.mcj130101.randplayer.data.local.entity.SyncStatus
import com.mcj130101.randplayer.data.local.entity.VideoSourceEntity
import com.mcj130101.randplayer.data.repository.SourceRepository
import com.mcj130101.randplayer.data.scanner.DiscoveredServer
import com.mcj130101.randplayer.data.scanner.SmbBrowser
import com.mcj130101.randplayer.data.scanner.SmbDiscoveryManager
import com.mcj130101.randplayer.data.scanner.SmbFileItem
import com.mcj130101.randplayer.sync.SyncCoordinator
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
    data class PickingShare(
        val server: String,
        val shares: List<String>,
        val auth: AuthenticationContext?,
        val customName: String? = null
    ) : SmbAddState()
    data class PickingFolder(
        val server: String,
        val share: String,
        val currentPath: String,
        val folders: List<SmbFileItem>,
        val auth: AuthenticationContext?,
        val customName: String? = null
    ) : SmbAddState()
    data class Error(val message: String, val server: String? = null) : SmbAddState()
}

@HiltViewModel
class SourceEditorViewModel @Inject constructor(
    private val sourceRepository: SourceRepository,
    private val credentialStore: SecureCredentialStore,
    private val discoveryManager: SmbDiscoveryManager,
    private val smbBrowser: SmbBrowser,
    private val syncCoordinator: SyncCoordinator
) : ViewModel() {

    private val _discoveredServers = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    val discoveredServers: StateFlow<List<DiscoveredServer>> = _discoveredServers.asStateFlow()

    private val _smbAddState = MutableStateFlow<SmbAddState>(SmbAddState.Discovering)
    val smbAddState: StateFlow<SmbAddState> = _smbAddState.asStateFlow()

    private val _editingSource = MutableStateFlow<VideoSourceEntity?>(null)
    val editingSource: StateFlow<VideoSourceEntity?> = _editingSource.asStateFlow()

    private val _sourceType = MutableStateFlow<SourceType>(SourceType.LOCAL)
    val sourceType: StateFlow<SourceType> = _sourceType.asStateFlow()

    fun setType(type: SourceType) {
        _sourceType.value = type
    }

    fun loadSource(sourceId: String) {
        viewModelScope.launch {
            val source = sourceRepository.getSourceById(sourceId)
            _editingSource.value = source
        }
    }

    fun updateLocalSource(id: String, name: String, uri: String, recursive: Boolean) {
        viewModelScope.launch {
            val existing = sourceRepository.getSourceById(id) ?: return@launch
            val updated = existing.copy(
                name = name,
                location = uri,
                recursive = recursive
            )
            sourceRepository.updateSource(updated)
        }
    }

    fun updateSmbSource(
        id: String,
        name: String,
        server: String,
        share: String,
        username: String?,
        password: String?,
        domain: String?,
        recursive: Boolean
    ) {
        viewModelScope.launch {
            val existing = sourceRepository.getSourceById(id) ?: return@launch
            
            var credentialId = existing.credentialId
            if (!username.isNullOrEmpty() && password != null) {
                if (credentialId == null) {
                    credentialId = UUID.randomUUID().toString()
                }
                credentialStore.saveCredential(credentialId, username, password, domain)
            }

            val updated = existing.copy(
                name = name,
                location = "$server/$share",
                credentialId = credentialId,
                recursive = recursive
            )
            sourceRepository.updateSource(updated)
        }
    }

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

    private fun parseSmbError(error: Throwable, server: String, isGuest: Boolean): String {
        val message = error.message ?: error.toString()
        val lowerMessage = message.lowercase()

        return when {
            error is jcifs.smb.SmbAuthException ||
            lowerMessage.contains("logon failure") ||
            lowerMessage.contains("bad password") ||
            lowerMessage.contains("unknown user name") ||
            lowerMessage.contains("access is denied") ||
            lowerMessage.contains("account_disabled") ||
            lowerMessage.contains("0xc000006d") ||
            lowerMessage.contains("0xc0000022") -> {
                if (isGuest) {
                    "Guest access was denied by $server. Please log in with a username and password."
                } else {
                    "Authentication failed for $server. Please check your username and password."
                }
            }

            error is kotlinx.coroutines.TimeoutCancellationException ||
            error is java.net.SocketTimeoutException ||
            lowerMessage.contains("timed out") ||
            lowerMessage.contains("timeout") -> {
                "Connection to $server timed out. Please check if the server is powered on and connected to the network."
            }

            error is java.net.UnknownHostException ||
            lowerMessage.contains("unknown host") ||
            lowerMessage.contains("dns") -> {
                "Server '$server' could not be found. Please check the IP address or hostname."
            }

            error is java.net.ConnectException ||
            lowerMessage.contains("connection refused") ||
            lowerMessage.contains("failed to connect") -> {
                "Unable to connect to $server. Please verify that SMB file sharing is enabled on the server."
            }

            else -> {
                val cleanCause = message.substringAfterLast(':').trim()
                if (cleanCause.isNotEmpty() && cleanCause.length < 80) {
                    "Connection error: $cleanCause"
                } else {
                    "Could not connect to $server. Please verify your network connection and server details."
                }
            }
        }
    }

    fun authenticateAndListShares(server: String, user: String?, pass: String?, domain: String?, customName: String? = null) {
        _smbAddState.value = SmbAddState.Authenticating(server, isConnecting = true)
        viewModelScope.launch {
            val isGuest = user == "Guest" || user.isNullOrEmpty()
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
                        _smbAddState.value = SmbAddState.PickingShare(server, shares, auth, customName)
                    } else {
                        _smbAddState.value = SmbAddState.Error(
                            message = "Connected to $server, but no public shares were found. Ensure sharing is enabled on the server.",
                            server = server
                        )
                    }
                },
                onFailure = { error ->
                    val humanReadable = parseSmbError(error, server, isGuest)
                    _smbAddState.value = SmbAddState.Error(
                        message = humanReadable,
                        server = server
                    )
                }
            )
        }
    }

    fun addSmbSourceManual(name: String, server: String, shareAndPath: String, auth: AuthenticationContext?) {
        addSmbSource(name, server, shareAndPath, auth, true)
    }

    fun onShareSelected(server: String, share: String, auth: AuthenticationContext?, customName: String? = null) {
        viewModelScope.launch {
            _smbAddState.value = SmbAddState.PickingFolder(server, share, "", emptyList(), auth, customName)
            browseFolders(server, share, "", auth, customName)
        }
    }

    fun browseFolders(server: String, share: String, path: String, auth: AuthenticationContext?, customName: String? = null) {
        viewModelScope.launch {
            val actualAuth = auth ?: AuthenticationContext.anonymous()
            smbBrowser.listFolders(server, share, path, actualAuth).fold(
                onSuccess = { folders ->
                    _smbAddState.value = SmbAddState.PickingFolder(server, share, path, folders, auth, customName)
                },
                onFailure = { error ->
                    val isGuest = auth?.isAnonymous == true || auth?.username == "Guest"
                    val humanReadable = parseSmbError(error, server, isGuest)
                    _smbAddState.value = SmbAddState.Error(
                        message = humanReadable,
                        server = server
                    )
                }
            )
        }
    }

    fun onFolderSelected(folder: SmbFileItem) {
        val currentState = _smbAddState.value as? SmbAddState.PickingFolder ?: return
        browseFolders(currentState.server, currentState.share, folder.path, currentState.auth, currentState.customName)
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
            syncCoordinator.syncSource(source.id)
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
            syncCoordinator.syncSource(source.id)
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
            syncCoordinator.syncSource(source.id)
        }
    }
}
