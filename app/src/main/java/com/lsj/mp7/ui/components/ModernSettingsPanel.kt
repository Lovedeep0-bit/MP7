package com.lsj.mp7.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lsj.mp7.data.SimplePlaybackSettings

@Composable
fun ModernSettingsPanel(
    settings: SimplePlaybackSettings,
    onSettingsChange: (SimplePlaybackSettings) -> Unit,
    onClose: () -> Unit,
    isVisible: Boolean
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it })
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Card(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.8f),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                            text = "Playback Settings",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Playback Speed
                    Text(
                        text = "Playback Speed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            FilterChip(
                                onClick = {
                                    onSettingsChange(settings.copy(playbackSpeed = speed))
                                },
                                label = { Text("${speed}x") },
                                selected = settings.playbackSpeed == speed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Volume Control
                    Text(
                        text = "Volume",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(
                            onClick = {
                                onSettingsChange(settings.copy(isMuted = !settings.isMuted))
                            }
                        ) {
                            Icon(
                                if (settings.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (settings.isMuted) "Unmute" else "Mute",
                                tint = if (settings.isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = if (settings.isMuted) 0f else settings.volume,
                            onValueChange = { volume ->
                                onSettingsChange(settings.copy(volume = volume, isMuted = false))
                            },
                            modifier = Modifier.weight(1f),
                            valueRange = 0f..1f,
                            steps = 19
                        )

                        Text(
                            text = "${(if (settings.isMuted) 0f else settings.volume * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Auto-play setting
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auto-play videos",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Switch(
                            checked = settings.autoPlay,
                            onCheckedChange = { autoPlay ->
                                onSettingsChange(settings.copy(autoPlay = autoPlay))
                            }
                        )
                    }
                }
            }
        }
    }
}
