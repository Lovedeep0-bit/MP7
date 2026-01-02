package com.lsj.mp7.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.lsj.mp7.player.PlayerConnection
import com.lsj.mp7.util.ArtworkProvider
import android.net.Uri
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Immutable
private data class MiniPlayerUiState(
    val title: String = "",
    val effectiveUri: String? = null,
    val isPlaying: Boolean = false,
    val hasPrev: Boolean = false,
    val hasNext: Boolean = false
)

@Composable
fun MiniPlayer(
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    enableFullBorder: Boolean = true
) {
    val controller = PlayerConnection.controller.collectAsState(initial = null).value

    var uiState by remember { mutableStateOf(MiniPlayerUiState()) }

    // Stable callback reference
    val onOpenNowPlayingStable by rememberUpdatedState(onOpenNowPlaying)

    // Stable shapes to avoid reallocations
    val pillShape = remember { RoundedCornerShape(22.dp) }

    // Bridge MediaController callbacks into Compose state
    DisposableEffect(controller) {
        if (controller != null) {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    uiState = uiState.copy(isPlaying = isPlayingNow)
                }
                override fun onEvents(player: Player, events: Player.Events) {
                    uiState = uiState.copy(
                        title = player.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty(),
                        effectiveUri = player.currentMediaItem?.localConfiguration?.uri?.toString(),
                        hasPrev = player.hasPreviousMediaItem(),
                        hasNext = player.hasNextMediaItem(),
                        isPlaying = player.isPlaying
                    )
                }
            }
            controller.addListener(listener)
            // Initialize state immediately
            uiState = uiState.copy(
                title = controller.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty(),
                effectiveUri = controller.currentMediaItem?.localConfiguration?.uri?.toString(),
                hasPrev = controller.hasPreviousMediaItem(),
                hasNext = controller.hasNextMediaItem(),
                isPlaying = controller.isPlaying
            )
            onDispose { controller.removeListener(listener) }
        } else {
            onDispose { }
        }
    }

    // If neither controller is available nor we have any selection, hide the mini-player
    if (controller == null && uiState.effectiveUri.isNullOrBlank()) return

    // Load artwork directly from the current audio file, off main thread
    val context = LocalContext.current
    var artwork by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(uiState.effectiveUri) {
        artwork = null
        val uriString = uiState.effectiveUri
        if (!uriString.isNullOrBlank()) {
            try {
                val uri = Uri.parse(uriString)
                val bmp = withContext(Dispatchers.IO) {
                    ArtworkProvider.loadAudioArtwork(context, uri)
                }
                artwork = bmp
            } catch (_: Exception) {
                // Ignore errors for artwork loading
            }
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = colorScheme.surfaceVariant,
                shape = shape
            )
            .then(
                if (enableFullBorder) {
                    Modifier.border(
                        width = 1.dp,
                        color = colorScheme.outline.copy(alpha = 0.3f),
                        shape = shape
                    )
                } else {
                    Modifier.drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        val borderColor = colorScheme.outline.copy(alpha = 0.3f)
                        drawLine(
                            color = borderColor,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = strokeWidth
                        )
                    }
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left section (artwork + title) opens now playing
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onOpenNowPlayingStable() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album artwork thumbnail - positioned in front of song name
                if (artwork != null) {
                    Image(
                        bitmap = artwork!!.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    // Generate a colorful placeholder based on the song title
                    val placeholderColor = remember(uiState.title) { generateColorFromString(uiState.title) }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(placeholderColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                // Song name only
                Text(
                    text = if (uiState.title.isBlank()) "Now Playing" else uiState.title,
                    color = colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(16.dp))
            // Control buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous (compact, no pill background)
                IconButton(
                    onClick = { controller?.seekToPrevious() },
                    enabled = uiState.hasPrev,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = if (uiState.hasPrev) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Play/Pause
                IconButton(
                    onClick = {
                        if (controller != null) {
                            if (uiState.isPlaying) controller.pause() else controller.play()
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = colorScheme.onSurface,
                            shape = pillShape
                        )
                ) {
                    Icon(
                        if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                        tint = colorScheme.surface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Next (compact, no pill background)
                IconButton(
                    onClick = { controller?.seekToNext() },
                    enabled = uiState.hasNext,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = if (uiState.hasNext) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun generateColorFromString(input: String): Color {
    if (input.isBlank()) return Color(0xFF1DB954) // Spotify green as default
    val colors = listOf(
        Color(0xFF1DB954), // Spotify green
        Color(0xFF1ED760), // Light green
        Color(0xFF191414), // Dark gray
        Color(0xFF535353), // Medium gray
        Color(0xFFB3B3B3), // Light gray
        Color(0xFFE91429), // Red
        Color(0xFF056952), // Dark green
        Color(0xFF509BF5), // Blue
        Color(0xFFBC5900), // Orange
        Color(0xFFE8115B)  // Pink
    )
    val hash = input.hashCode()
    val index = kotlin.math.abs(hash) % colors.size
    return colors[index]
}


