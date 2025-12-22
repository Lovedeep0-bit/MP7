package com.lsj.mp7.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.lsj.mp7.data.VideoFile

/**
 * Video Player Screen for MP4 playback
 * 
 * This is a basic structure. You can integrate your pre-built MP4 player code here.
 * 
 * Key areas to customize:
 * - Player initialization and configuration
 * - Video playback controls
 * - Gesture handling (tap, swipe, etc.)
 * - Progress tracking
 * - Settings panel
 * - Fullscreen mode
 */
@Composable
fun VideoPlayerScreen(
    title: String = "",
    uri: String = "",
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var player: ExoPlayer? by remember { mutableStateOf(null) }
    var isPlaying by remember { mutableStateOf(false) }
    
    // Initialize player
    LaunchedEffect(uri) {
        if (uri.isNotBlank()) {
            player?.release()
            player = ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(uri)
                setMediaItem(mediaItem)
                prepare()
                play()
            }
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(player) {
        onDispose {
            player?.release()
            player = null
        }
    }
    
    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text(title.ifBlank { "Video Player" }, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0x80000000)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues)
        ) {
            // Video player view
            player?.let { exoPlayer ->
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Observe player state
                LaunchedEffect(exoPlayer) {
                    exoPlayer.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                            isPlaying = isPlayingNow
                        }
                    })
                }
            } ?: run {
                // Loading or placeholder
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

/**
 * Alternative VideoPlayerScreen with VideoFile parameter
 */
@Composable
fun VideoPlayerScreen(
    video: VideoFile,
    onBack: () -> Unit = {}
) {
    VideoPlayerScreen(
        title = video.title,
        uri = video.uri,
        onBack = onBack
    )
}

