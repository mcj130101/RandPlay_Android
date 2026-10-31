package com.mcj130101.randplayer.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mcj130101.randplayer.domain.model.AppTheme
import com.mcj130101.randplayer.domain.model.PlayerSelectionMode
import com.mcj130101.randplayer.domain.model.RandomMode
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                if (isSearchActive) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search settings...") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: General & Appearance
            SettingsSectionHeader(title = "General")

            SettingsGroupCard {
                val isDarkMode = when (settings.theme) {
                    AppTheme.DARK -> true
                    AppTheme.LIGHT -> false
                    AppTheme.SYSTEM -> isSystemInDarkTheme()
                }

                SettingRowToggle(
                    icon = Icons.Outlined.Palette,
                    title = "Switch themes",
                    description = "Toggle between Dark and Light mode",
                    checked = isDarkMode,
                    onCheckedChange = { checked ->
                        viewModel.setTheme(if (checked) AppTheme.DARK else AppTheme.LIGHT)
                    }
                )
            }

            // Section 2: Randomization
            SettingsSectionHeader(title = "Randomization")

            SettingsGroupCard {
                var randomMenuExpanded by remember { mutableStateOf(false) }

                SettingRowSelector(
                    icon = Icons.Outlined.Shuffle,
                    title = "Randomization mode",
                    valueText = when (settings.randomMode) {
                        RandomMode.UNIFORM -> "Uniform"
                        RandomMode.WEIGHTED -> "Weighted"
                        RandomMode.SMART_SHUFFLE -> "Smart Shuffle"
                        RandomMode.RANDOM_FOLDER -> "Random Folder"
                    },
                    onClick = { randomMenuExpanded = true },
                    dropdownMenu = {
                        DropdownMenu(
                            expanded = randomMenuExpanded,
                            onDismissRequest = { randomMenuExpanded = false }
                        ) {
                            RandomMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        viewModel.setRandomMode(mode)
                                        randomMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                )

                if (settings.randomMode == RandomMode.SMART_SHUFFLE) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    SettingSliderRow(
                        title = "History limit",
                        value = settings.smartShuffleHistoryLimit.toFloat(),
                        onValueChange = { viewModel.setSmartShuffleLimit(it.roundToInt()) },
                        valueRange = 10f..200f,
                        steps = 19,
                        displayValue = "${settings.smartShuffleHistoryLimit} videos"
                    )
                }
            }

            // Section 3: Playback & Automation
            SettingsSectionHeader(title = "Playback & Automation")

            SettingsGroupCard {
                SettingRowToggle(
                    icon = Icons.Outlined.Vibration,
                    title = "Play on shake",
                    description = "Shake device to play a random video",
                    checked = settings.playOnShakeEnabled,
                    onCheckedChange = { viewModel.setPlayOnShake(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                var playerMenuExpanded by remember { mutableStateOf(false) }

                SettingRowSelector(
                    icon = Icons.Outlined.PlayCircle,
                    title = "Video player app",
                    valueText = when (settings.playerSelectionMode) {
                        PlayerSelectionMode.ASK_EVERY_TIME -> "Ask Every Time"
                        PlayerSelectionMode.SYSTEM_DEFAULT -> "System Default"
                        PlayerSelectionMode.SPECIFIC_APP -> "Specific App"
                    },
                    onClick = { playerMenuExpanded = true },
                    dropdownMenu = {
                        DropdownMenu(
                            expanded = playerMenuExpanded,
                            onDismissRequest = { playerMenuExpanded = false }
                        ) {
                            PlayerSelectionMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        val label = when (mode) {
                                            PlayerSelectionMode.ASK_EVERY_TIME -> "Ask Every Time"
                                            PlayerSelectionMode.SYSTEM_DEFAULT -> "System Default"
                                            PlayerSelectionMode.SPECIFIC_APP -> "Specific App"
                                        }
                                        Text(label)
                                    },
                                    onClick = {
                                        viewModel.setPlayerSelectionMode(mode)
                                        playerMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                )
            }

            // Section 4: Synchronization
            SettingsSectionHeader(title = "Synchronization")

            SettingsGroupCard {
                SettingRowToggle(
                    icon = Icons.Outlined.Sync,
                    title = "Auto sync sources",
                    description = "Periodically sync SMB & local sources",
                    checked = settings.autoSyncEnabled,
                    onCheckedChange = { viewModel.setAutoSync(it) }
                )

                if (settings.autoSyncEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    SettingSliderRow(
                        title = "Sync interval",
                        value = settings.syncIntervalHours.toFloat(),
                        onValueChange = { viewModel.setSyncInterval(it.roundToInt()) },
                        valueRange = 1f..24f,
                        steps = 23,
                        displayValue = "${settings.syncIntervalHours}h"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    SettingRowToggle(
                        icon = Icons.Outlined.Sync,
                        title = "WiFi only",
                        description = "Sync only over WiFi networks",
                        checked = settings.syncWifiOnly,
                        onCheckedChange = { viewModel.setSyncWifiOnly(it) }
                    )
                }
            }

            // Section 5: Legal & App Info
            SettingsSectionHeader(title = "Legal & Info")

            SettingsGroupCard {
                SettingActionRow(
                    icon = Icons.Outlined.Info,
                    title = "About RandPlayer",
                    subtitle = "Version 1.6 • Random Video Player",
                    onClick = { }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                SettingActionRow(
                    icon = Icons.AutoMirrored.Outlined.Help,
                    title = "Data Privacy Terms",
                    subtitle = "Read our privacy guidelines",
                    onClick = { }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingRowToggle(
    icon: ImageVector,
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        )
    }
}

@Composable
private fun SettingRowSelector(
    icon: ImageVector,
    title: String,
    valueText: String,
    onClick: () -> Unit,
    dropdownMenu: @Composable () -> Unit
) {
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        dropdownMenu()
    }
}

@Composable
private fun SettingActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingSliderRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayValue: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        )
    }
}
