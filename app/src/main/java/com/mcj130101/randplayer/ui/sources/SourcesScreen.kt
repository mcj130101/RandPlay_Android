package com.mcj130101.randplayer.ui.sources

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mcj130101.randplayer.data.local.entity.SourceType
import com.mcj130101.randplayer.data.local.entity.SyncStatus
import com.mcj130101.randplayer.data.local.entity.VideoSourceEntity
import com.mcj130101.randplayer.ui.logs.LogsScreen
import com.mcj130101.randplayer.ui.logs.LogsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    viewModel: SourcesViewModel,
    onBrowse: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFabMenu by remember { mutableStateOf(false) }

    var selectedSourceForDetail by remember { mutableStateOf<VideoSourceEntity?>(null) }
    var selectedSourceForLogs by remember { mutableStateOf<VideoSourceEntity?>(null) }
    
    var showAddSourceType by remember { mutableStateOf<SourceType?>(null) }
    var editSourceId by remember { mutableStateOf<String?>(null) }
    
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val logsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addEditSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = Modifier.fillMaxSize()) {
        if (showFabMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showFabMenu = false }
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                SpeedDialFab(
                    expanded = showFabMenu,
                    onExpandedChange = { showFabMenu = it },
                    onLocalFolderClick = {
                        showAddSourceType = SourceType.LOCAL
                    },
                    onSmbShareClick = {
                        showAddSourceType = SourceType.SMB
                    }
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = "Video Sources",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 16.dp),
                )

                if (uiState.sources.isEmpty()) {
                    EmptySourcesView()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(uiState.sources, key = { it.id }) { source ->
                            SourceItem(
                                source = source,
                                onToggleEnabled = { enabled -> viewModel.toggleSourceEnabled(source.id, enabled) },
                                onDetail = { selectedSourceForDetail = source },
                                onBrowse = { onBrowse(source.id) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
    
    if (selectedSourceForDetail != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedSourceForDetail = null },
            sheetState = detailSheetState
        ) {
            val detailViewModel: SourceDetailViewModel = hiltViewModel(key = selectedSourceForDetail!!.id)
            androidx.compose.runtime.LaunchedEffect(selectedSourceForDetail!!.id) {
                detailViewModel.initialize(selectedSourceForDetail!!.id)
            }
            SourceDetailScreen(
                viewModel = detailViewModel,
                onNavigateBack = { selectedSourceForDetail = null },
                onEditSource = { id -> 
                    selectedSourceForDetail = null
                    editSourceId = id
                },
                onViewLogs = { id -> 
                    val src = uiState.sources.find { it.id == id }
                    if (src != null) {
                        selectedSourceForDetail = null
                        selectedSourceForLogs = src
                    }
                },
                onBrowse = { id -> 
                    selectedSourceForDetail = null
                    onBrowse(id) 
                }
            )
        }
    }
    
    if (selectedSourceForLogs != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedSourceForLogs = null },
            sheetState = logsSheetState
        ) {
            val logsViewModel: LogsViewModel = hiltViewModel(key = selectedSourceForLogs!!.id)
            androidx.compose.runtime.LaunchedEffect(selectedSourceForLogs!!.id) {
                logsViewModel.selectSource(selectedSourceForLogs!!.id)
            }
            LogsScreen(
                viewModel = logsViewModel
            )
        }
    }
    
    if (showAddSourceType != null || editSourceId != null) {
        ModalBottomSheet(
            onDismissRequest = { 
                showAddSourceType = null
                editSourceId = null
            },
            sheetState = addEditSheetState
        ) {
            val addEditViewModel: SourceEditorViewModel = hiltViewModel(key = editSourceId ?: "new")
            androidx.compose.runtime.LaunchedEffect(editSourceId, showAddSourceType) {
                if (editSourceId != null) {
                    addEditViewModel.loadSource(editSourceId!!)
                } else if (showAddSourceType != null) {
                    addEditViewModel.setType(showAddSourceType!!)
                }
            }
            AddSourceScreen(
                viewModel = addEditViewModel,
                onNavigateBack = { 
                    showAddSourceType = null
                    editSourceId = null
                }
            )
        }
    }
}

@Composable
private fun SpeedDialFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onLocalFolderClick: () -> Unit,
    onSmbShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }) + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }) + scaleOut(targetScale = 0.8f)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Option 1: Local Folder
                SpeedDialItem(
                    icon = Icons.Outlined.FolderOpen,
                    label = "Local Folder",
                    onClick = {
                        onExpandedChange(false)
                        onLocalFolderClick()
                    }
                )

                // Option 2: SMB Share
                SpeedDialItem(
                    icon = Icons.Outlined.Cloud,
                    label = "SMB Share",
                    onClick = {
                        onExpandedChange(false)
                        onSmbShareClick()
                    }
                )
            }
        }

        // Main FAB Button
        Surface(
            onClick = { onExpandedChange(!expanded) },
            shape = CircleShape,
            color = Color.Black,
            contentColor = Color.White,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
            shadowElevation = 8.dp,
            modifier = Modifier.size(58.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                val rotation by animateFloatAsState(
                    targetValue = if (expanded) 135f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "fab_rotation"
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (expanded) "Close menu" else "Add source menu",
                    tint = Color.White,
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer {
                            rotationZ = if (expanded) 0f else rotation
                        }
                )
            }
        }
    }
}

@Composable
private fun SpeedDialItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.85f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            shadowElevation = 4.dp,
            modifier = Modifier.padding(end = 12.dp)
        ) {
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color.Black,
            contentColor = Color.White,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
            shadowElevation = 6.dp,
            modifier = Modifier
                .padding(end = 6.dp)
                .size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun EmptySourcesView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No sources added",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Add a local folder or SMB share to start",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SourceItem(
    source: VideoSourceEntity,
    onToggleEnabled: (Boolean) -> Unit,
    onDetail: () -> Unit,
    onBrowse: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onBrowse,
                onLongClick = onDetail
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (source.type == SourceType.LOCAL) Icons.Default.FolderOpen else Icons.Default.SettingsEthernet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = source.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = source.enabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionChip(
                    onClick = { },
                    label = { Text(source.type.name, style = MaterialTheme.typography.labelSmall) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    border = null,
                    modifier = Modifier.height(24.dp)
                )

                when (source.lastSyncStatus) {
                    SyncStatus.IDLE -> {
                        SuggestionChip(
                            onClick = { },
                            label = { Text("Online", style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = null,
                            modifier = Modifier.height(24.dp)
                        )
                    }
                    SyncStatus.SYNCING -> {
                        SuggestionChip(
                            onClick = { },
                            label = { Text("Syncing...", style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            border = null,
                            modifier = Modifier.height(24.dp)
                        )
                    }
                    SyncStatus.OFFLINE, SyncStatus.ERROR -> {
                        SuggestionChip(
                            onClick = { },
                            label = { Text("Offline", style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            border = null,
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }
        }
    }
}
