package com.lsj.mp7.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lsj.mp7.data.AdvancedSettings

/**
 * Advanced settings panel for audio playback
 */
@Composable
fun AdvancedSettingsPanel(
    settings: AdvancedSettings,
    onSettingsChange: (AdvancedSettings) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Advanced Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                // Audio Track Auto-Select
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Auto-select Audio Track",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Automatically choose best audio track",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.audioTrackAutoSelect,
                        onCheckedChange = { autoSelect ->
                            onSettingsChange(settings.copy(audioTrackAutoSelect = autoSelect))
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                // Subtitles Enabled
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Enable Subtitles",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Show subtitles during playback",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.subtitlesEnabled,
                        onCheckedChange = { enabled ->
                            onSettingsChange(settings.copy(subtitlesEnabled = enabled))
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                // Advanced Controls Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Show Advanced Controls",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Display additional player controls",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.showAdvancedControls,
                        onCheckedChange = { show ->
                            onSettingsChange(settings.copy(showAdvancedControls = show))
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                // Close button
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
    }
}
