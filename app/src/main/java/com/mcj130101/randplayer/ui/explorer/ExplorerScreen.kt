package com.mcj130101.randplayer.ui.explorer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mcj130101.randplayer.data.local.entity.VideoEntity
import com.mcj130101.randplayer.domain.browser.BrowserFileItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    viewModel: ExplorerViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<BrowserFileItem?>(null) }
    var fileToDelete by remember { mutableStateOf<BrowserFileItem?>(null) }

    val displayFiles = remember(uiState.searchQuery, uiState.files, uiState.indexedVideos) {
        if (uiState.searchQuery.isEmpty()) {
            uiState.files
        } else {
            val query = uiState.searchQuery.lowercase()
            val directMatches = uiState.files.filter { it.name.lowercase().contains(query) }
            val directPaths = directMatches.map { it.path }.toSet()

            val indexedMatches = uiState.indexedVideos.filter { video ->
                video.fileName.lowercase().contains(query) || video.path.lowercase().contains(query)
            }.map { video ->
                BrowserFileItem(
                    id = video.id,
                    name = video.fileName,
                    isDirectory = false,
                    size = video.sizeBytes ?: 0L,
                    lastModified = video.modifiedTimestamp ?: 0L,
                    path = video.path,
                    mimeType = null
                )
            }.filter { it.path !in directPaths }

            directMatches + indexedMatches
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(uiState.source?.name ?: "Explorer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshFiles() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                    var showSortMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, "Sort")
                    }
                    if (showSortMenu) {
                        androidx.compose.material3.ModalBottomSheet(
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                                Text(
                                    text = "Sort By",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                SortOrder.entries.forEach { order ->
                                    val label = order.name.replace("_", " ").lowercase()
                                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                                    androidx.compose.material3.ListItem(
                                        headlineContent = { Text(label) },
                                        modifier = Modifier.clickable {
                                            viewModel.setSortOrder(order)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = { showAddFolderDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, "New Folder")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) }
            )

            Breadcrumbs(
                crumbs = uiState.breadcrumbs,
                onCrumbClick = { viewModel.navigateTo(it) }
            )

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                ErrorView(uiState.error!!) { viewModel.refreshFiles() }
            } else {
                if (displayFiles.isEmpty()) {
                    EmptyExplorerView()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(displayFiles, key = { it.id.ifEmpty { it.path } }) { file ->
                            val videoEntity = remember(file, uiState.indexedVideos) {
                                uiState.indexedVideos.find { it.id == file.id || it.path == file.path }
                            }
                            FileListItem(
                                file = file,
                                videoEntity = videoEntity,
                                onClick = {
                                    if (file.isDirectory) viewModel.navigateTo(file.path)
                                    else viewModel.playFile(file)
                                },
                                onPlay = { viewModel.playFile(file) },
                                onToggleFavorite = { viewModel.toggleFavorite(file) },
                                onToggleExclude = { viewModel.toggleExclude(file) },
                                onRename = { fileToRename = file },
                                onDelete = { fileToDelete = file }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddFolderDialog) {
        InputDialog(
            title = "New Folder",
            onDismiss = { showAddFolderDialog = false },
            onConfirm = {
                viewModel.createDirectory(it)
                showAddFolderDialog = false
            }
        )
    }

    fileToRename?.let { file ->
        InputDialog(
            title = "Rename",
            initialValue = file.name,
            onDismiss = { fileToRename = null },
            onConfirm = {
                viewModel.renameFile(file, it)
                fileToRename = null
            }
        )
    }

    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${file.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFile(file)
                    fileToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        placeholder = { Text("Search files & subfolders...", style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f),
        ),
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun Breadcrumbs(crumbs: List<Pair<String, String>>, onCrumbClick: (String) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(crumbs.size) { index ->
            val (name, path) = crumbs[index]
            val isCurrent = index == crumbs.lastIndex

            SuggestionChip(
                onClick = { onCrumbClick(path) },
                label = { Text(name, style = MaterialTheme.typography.labelMedium) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    labelColor = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = null,
                modifier = Modifier.height(28.dp)
            )

            if (index < crumbs.lastIndex) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListItem(
    file: BrowserFileItem,
    videoEntity: VideoEntity?,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleExclude: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isFavorite = videoEntity?.isFavorite == true
    val isExcluded = videoEntity?.isExcluded == true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (file.isDirectory) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when {
                            file.isDirectory -> Icons.Default.Folder
                            file.name.lowercase().endsWith(".mp4") || file.name.lowercase().endsWith(".mkv") || file.name.lowercase().endsWith(".webm") || file.name.lowercase().endsWith(".avi") -> Icons.Default.Movie
                            file.name.lowercase().endsWith(".mp3") || file.name.lowercase().endsWith(".wav") || file.name.lowercase().endsWith(".ogg") || file.name.lowercase().endsWith(".flac") -> Icons.Default.Audiotrack
                            file.name.lowercase().endsWith(".jpg") || file.name.lowercase().endsWith(".png") || file.name.lowercase().endsWith(".gif") || file.name.lowercase().endsWith(".jpeg") -> Icons.Default.Image
                            else -> Icons.Default.Description
                        },
                        contentDescription = null,
                        tint = if (file.isDirectory) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isFavorite) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Favorite",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (!file.isDirectory) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${formatSize(file.size)} • ${formatDate(file.lastModified)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (showMenu) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showMenu = false }
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                if (!file.isDirectory) {
                    androidx.compose.material3.ListItem(
                        headlineContent = { Text("Play Video") },
                        leadingContent = { Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            showMenu = false
                            onPlay()
                        }
                    )
                    androidx.compose.material3.ListItem(
                        headlineContent = { Text(if (isFavorite) "Unmark Favorite" else "Mark Favorite") },
                        leadingContent = {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.clickable {
                            showMenu = false
                            onToggleFavorite()
                        }
                    )
                    androidx.compose.material3.ListItem(
                        headlineContent = { Text(if (isExcluded) "Include in Random Roll" else "Exclude from Random Roll") },
                        leadingContent = {
                            Icon(
                                imageVector = if (isExcluded) Icons.Default.CheckCircle else Icons.Default.Block,
                                contentDescription = null,
                                tint = if (isExcluded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        },
                        modifier = Modifier.clickable {
                            showMenu = false
                            onToggleExclude()
                        }
                    )
                    HorizontalDivider()
                }
                androidx.compose.material3.ListItem(
                    headlineContent = { Text("Rename") },
                    leadingContent = { Icon(Icons.Default.Edit, null) },
                    modifier = Modifier.clickable {
                        showMenu = false
                        onRename()
                    }
                )
                androidx.compose.material3.ListItem(
                    headlineContent = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
fun InputDialog(
    title: String,
    initialValue: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ErrorView(error: String, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry, shape = RoundedCornerShape(14.dp)) { Text("Retry") }
    }
}

@Composable
fun EmptyExplorerView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "No files or subfolders found",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (kotlin.math.log10(size.toDouble()) / kotlin.math.log10(1024.0)).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
