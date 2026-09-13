package com.example.randplayer.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.randplayer.domain.model.AppTheme
import com.example.randplayer.domain.model.PlayerSelectionMode
import com.example.randplayer.domain.model.RandomMode
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 16.dp),
            )

            SettingsSection(title = "Appearance", icon = Icons.Default.Palette) {
                ThemeSelector(
                    currentTheme = settings.theme,
                    onThemeSelected = { viewModel.setTheme(it) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                AccentColorSelector(
                    currentColor = settings.accentColorHex,
                    onColorSelected = { viewModel.setAccentColor(it) }
                )
            }

            SettingsSection(title = "Randomization", icon = Icons.Default.Shuffle) {
                RandomModeSelector(
                    currentMode = settings.randomMode,
                    onModeSelected = { viewModel.setRandomMode(it) }
                )

                if (settings.randomMode == RandomMode.SMART_SHUFFLE) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingSlider(
                        label = "History Limit",
                        value = settings.smartShuffleHistoryLimit.toFloat(),
                        onValueChange = { viewModel.setSmartShuffleLimit(it.roundToInt()) },
                        valueRange = 10f..200f,
                        steps = 19,
                        displayValue = settings.smartShuffleHistoryLimit.toString()
                    )
                }
            }

            SettingsSection(title = "Play On Shake", icon = Icons.Default.Vibration) {
                SettingSwitch(
                    label = "Play On Shake",
                    description = "Enable Device Shaking to Play Random Videos.",
                    checked = settings.playOnShakeEnabled,
                    onCheckedChange = { viewModel.setPlayOnShake(it) }
                )

            }

            SettingsSection(title = "Synchronization", icon = Icons.Default.Sync) {
                SettingSwitch(
                    label = "Auto Sync",
                    description = "Periodically check for new videos",
                    checked = settings.autoSyncEnabled,
                    onCheckedChange = { viewModel.setAutoSync(it) }
                )

                if (settings.autoSyncEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingSlider(
                        label = "Sync Interval",
                        value = settings.syncIntervalHours.toFloat(),
                        onValueChange = { viewModel.setSyncInterval(it.roundToInt()) },
                        valueRange = 1f..24f,
                        steps = 23,
                        displayValue = "${settings.syncIntervalHours}h"
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                SettingSwitch(
                    label = "WiFi Only",
                    checked = settings.syncWifiOnly,
                    onCheckedChange = { viewModel.setSyncWifiOnly(it) }
                )

                SettingSwitch(
                    label = "Sync while charging",
                    checked = settings.syncWhileCharging,
                    onCheckedChange = { viewModel.setSyncWhileCharging(it) }
                )
            }

            SettingsSection(title = "Playback", icon = Icons.Default.PlayCircle) {
                PlayerModeSelector(
                    currentMode = settings.playerSelectionMode,
                    onModeSelected = { viewModel.setPlayerSelectionMode(it) }
                )

                if (settings.playerSelectionMode == PlayerSelectionMode.SPECIFIC_APP) {
                    Spacer(modifier = Modifier.height(16.dp))
                    PlayerSelector(
                        currentPackage = settings.preferredPlayerPackage,
                        onPlayerSelected = { viewModel.setPreferredPlayer(it) }
                    )
                }

            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 1.dp,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SettingSwitch(
    label: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
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
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun SettingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayValue: String
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = displayValue, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
fun ThemeSelector(currentTheme: AppTheme, onThemeSelected: (AppTheme) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("App Theme", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Box {
            Surface(
                onClick = { expanded = true },
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentTheme.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                AppTheme.entries.forEach { theme ->
                    DropdownMenuItem(
                        text = { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        onClick = {
                            onThemeSelected(theme)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AccentColorSelector(currentColor: String, onColorSelected: (String) -> Unit) {
    val colors = listOf(
        "#3B82F6", // Blue
        "#8B5CF6", // Purple
        "#10B981", // Green
        "#EF4444", // Red
        "#F59E0B", // Orange
        "#EC4899", // Pink
        "#06B6D4"  // Cyan
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Accent Color",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            colors.forEach { hex ->
                val color = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
                val isSelected = currentColor.equals(hex, ignoreCase = true)
                
                Surface(
                    onClick = { onColorSelected(hex) },
                    modifier = Modifier.size(36.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = color,
                    border = if (isSelected) {
                        androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
                    } else null,
                    tonalElevation = if (isSelected) 8.dp else 0.dp
                ) {
                    if (isSelected) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = if (hex == "#F59E0B") androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerModeSelector(currentMode: PlayerSelectionMode, onModeSelected: (PlayerSelectionMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Player Logic", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Box {
            Surface(
                onClick = { expanded = true },
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayText = when (currentMode) {
                        PlayerSelectionMode.ASK_EVERY_TIME -> "Ask Every Time"
                        PlayerSelectionMode.SYSTEM_DEFAULT -> "System Default"
                        PlayerSelectionMode.SPECIFIC_APP -> "Specific App"
                    }
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                PlayerSelectionMode.entries.forEach { mode ->
                    val itemText = when (mode) {
                        PlayerSelectionMode.ASK_EVERY_TIME -> "Ask Every Time"
                        PlayerSelectionMode.SYSTEM_DEFAULT -> "System Default"
                        PlayerSelectionMode.SPECIFIC_APP -> "Choose Specific App"
                    }
                    DropdownMenuItem(
                        text = { Text(itemText) },
                        onClick = {
                            onModeSelected(mode)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerSelector(currentPackage: String?, onPlayerSelected: (String?) -> Unit) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    
    val players = remember<List<Pair<String, String>>> {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            type = "video/*"
        }
        val pm = context.packageManager
        
        @Suppress("DEPRECATION")
        val resolved = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            pm.queryIntentActivities(intent, 0)
        }

        resolved.map {
            it.activityInfo.packageName to it.loadLabel(pm).toString()
        }
        .filter { it.first != context.packageName } // Don't show self
        .distinctBy { it.first }
        .sortedBy { it.second }
    }

    val selectedLabel = players.find { it.first == currentPackage }?.second ?: "System Default"

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Preferred Player", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text(
            "Directly launch this app for playback",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Surface(
            onClick = { expanded = true },
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PlayCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = selectedLabel,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("System Default (Ask Always)") },
                    onClick = {
                        onPlayerSelected(null)
                        expanded = false
                    }
                )
                players.forEach { (pkg, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onPlayerSelected(pkg)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RandomModeSelector(currentMode: RandomMode, onModeSelected: (RandomMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Selection Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Box {
            Surface(
                onClick = { expanded = true },
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentMode.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                RandomMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        onClick = {
                            onModeSelected(mode)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
