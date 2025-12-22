package com.lsj.mp7.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lsj.mp7.util.DurationFormatter
import com.lsj.mp7.data.AudioFile
import com.lsj.mp7.util.ArtworkProvider
import com.lsj.mp7.R
import com.lsj.mp7.ui.components.MiniPlayer
import com.lsj.mp7.data.AlbumCoverStore
import androidx.compose.ui.graphics.asImageBitmap
import com.lsj.mp7.ui.components.ModernLoadingIndicator
import androidx.compose.ui.res.painterResource


@Composable
fun AudioListScreen(
    title: String,
    itemsList: List<AudioFile>,
    onItemClick: (AudioFile) -> Unit,
    onOpenNowPlaying: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var isShuffleOn by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    
    val filteredItems = if (searchQuery.isBlank()) {
        itemsList
    } else {
        itemsList.filter { audioFile ->
            audioFile.title.contains(searchQuery, ignoreCase = true) ||
            (audioFile.artist?.contains(searchQuery, ignoreCase = true) == true) ||
            (audioFile.album?.contains(searchQuery, ignoreCase = true) == true)
        }
    }
    
    // Remove: val uiState by collectAsState()
    // Remove: val currentUri = uiState.currentUri
    
     val controller = com.lsj.mp7.player.PlayerConnection.controller.collectAsState(initial = null).value
     var currentUri by remember { mutableStateOf(controller?.currentMediaItem?.localConfiguration?.uri?.toString()) }
     var isPlaying by remember { mutableStateOf(controller?.isPlaying ?: false) }
     DisposableEffect(controller) {
         if (controller != null) {
             val listener = object : androidx.media3.common.Player.Listener {
                 override fun onEvents(player: androidx.media3.common.Player, events: androidx.media3.common.Player.Events) {
                     currentUri = player.currentMediaItem?.localConfiguration?.uri?.toString()
                     isPlaying = player.isPlaying
                 }
                 override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                     currentUri = mediaItem?.localConfiguration?.uri?.toString()
                 }
                 override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                     isPlaying = isPlayingNow
                 }
             }
             controller.addListener(listener)
             currentUri = controller.currentMediaItem?.localConfiguration?.uri?.toString()
             isPlaying = controller.isPlaying
             onDispose { controller.removeListener(listener) }
         } else onDispose { }
     }
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
            // Spotify-like header
            PlaylistHeader(
                title = title,
                itemCount = itemsList.size,
                 onShuffleToggle = { 
                     isShuffleOn = !isShuffleOn
                 },
                isShuffleOn = isShuffleOn,
                onPlayAll = {
                    val listToPlay = if (isShuffleOn) itemsList.shuffled() else itemsList
                    if (controller != null && listToPlay.isNotEmpty()) {
                        val mediaItems = listToPlay.map { f ->
                            androidx.media3.common.MediaItem.Builder()
                                .setUri(f.uri)
                                .setMediaId(f.id.toString())
                                .setMediaMetadata(
                                    androidx.media3.common.MediaMetadata.Builder()
                                        .setTitle(f.title)
                                        .setArtist(f.artist ?: "")
                                        .setAlbumTitle(f.album ?: "")
                                        .build()
                                )
                                .build()
                        }
                        controller.setMediaItems(mediaItems, 0, 0L)
                        controller.prepare()
                        controller.play()
                    }
                },
                 searchQuery = searchQuery,
                 onSearchQueryChange = { searchQuery = it },
                 isSearchVisible = isSearchVisible,
                 onSearchVisibilityToggle = { isSearchVisible = !isSearchVisible },
                 isPlaying = isPlaying,
                 controller = controller,
                 onBack = onBack
             )
            
            // Song list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (filteredItems.isEmpty() && searchQuery.isNotBlank()) {
                    item {
                        EmptySearchResult(searchQuery = searchQuery)
                    }
                } else {
                    items(filteredItems, key = { it.id }) { item ->
                        // Remove: val isCurrent = currentUri != null && currentUri == item.uri.toString()
                        SongItem(
                            audioFile = item,
                            onClick = {
                                // Start playback via MediaController but do not navigate to player
                                val controller = com.lsj.mp7.player.PlayerConnection.controller.value
                                if (controller != null) {
                                    val mediaItems = filteredItems.map { f ->
                                        androidx.media3.common.MediaItem.Builder()
                                            .setUri(f.uri)
                                            .setMediaId(f.id.toString())
                                            .setMediaMetadata(
                                                androidx.media3.common.MediaMetadata.Builder()
                                                    .setTitle(f.title)
                                                    .setArtist(f.artist ?: "")
                                                    .setAlbumTitle(f.album ?: "")
                                                    .build()
                                            )
                                            .build()
                                    }
                                    val index = filteredItems.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                                    controller.setMediaItems(mediaItems, index, 0L)
                                    controller.prepare()
                                    controller.play()
                                }
                                onItemClick(item)
                            },
                            isCurrent = (currentUri != null && currentUri == item.uri)
                        )
                    }
                }
            }
        }
    }

