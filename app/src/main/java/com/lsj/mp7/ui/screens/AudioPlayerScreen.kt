package com.lsj.mp7.ui.screens

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lsj.mp7.ui.components.ModernLoadingIndicator
import androidx.compose.ui.res.painterResource
import com.lsj.mp7.R
import com.lsj.mp7.viewmodel.AudioListViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lsj.mp7.util.DurationFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures

import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioPlayerScreen(
    title: String = "", 
    uri: String = "",
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val listVm: AudioListViewModel = viewModel()
    val allAudios by listVm.items().collectAsState(initial = emptyList())
    val controller = com.lsj.mp7.player.PlayerConnection.controller.collectAsState(initial = null).value
    var isPlaying by remember { mutableStateOf(false) }
    var currentDurationMs by remember { mutableStateOf(0L) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var coverBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // Use current player state if no specific title/uri provided
    var currentTitle by remember { mutableStateOf(title) }
    var currentUri by remember { mutableStateOf(uri) }

    // React to controller updates for correct icons/state
    DisposableEffect(controller) {
        if (controller != null) {
            val listener = object : androidx.media3.common.Player.Listener {
                override fun onIsPlayingChanged(isPlayingNow: Boolean) { isPlaying = isPlayingNow }
                override fun onEvents(player: androidx.media3.common.Player, events: androidx.media3.common.Player.Events) {
                    currentTitle = if (title.isNotBlank()) title else player.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty()
                    currentUri = if (uri.isNotBlank()) uri else player.currentMediaItem?.localConfiguration?.uri?.toString().orEmpty()
                    isPlaying = player.isPlaying
                    val d = player.duration
                    if (d > 0) currentDurationMs = d
                }
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    val d = controller.duration
                    if (d > 0) currentDurationMs = d else currentDurationMs = 0L
                    currentPositionMs = 0L
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    val d = controller.duration
                    if (d > 0) currentDurationMs = d
                }
            }
            controller.addListener(listener)
            // initialize
            currentTitle = if (title.isNotBlank()) title else controller.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty()
            currentUri = if (uri.isNotBlank()) uri else controller.currentMediaItem?.localConfiguration?.uri?.toString().orEmpty()
            isPlaying = controller.isPlaying
            val d = controller.duration
            if (d > 0) currentDurationMs = d else currentDurationMs = 0L
            onDispose { controller.removeListener(listener) }
        } else { onDispose { } }
    }

    // Sync UI with existing controller session when opening without explicit uri
    LaunchedEffect(Unit) { /* controller already reflects current */ }

    // Only play if we have a specific URI and it's different from current
    LaunchedEffect(uri) { 
        if (uri.isNotBlank() && controller != null) {
            val item = androidx.media3.common.MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(title)
                        .build()
                ).build()
            controller.setMediaItem(item)
            controller.prepare()
            controller.play()
            // reset position; duration will be updated by listener once ready
            currentPositionMs = 0L
            currentDurationMs = 0L
        }
    }
    // Poll position while playing
    // val durationMs = ui.durationMs
    // val positionMs = ui.positionMs

    // Try to extract embedded cover art (best-effort)
    LaunchedEffect(currentUri) {
        if (currentUri.isNotBlank()) {
            runCatching {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(context, Uri.parse(currentUri))
                val art = mmr.embeddedPicture
                if (art != null) coverBitmap = BitmapFactory.decodeByteArray(art, 0, art.size).asImageBitmap()
                mmr.release()
            }
        }
    }

    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    // If dragged down significantly (positive y), trigger onBack
                    // We can use a threshold, e.g., 20 pixels
                    if (dragAmount > 20) {
                        onBack()
                    }
                }
            }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp)) // Increased from 32dp to 80dp to move everything down
        
        // Album artwork
        val art = coverBitmap
        if (art != null) {
            Image(
                bitmap = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(72.dp)
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
        


        // Song name only
        Text(
            text = currentTitle.ifBlank { "Now Playing" },
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(),
            color = colorScheme.onSurface,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        
        Spacer(Modifier.height(32.dp))
        
        // Progress bar
        var sliderValue by remember { mutableStateOf(0f) }
        var isScrubbing by remember { mutableStateOf(false) }
        // val duration = durationMs.coerceAtLeast(0L)
        // val pos = if (isScrubbing) (sliderValue * duration).toLong() else positionMs
        // val frac = if (duration > 0) pos.toFloat() / duration.toFloat() else 0f
        
        // Periodically tick position for smooth updates
        LaunchedEffect(controller, isPlaying) {
            while (controller != null && this.isActive) {
                currentPositionMs = controller.currentPosition
                val d = controller.duration
                if (d > 0) currentDurationMs = d
                delay(250)
            }
        }
        val duration = currentDurationMs
        val position = currentPositionMs
        val frac = if (duration > 0) (position.coerceAtLeast(0L).coerceAtMost(duration)).toFloat() / duration.toFloat() else 0f
        // Keep slider synced to playback when not actively scrubbing
        LaunchedEffect(frac, isScrubbing) {
            if (!isScrubbing) {
                sliderValue = frac
            }
        }
        Column(Modifier.fillMaxWidth()) {
            Slider(
                value = if (isScrubbing) sliderValue else frac,
                onValueChange = { v ->
                    isScrubbing = true
                    sliderValue = v
                },
                onValueChangeFinished = {
                    if (controller != null) {
                        val target = (sliderValue * (duration.coerceAtLeast(0L)).toFloat()).toLong()
                        controller.seekTo(target)
                    }
                    isScrubbing = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = colorScheme.onSurface,
                    activeTrackColor = colorScheme.onSurface,
                    inactiveTrackColor = colorScheme.outlineVariant
                ),
                enabled = duration > 0L,
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    DurationFormatter.format(position.coerceAtLeast(0L)),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                // val remaining = (duration - pos).coerceAtLeast(0L)
                Text(
                    DurationFormatter.format(duration.coerceAtLeast(0L)),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Main controls - only Previous, Play/Pause, Next
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button
            IconButton(
                onClick = { controller?.seekToPrevious() },
                enabled = controller?.hasPreviousMediaItem() == true,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = if (controller?.hasPreviousMediaItem() == true) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Play/Pause button
            IconButton(
                onClick = {
                    if (controller != null) {
                        if (controller.isPlaying) controller.pause() else controller.play()
                    }
                },
                modifier = Modifier
                    .size(72.dp)
                    .background(colorScheme.onSurface, RoundedCornerShape(36.dp))
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = colorScheme.surfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
            }
            
            // Next button
            IconButton(
                onClick = { controller?.seekToNext() },
                enabled = controller?.hasNextMediaItem() == true,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = if (controller?.hasNextMediaItem() == true) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        error?.let { 
            Text(
                it,
                color = colorScheme.error,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}


