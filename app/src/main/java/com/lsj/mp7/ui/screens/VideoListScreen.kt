package com.lsj.mp7.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.lsj.mp7.data.VideoFile
import com.lsj.mp7.data.VideoFolder
import com.lsj.mp7.data.VideoRepository
import com.lsj.mp7.util.DurationFormatter
import com.lsj.mp7.viewmodel.VideoListViewModel

@Composable
fun VideoListScreen(
    title: String,
    itemsList: List<VideoFile>,
    onItemClick: (VideoFile) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, color = colorScheme.onSurface) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (itemsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "No videos found",
                        color = colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                )
            ) {
                items(itemsList) { video ->
                    VideoCard(
                        video = video,
                        onClick = { onItemClick(video) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoCard(
    video: VideoFile,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Video thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f) // Video aspect ratio
                .clip(RoundedCornerShape(12.dp))
                .background(colorScheme.surfaceVariant)
                .border(1.dp, colorScheme.outline, RoundedCornerShape(12.dp))
        ) {
            // Video thumbnail using coil-video
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(video.uri)
                        .setParameter("video_frame_millis", 1000) // Get frame at 1 second
                        .allowHardware(false)
                        .crossfade(true)
                        .build(),
                    imageLoader = com.lsj.mp7.util.ImageLoaderProvider.get(context)
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Play button overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color(0x80000000),
                            RoundedCornerShape(50)
                        )
                        .padding(12.dp)
                )
            }
            
            // Duration overlay
            if (video.duration > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Text(
                        text = DurationFormatter.format(video.duration),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .background(Color(0x80000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Video info
        Text(
            text = video.title,
            color = colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Video folder grid screen
@Composable
fun VideoFolderScreen(
    folders: List<VideoFolder>,
    onFolderClick: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colorScheme.background
    ) { paddingValues ->
        if (folders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "No video folders found",
                        color = colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                )
            ) {
                items(folders) { folder ->
                    VideoFolderCard(
                        folder = folder,
                        onClick = { onFolderClick(folder.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoFolderCard(
    folder: VideoFolder,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Video folder thumbnail placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f) // Video aspect ratio
                .clip(RoundedCornerShape(12.dp))
                .background(colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            // Video count overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Text(
                    text = "${folder.videoCount}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .background(Color(0x80000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Folder info
        Text(
            text = folder.name,
            color = colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(Modifier.height(2.dp))
        
        Text(
            text = "${folder.videoCount} videos",
            color = colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Video List Screen with Progress Tracking
 * Shows videos with progress bars (matches MainActivity functionality)
 */
@Composable
fun VideoListScreenWithProgress(
    title: String,
    itemsList: List<VideoFile>,
    videoRepository: VideoRepository,
    onItemClick: (VideoFile) -> Unit,
    onBack: () -> Unit = {}
) {
    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color.White) },
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
                    containerColor = Color(0xFF121212)
                )
            )
        }
    ) { paddingValues ->
        if (itemsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color(0xFFB3B3B3),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "No videos found",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                )
            ) {
                items(itemsList) { video ->
                    VideoCardWithProgress(
                        video = video,
                        onClick = { onItemClick(video) }
                    )
                }
            }
        }
    }
}

/**
 * Video Card with Progress Bar
 */
@Composable
private fun VideoCardWithProgress(
    video: VideoFile,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Video thumbnail with progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E1E))
        ) {
            // Video thumbnail
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(video.uri)
                        .setParameter("video_frame_millis", 1000)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Progress bar at bottom
            if (video.watchProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomStart)
                        .background(Color(0x40000000))
                ) {
                    LinearProgressIndicator(
                        progress = video.watchProgress,
                        modifier = Modifier.fillMaxSize(),
                        color = if (video.isCompleted) Color(0xFF1DB954) else Color(0xFF509BF5),
                        trackColor = Color.Transparent
                    )
                }
            }
            
            // Completed checkmark overlay
            if (video.isCompleted) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Completed",
                        tint = Color(0xFF1DB954),
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0x80000000), RoundedCornerShape(50))
                            .padding(4.dp)
                    )
                }
            }
            
            // Play button overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0x80000000), RoundedCornerShape(50))
                        .padding(12.dp)
                )
            }
            
            // Duration overlay
            if (video.duration > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Text(
                        text = DurationFormatter.format(video.duration),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .background(Color(0x80000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Video info
        Text(
            text = video.title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        // Progress text
        if (video.watchProgress > 0f) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (video.isCompleted) "Completed" else "${(video.watchProgress * 100).toInt()}% watched",
                color = Color(0xFFB3B3B3),
                fontSize = 12.sp
            )
        }
    }
}
