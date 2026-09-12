package com.example.randplayer.ui.sources

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.randplayer.data.scanner.DiscoveredServer
import com.example.randplayer.data.scanner.SmbFileItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSourceScreen(
    viewModel: SourceEditorViewModel,
    onNavigateBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Local", "SMB Share")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Source", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { 
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTabIndex) {
                    0 -> LocalSourceForm(viewModel, onNavigateBack)
                    1 -> SmbSourceDiscoveryFlow(viewModel, onNavigateBack)
                }
            }
        }
    }
}

@Composable
fun LocalSourceForm(viewModel: SourceEditorViewModel, onComplete: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var uri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { result ->
        if (result != null) {
            uri = result
            context.contentResolver.takePersistableUriPermission(
                result,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
        Text(
            text = "Local Folder",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Scan a folder on your device for videos",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Source Name") },
            placeholder = { Text("e.g. My Movies") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            onClick = { launcher.launch(null) }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (uri == null) "Select Folder" else "Folder Selected",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (uri != null) {
                        Text(
                            text = uri!!.path?.substringAfterLast(':') ?: "Path selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = {
                viewModel.addLocalSource(name, uri.toString(), true)
                onComplete()
            },
            enabled = name.isNotEmpty() && uri != null,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Add Source", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun SmbSourceDiscoveryFlow(viewModel: SourceEditorViewModel, onComplete: () -> Unit) {
    val state by viewModel.smbAddState.collectAsStateWithLifecycle()
    val discoveredServers by viewModel.discoveredServers.collectAsStateWithLifecycle()

    BackHandler(enabled = state !is SmbAddState.Discovering) {
        when (state) {
            is SmbAddState.Authenticating -> viewModel.resetSmbState()
            is SmbAddState.PickingShare -> viewModel.resetSmbState()
            is SmbAddState.PickingFolder -> viewModel.onBackFromFolder()
            is SmbAddState.Error -> viewModel.resetSmbState()
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val currentState = state) {
            is SmbAddState.Discovering -> {
                SmbDiscoveryList(
                    servers = discoveredServers,
                    onServerSelected = { viewModel.onServerSelected(it) },
                    onManualEntry = { viewModel.onManualServerEntry(it) }
                )
            }
            is SmbAddState.Authenticating -> {
                SmbAuthenticationForm(
                    server = currentState.server,
                    isConnecting = currentState.isConnecting,
                    onAuthenticate = { user, pass -> 
                        viewModel.authenticateAndListShares(currentState.server, user, pass, null)
                    }
                )
            }
            is SmbAddState.PickingShare -> {
                SmbSharePicker(
                    server = currentState.server,
                    shares = currentState.shares,
                    onShareSelected = { shareName ->
                        viewModel.onShareSelected(currentState.server, shareName, currentState.auth)
                    }
                )
            }
            is SmbAddState.PickingFolder -> {
                SmbFolderPicker(
                    server = currentState.server,
                    share = currentState.share,
                    currentPath = currentState.currentPath,
                    folders = currentState.folders,
                    onFolderSelected = { viewModel.onFolderSelected(it) },
                    onConfirm = {
                        val name = if (currentState.currentPath.isEmpty()) currentState.share else currentState.currentPath.substringAfterLast('/')
                        viewModel.addSmbSource(name, currentState.server, "${currentState.share}/${currentState.currentPath}".trimEnd('/'), currentState.auth, true)
                        onComplete()
                    }
                )
            }
            is SmbAddState.Error -> {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Connection Failed",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = currentState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.resetSmbState() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

@Composable
fun SmbDiscoveryList(
    servers: List<DiscoveredServer>,
    onServerSelected: (DiscoveredServer) -> Unit,
    onManualEntry: (String) -> Unit
) {
    var manualHost by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(24.dp)) {
        Text("Network Servers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Scanning for available SMB shares...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (servers.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Searching network...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(servers) { server ->
                    ServerItem(server) { onServerSelected(server) }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Manual Connection", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = manualHost,
            onValueChange = { manualHost = it },
            label = { Text("IP or Hostname") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                IconButton(onClick = { onManualEntry(manualHost) }, enabled = manualHost.isNotEmpty()) {
                    Icon(Icons.Default.Dns, "Add", tint = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

@Composable
fun ServerItem(server: DiscoveredServer, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Computer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(server.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(server.ip ?: server.host, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SmbAuthenticationForm(
    server: String, 
    isConnecting: Boolean = false,
    onAuthenticate: (String?, String?) -> Unit
) {
    var isGuest by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Login to $server", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text("Connect as Guest", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = isGuest, 
                    onCheckedChange = { isGuest = it },
                    enabled = !isConnecting
                )
            }
        }

        if (!isGuest) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = username, 
                onValueChange = { username = it }, 
                label = { Text("Username") }, 
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = !isConnecting
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password, 
                onValueChange = { password = it }, 
                label = { Text("Password") }, 
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = !isConnecting,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        enabled = !isConnecting
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = { 
                val finalUser = if (isGuest) "Guest" else username
                val finalPass = if (isGuest) "" else password
                onAuthenticate(finalUser, finalPass) 
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isConnecting
        ) {
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Connecting...", style = MaterialTheme.typography.titleMedium)
            } else {
                Text(if (isGuest) "Connect as Guest" else "Login", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun SmbSharePicker(server: String, shares: List<String>, onShareSelected: (String) -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text("Select Share", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Choose a share on $server", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(shares) { share ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    onClick = { onShareSelected(share) }
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(share, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun SmbFolderPicker(
    server: String,
    share: String,
    currentPath: String,
    folders: List<SmbFileItem>,
    onFolderSelected: (SmbFileItem) -> Unit,
    onConfirm: () -> Unit
) {
    Column(modifier = Modifier.padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Select Folder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = if (currentPath.isEmpty()) "$server/$share" else ".../$share/$currentPath", 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            IconButton(
                onClick = onConfirm,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(Icons.Default.Check, "Select", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            if (folders.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("No subfolders", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(folders) { folder ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        onClick = { onFolderSelected(folder) }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(folder.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onConfirm, 
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Select Current Folder")
        }
    }
}
