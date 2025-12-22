package com.lsj.mp7.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lsj.mp7.viewmodel.HomeViewModel
import com.lsj.mp7.data.AudioFolder
import com.lsj.mp7.util.ArtworkProvider
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMp3Tab: () -> Unit,
    onOpenFolder: (AudioFolder) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = Color(0xFF121212),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF121212),
                modifier = Modifier.background(Color(0xFF121212))
            ) {
                val activeColor = Color.White
                val inactiveColor = Color(0xFFA0A0A0)
                NavigationBarItem(
                    selected = true,
                    onClick = onMp3Tab,
                    icon = { Icon(Icons.Default.MusicNote, contentDescription = null, tint = activeColor) },
                    label = { Text("MP7", color = activeColor, fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = activeColor,
                        unselectedIconColor = inactiveColor,
                        selectedTextColor = activeColor,
                        unselectedTextColor = inactiveColor,
                        indicatorColor = Color.Transparent,
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            
            // Spotify-like header
            Text(
                text = "Music",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(24.dp))
            
            // Enhanced search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF2A2A2A))
                    .clickable { /* TODO: Open search */ }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFFB3B3B3),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Search",
                    color = Color(0xFFB3B3B3),
                    fontSize = 16.sp
                )
            }
            
            Spacer(Modifier.height(32.dp))
            
            // Debug info
            Text(
                text = "Found ${uiState.folders.size} folders",
                color = Color(0xFFB3B3B3),
                fontSize = 12.sp
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Album grid
            if (uiState.folders.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.folders) { folder ->
                        AlbumCard(
                            folder = folder,
                            onClick = { onOpenFolder(folder) }
                        )
                    }
                }
            } else {
                // Fallback: Show loading or empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFFB3B3B3),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No music folders found",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Add some MP7 files to get started",
                            color = Color(0xFFB3B3B3),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(
    folder: AudioFolder,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    // Load artwork from the first audio file in the folder
    var artwork by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(folder.id) {
        isLoading = true
        // AudioFolder doesn't have coverUri, so we'll use a placeholder
        isLoading = false
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Album artwork
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E1E))
        ) {
            if (artwork != null && !isLoading) {
                Image(
                    bitmap = artwork!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Generate a colorful placeholder based on the folder name
                val placeholderColor = generateColorFromString(folder.name)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(placeholderColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            // Play button overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = "Play",
                    tint = Color(0xFF1DB954),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Album info
        Text(
            text = folder.name,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start
        )
        
        Spacer(Modifier.height(2.dp))
        
        Text(
            text = "${folder.audioCount} songs",
            color = Color(0xFFB3B3B3),
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
        Color(0xFFE8115B), // Pink
        Color(0xFF8C67AB), // Purple
        Color(0xFFBA5D07), // Brown
        Color(0xFF1E3264), // Dark blue
        Color(0xFFE8115B), // Magenta
        Color(0xFF148A08)  // Forest green
    )
    
    val hash = input.hashCode()
    val index = kotlin.math.abs(hash) % colors.size
    return colors[index]
}

@Composable
private fun CategoryItem(
    background: Color,
    icon: @Composable () -> Unit,
    title: String,
    count: Int,
    onClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(background, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) { Box(Modifier.size(24.dp)) { icon() } }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text("${count} files", color = Color(0xFFA0A0A0), fontSize = 14.sp)
        }
    }
}

@Composable
private fun FolderItem(folder: AudioFolder, onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFF2C2C2C), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) { Box(Modifier.size(24.dp)) { Icon(Icons.Default.MusicNote, null, tint = Color.White) } }
        Column(modifier = Modifier.weight(1f)) {
            Text(folder.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text("${folder.audioCount} files", color = Color(0xFFA0A0A0), fontSize = 14.sp)
        }
    }
}