@Composable
private fun PlaylistHeader(
    title: String,
    itemCount: Int,
    onShuffleToggle: () -> Unit,
    isShuffleOn: Boolean,
    onPlayAll: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearchVisible: Boolean,
    onSearchVisibilityToggle: () -> Unit,
    isPlaying: Boolean = false,
    controller: androidx.media3.common.Player?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val albumCoverStore = remember { AlbumCoverStore(context) }
    val customCoverUri by albumCoverStore
        .getCustomCoverFlow(title)
        .collectAsState(initial = null)

    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        // Search bar (when visible)
        if (isSearchVisible) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSearchVisibilityToggle,
                    modifier = Modifier.size(40.dp)
                ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(Modifier.width(8.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                                Text(
                                    text = "Search in $title",
                                    color = colorScheme.onSurfaceVariant
                                )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline,
                        focusedTextColor = colorScheme.onSurface,
                        unfocusedTextColor = colorScheme.onSurface,
                        cursorColor = colorScheme.primary,
                        focusedContainerColor = colorScheme.surfaceVariant,
                        unfocusedContainerColor = colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        
        // Playlist info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Playlist artwork
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.surfaceVariant)
                        .border(1.dp, colorScheme.outline, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                if (customCoverUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(customCoverUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = colorScheme.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$itemCount songs",
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
            
            // Search button
            IconButton(
                onClick = onSearchVisibilityToggle,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isSearchVisible) colorScheme.onSurface else colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(1.dp, colorScheme.outline, RoundedCornerShape(20.dp))
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (isSearchVisible) colorScheme.surfaceVariant else colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button (same size style as shuffle)
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(1.dp, colorScheme.outline, RoundedCornerShape(20.dp))
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

             // Play button
            val playShape = RoundedCornerShape(50)
            Button(
                 onClick = {
                     if (isPlaying) {
                         // Pause the current playback
                         controller?.pause()
                     } else {
                         // Start playing
                         onPlayAll()
                     }
                 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPlaying) colorScheme.onSurface else colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                ,
                shape = playShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline)
            ) {
                 Icon(
                     if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                     contentDescription = if (isPlaying) "Pause" else "Play",
                     tint = if (isPlaying) colorScheme.surfaceVariant else colorScheme.onSurface,
                     modifier = Modifier.size(20.dp)
                 )
                 Spacer(Modifier.width(8.dp))
                 Text(
                     text = if (isPlaying) "Pause" else "Play",
                     color = if (isPlaying) colorScheme.surfaceVariant else colorScheme.onSurface,
                     fontWeight = FontWeight.Bold
                 )
             }
            
            // Shuffle button
            IconButton(
                onClick = onShuffleToggle,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isShuffleOn) colorScheme.onSurface else colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(1.dp, colorScheme.outline, RoundedCornerShape(20.dp))
            ) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffleOn) colorScheme.surfaceVariant else colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptySearchResult(searchQuery: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No results found",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Try searching for something else",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun SongItem(
    audioFile: AudioFile,
    onClick: () -> Unit,
    isCurrent: Boolean = false
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                color = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val art by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = audioFile.uri) {
            value = ArtworkProvider.loadAudioArtwork(context, Uri.parse(audioFile.uri))
        }

        // Song artwork
        if (art != null) {
            Image(
                bitmap = art!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        } else {
            val colorScheme = MaterialTheme.colorScheme
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colorScheme.surfaceVariant)
                    .border(1.dp, colorScheme.outline, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Song info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = audioFile.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Duration
        Text(
            text = DurationFormatter.format(audioFile.duration),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
    }
}


