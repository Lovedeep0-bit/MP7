package com.lsj.mp7.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Delete
// import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.painter.Painter
import kotlinx.coroutines.launch
import com.lsj.mp7.ui.components.MiniPlayer
import com.lsj.mp7.ui.components.ModernLoadingIndicator
import com.lsj.mp7.data.AudioFolder
import com.lsj.mp7.data.AlbumCoverStore
import com.lsj.mp7.data.AudioFile
import com.lsj.mp7.viewmodel.AudioListViewModel
import com.lsj.mp7.util.DurationFormatter
import com.lsj.mp7.player.PlayerConnection
import android.net.Uri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lsj.mp7.ui.screens.ThemeState
import com.lsj.mp7.ui.screens.AppTheme
import com.lsj.mp7.ui.screens.DefaultTab
import com.lsj.mp7.ui.screens.DefaultTabState
import com.lsj.mp7.utils.ScannedDirectoriesState
import androidx.compose.ui.res.painterResource
import com.lsj.mp7.R

enum class SearchFilter { Playlists, Tracks, Both }

enum class PlaylistSort { Alphabetical, SongCount }
enum class TrackSort { Alphabetical, Length }

@Composable
fun TabsRootScreen(
    homeViewModel: com.lsj.mp7.viewmodel.HomeViewModel,
    onOpenAudio: (AudioFile) -> Unit,
    onOpenAudioFolder: (String) -> Unit,
    onOpenNowPlaying: () -> Unit,
    onNavigateToVideo: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val homeVm = homeViewModel
    val uiState by homeVm.uiState.collectAsState(initial = homeVm.uiState.value)
    var isGrid by rememberSaveable(key = "mp3_view_style") { mutableStateOf(true) }

    val context = LocalContext.current
    val requiredPermissions = remember { getRequiredMediaPermissions() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        if (granted) {
            homeVm.refreshCounts()
        } else {
            // Show inline snackbar with action to open settings
            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", context.packageName, null)
            }
            // Launch snackbar asynchronously
            scope.launch {
                val res = snackbarHostState.showSnackbar(
                    message = "Permission required to scan media",
                    actionLabel = "Open Settings"
                )
                if (res == SnackbarResult.ActionPerformed) {
                    context.startActivity(intent)
                }
            }
        }
        // If denied, do nothing; lists remain empty until user triggers an action that can retry.
    }

    LaunchedEffect(Unit) {
        if (hasMediaPermissions(context)) {
            homeVm.refreshCounts()
        } else {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    // Search state
    var isSearchVisible by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchFilter by rememberSaveable { mutableStateOf(SearchFilter.Both) }
    
    // Sort state
    var playlistSort by rememberSaveable { mutableStateOf(PlaylistSort.Alphabetical) }
    var trackSort by rememberSaveable { mutableStateOf(TrackSort.Alphabetical) }
    var playlistSortAscending by rememberSaveable { mutableStateOf(true) }
    var trackSortAscending by rememberSaveable { mutableStateOf(true) }
    
    // Settings state - use shared state
    val showSettings = SettingsState.isSettingsOpen
    
    // Load audio files for track search
    val audioListViewModel: AudioListViewModel = viewModel()
    LaunchedEffect(Unit) {
        audioListViewModel.loadAudiosInMusicFolder()
    }
    val allAudioFiles by audioListViewModel.items().collectAsState(initial = emptyList())
    
    Box(modifier = Modifier.fillMaxSize()) {
    val colorScheme = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
            // Main UI content
            // Main UI content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                    // Header with icons and "MP3" title
                    PlaylistsHeader(
                        onSettingsClick = { SettingsState.openSettings() },
                        onMenuClick = { /* Unused */ },
                        onSearchClick = { 
                            isSearchVisible = !isSearchVisible 
                            if (!isSearchVisible) searchQuery = ""
                        },
                        isGrid = isGrid,
                        onGridClick = { isGrid = !isGrid },
                        isSearchVisible = isSearchVisible,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        searchFilter = searchFilter,
                        onSearchFilterChange = { searchFilter = it },
                        playlistSort = playlistSort,
                        trackSort = trackSort,
                        onPlaylistSortChange = { playlistSort = it },
                        onTrackSortChange = { trackSort = it },
                        playlistSortAscending = playlistSortAscending,
                        trackSortAscending = trackSortAscending,
                        onPlaylistSortDirectionChange = { playlistSortAscending = it },
                        onTrackSortDirectionChange = { trackSortAscending = it },
                        onNavigateToVideo = onNavigateToVideo
                    )
                    
                    // Filter and search logic
                    val folders = uiState.musicSubfolders
                    val filteredFolders = remember(folders, searchQuery, searchFilter, playlistSort, playlistSortAscending) {
                        val filtered = when {
                            searchQuery.isBlank() -> folders
                            searchFilter == SearchFilter.Tracks -> emptyList() // Tracks only
                            else -> folders.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        }
                        // Apply sorting
                        when (playlistSort) {
                            PlaylistSort.Alphabetical -> {
                                if (playlistSortAscending) {
                                    filtered.sortedBy { it.name.lowercase() }
                                } else {
                                    filtered.sortedByDescending { it.name.lowercase() }
                                }
                            }
                            PlaylistSort.SongCount -> {
                                if (playlistSortAscending) {
                                    filtered.sortedBy { it.audioCount }
                                } else {
                                    filtered.sortedByDescending { it.audioCount }
                                }
                            }
                        }
                    }
                    
                    val filteredTracks = remember(searchQuery, searchFilter, allAudioFiles, trackSort, trackSortAscending) {
                        val filtered = when {
                            searchFilter == SearchFilter.Playlists -> emptyList()
                            searchQuery.isBlank() -> emptyList()
                            else -> allAudioFiles.filter { 
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                (it.artist?.contains(searchQuery, ignoreCase = true) == true)
                            }
                        }
                        // Apply sorting
                        when (trackSort) {
                            TrackSort.Alphabetical -> {
                                if (trackSortAscending) {
                                    filtered.sortedBy { it.title.lowercase() }
                                } else {
                                    filtered.sortedByDescending { it.title.lowercase() }
                                }
                            }
                            TrackSort.Length -> {
                                if (trackSortAscending) {
                                    filtered.sortedBy { it.duration }
                                } else {
                                    filtered.sortedByDescending { it.duration }
                                }
                            }
                        }
                    }
                    
                    if (uiState.isLoading) {
                        LoadingPlaceholder()
                    } else if (filteredFolders.isEmpty() && filteredTracks.isEmpty()) {
                        if (searchQuery.isNotBlank()) {
                            EmptyHint("No results found matching \"$searchQuery\"")
                        } else {
                            EmptyHint("No folders in Music")
                        }
                    } else {
                        // Show playlists if filter allows
                        if (searchFilter != SearchFilter.Tracks && filteredFolders.isNotEmpty()) {
                            if (isGrid) {
                                AlbumGrid(
                                    folders = filteredFolders,
                                    onOpen = { onOpenAudioFolder(it) }
                                )
                            } else {
                                AlbumList(
                                    folders = filteredFolders,
                                    onOpen = { onOpenAudioFolder(it) }
                                )
                            }
                        }
                        // Show tracks if filter allows (minimal gap)
                        if (searchFilter != SearchFilter.Playlists && filteredTracks.isNotEmpty()) {
                            val topPadding = if (filteredFolders.isNotEmpty()) 0.dp else 8.dp
                            TrackList(
                                tracks = filteredTracks,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = topPadding, bottom = 8.dp),
                                onPlayTrack = { track, index ->
                                    val controller = PlayerConnection.controller.value
                                    if (controller != null) {
                                        val mediaItems = filteredTracks.map { f ->
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
                                        val startIndex = index.coerceIn(0, mediaItems.lastIndex)
                                        controller.setMediaItems(mediaItems, startIndex, 0L)
                                        controller.prepare()
                                        controller.play()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        

    }
}

@Composable
private fun TrackList(
    tracks: List<AudioFile>,
    modifier: Modifier = Modifier,
    onPlayTrack: (AudioFile, Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        itemsIndexed(tracks) { index, track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onPlayTrack(track, index) }
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = track.title,
                        color = colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!track.artist.isNullOrBlank()) {
                        Text(
                            text = track.artist ?: "",
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = DurationFormatter.format(track.duration),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PlaylistsHeader(
    onSettingsClick: () -> Unit,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    isGrid: Boolean,
    onGridClick: () -> Unit,
    isSearchVisible: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchFilter: SearchFilter,
    onSearchFilterChange: (SearchFilter) -> Unit,
    playlistSort: PlaylistSort,
    trackSort: TrackSort,
    onPlaylistSortChange: (PlaylistSort) -> Unit,
    onTrackSortChange: (TrackSort) -> Unit,
    playlistSortAscending: Boolean,
    trackSortAscending: Boolean,
    onPlaylistSortDirectionChange: (Boolean) -> Unit,
    onTrackSortDirectionChange: (Boolean) -> Unit,
    onNavigateToVideo: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.fillMaxWidth()) {
        // Header row - Layout: Search, Sort, MP3, Grid/List, Settings
        var showSortMenu by remember { mutableStateOf(false) }
        
        // Determine which sort options to show based on search filter
        val showPlaylistSort = searchFilter == SearchFilter.Playlists || searchFilter == SearchFilter.Both
        val showTrackSort = searchFilter == SearchFilter.Tracks || searchFilter == SearchFilter.Both
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Search/Close and Sort
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (isSearchVisible) "Close Search" else "Search",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (!isSearchVisible) {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Sort",
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(colorScheme.surfaceVariant)
                        ) {
                            // Unified Sort Options
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Alphabetical",
                                        color = colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onPlaylistSortChange(PlaylistSort.Alphabetical)
                                    onTrackSortChange(TrackSort.Alphabetical)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Count",
                                        color = colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onPlaylistSortChange(PlaylistSort.SongCount)
                                    onTrackSortChange(TrackSort.Length)
                                    showSortMenu = false
                                }
                            )
                            
                            // Divider
                            HorizontalDivider(
                                color = colorScheme.outline,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            
                            // Unified Direction Controls
                            val currentAscending = if (searchFilter == SearchFilter.Tracks) trackSortAscending else playlistSortAscending
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            onPlaylistSortDirectionChange(true)
                                            onTrackSortDirectionChange(true)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = "Ascending",
                                            tint = if (currentAscending) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            onPlaylistSortDirectionChange(false)
                                            onTrackSortDirectionChange(false)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowDownward,
                                            contentDescription = "Descending",
                                            tint = if (!currentAscending) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Center: Left arrow, "MP3" title, Right arrow
            if (!isSearchVisible) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onNavigateToVideo,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Go to MP4",
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "MP3",
                        color = colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    IconButton(
                        onClick = onNavigateToVideo,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Go to MP4",
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // Right side: Grid/List and Settings
            if (!isSearchVisible) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onGridClick) {
                        Icon(
                            if (isGrid) Icons.Default.GridView else Icons.Default.ViewList,
                            contentDescription = if (isGrid) "Grid View" else "List View",
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(96.dp))
            }
        }

        // Search bar overlay (when visible) - overlays the header but keeps close button accessible
        AnimatedVisibility(
            visible = isSearchVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            var showSearchFilterMenu by remember { mutableStateOf(false) }
            
            // Search bar positioned to overlay the header, starting after the close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 72.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                    .align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    trailingIcon = {
                        Box {
                            IconButton(onClick = { showSearchFilterMenu = true }) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = showSearchFilterMenu,
                                onDismissRequest = { showSearchFilterMenu = false },
                                modifier = Modifier.background(colorScheme.surfaceVariant)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Playlists",
                                            color = colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        onSearchFilterChange(SearchFilter.Playlists)
                                        showSearchFilterMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Tracks",
                                            color = colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        onSearchFilterChange(SearchFilter.Tracks)
                                        showSearchFilterMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Both",
                                            color = colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        onSearchFilterChange(SearchFilter.Both)
                                        showSearchFilterMenu = false
                                    }
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline,
                        focusedTextColor = colorScheme.onSurface,
                        unfocusedTextColor = colorScheme.onSurface,
                        cursorColor = colorScheme.primary,
                        focusedContainerColor = colorScheme.surfaceVariant,
                        unfocusedContainerColor = colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PlaylistMusicNoteGrid() {
    val colorScheme = MaterialTheme.colorScheme
    // Single centered music note placeholder
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surfaceVariant)
            .border(1.dp, colorScheme.outline, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
private fun AlbumGrid(
    folders: List<AudioFolder>, 
    onOpen: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp) // Extra bottom padding for mini player
    ) {
        items(folders) { folder ->
            AlbumCard(
                folder = folder,
                onClick = { onOpen(folder.name) }
            )
        }
    }
}

@Composable
private fun AlbumList(
    folders: List<AudioFolder>,
    onOpen: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp)
    ) {
        items(folders) { folder ->
            AlbumListItem(folder = folder, onClick = { onOpen(folder.name) })
        }
    }
}

@Composable
private fun AlbumListItem(
    folder: AudioFolder,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val albumCoverStore = remember { AlbumCoverStore(context) }
    val customCoverUri by albumCoverStore
        .getCustomCoverFlow(folder.name)
        .collectAsState(initial = null)
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.surfaceVariant)
            .border(1.dp, colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colorScheme.surface),
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
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                color = colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${folder.audioCount} ${if (folder.audioCount == 1) "song" else "songs"}",
                color = colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun AlbumCard(
    folder: AudioFolder,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val albumCoverStore = remember { AlbumCoverStore(context) }
    val scope = rememberCoroutineScope()
    
    // Persisted custom cover (flow-backed so it survives recomposition and restarts)
    val customCoverUri by albumCoverStore
        .getCustomCoverFlow(folder.name)
        .collectAsState(initial = null)
    
    // Image picker launcher with persistable URI permission
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // Ignore if permission can't be persisted
                }
                albumCoverStore.saveCustomCover(folder.name, it.toString())
            }
        }
    }
    
    // Show dialog for cover options
    var showCoverOptions by remember { mutableStateOf(false) }
    
    if (showCoverOptions) {
        AlertDialog(
            onDismissRequest = { showCoverOptions = false },
            title = null,
            text = null,
            confirmButton = {
                TextButton(
                    onClick = {
                        imagePickerLauncher.launch(arrayOf("image/*"))
                        showCoverOptions = false
                    }
                ) {
                    Text("Select Custom Cover", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            albumCoverStore.removeCustomCover(folder.name)
                        }
                        showCoverOptions = false
                    }
                ) {
                    Text("Remove Custom Cover", color = Color(0xFFE91429))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Click handled in artwork box (supports tap + long-press)
    ) {
        // Playlist artwork - custom cover or music note placeholder
        val colorScheme = MaterialTheme.colorScheme
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // Square shape
                .clip(RoundedCornerShape(8.dp))
                .background(colorScheme.surfaceVariant)
                .pointerInput(folder.name) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = {
                            showCoverOptions = true
                        }
                )
                }
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
                // Always use music note placeholder until a custom cover is chosen
                PlaylistMusicNoteGrid()
            }
            
            // Song count overlay with music note icon
                Box(
                    modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            colorScheme.surface.copy(alpha = 0.75f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${folder.audioCount}",
                        color = colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Playlist name below the card
        Spacer(Modifier.height(8.dp))
        Text(
            text = folder.name,
            color = colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
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
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val colorScheme = MaterialTheme.colorScheme
        Text(text, color = colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LoadingPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        val colorScheme = MaterialTheme.colorScheme
        ModernLoadingIndicator()
    }
}

@Composable
private fun FolderList(items: List<AudioFolder>, onOpen: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { it.id }) { folder ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpen(folder.name) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(folder.name, color = Color.White, fontSize = 16.sp)
                Text("${folder.audioCount}", color = Color(0xFFA0A0A0), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AudioList(items: List<AudioFile>, onOpen: (AudioFile) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { it.id }) { item ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpen(item) },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White)
                Column(Modifier.weight(1f)) {
                    Text(item.title, color = Color.White, fontSize = 16.sp)
                    // Duration unavailable here in MP3-only simplified list
                    // Text(DurationFormatter.format(item.duration), color = Color(0xFFA0A0A0), fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Transport controls removed in MP3-only simplified list
                }
            }
        }
    }
}

// @Composable
// MP3-only build: video components removed
/*private fun VideoGrid(videos: List<VideoFile>, onOpen: (VideoFile) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(videos) { video ->
            VideoCard(
                video = video,
                onClick = { onOpen(video) }
            )
        }
    }
}*/

// @Composable
/*private fun VideoFolderGrid(folders: List<AudioFolder>, onOpen: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(folders) { folder ->
            VideoFolderCard(
                folder = folder,
                onClick = { onOpen(folder.name) }
            )
        }
    }
}*/

// @Composable
/*private fun VideoCard(
    video: VideoFile,
    onClick: () -> Unit
) {
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
                .background(Color(0xFF1E1E1E))
        ) {
            // Video thumbnail using coil-video
            Image(
                painter = coil.compose.rememberAsyncImagePainter(
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data(video.uri)
                        .setParameter("video_frame_millis", 1000)
                        .crossfade(true)
                        .build()
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
                // MP4 overlay removed
            }
            
            // Duration overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Text(
                    // MP4 removed
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
        
        // Video info
        Text(
            text = video.title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start
        )
        
        Spacer(Modifier.height(2.dp))
        
        Text(
            // MP4 removed
            color = Color(0xFFB3B3B3),
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}*/

/*@Composable
private fun VideoFolderCard(
    folder: AudioFolder,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Video folder thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f) // Video aspect ratio
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E1E))
        ) {
            // Use actual video thumbnail if available
            if (folder.coverUri != null) {
                Image(
                    painter = coil.compose.rememberAsyncImagePainter(
                        model = coil.request.ImageRequest.Builder(context)
                            .data(folder.coverUri)
                            .setParameter("video_frame_millis", 1000)
                            .crossfade(true)
                            .build()
                    ),
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
            
            // Video count overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Text(
                    text = "${folder.audioCount}",
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
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start
        )
        
        Spacer(Modifier.height(2.dp))
        
        Text(
            text = "${folder.audioCount} videos",
            color = Color(0xFFB3B3B3),
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}*/

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with back button and "Settings" title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Settings",
                    color = colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            // Settings content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    // Theme setting
                    ThemeSettingItem()
                }
                item {
                    DefaultTabSettingItem()
                }
                item {
                    ScannedDirectoriesSettingItem()
                }
                item {
                    GithubRepoSettingItem()
                }
                
                item {
                    Spacer(Modifier.height(24.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Version 2.1",
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeSettingItem() {
    val context = LocalContext.current
    // Directly observe ThemeState so UI updates when theme changes
    val currentTheme = ThemeState.currentTheme
    var showDropdown by remember { mutableStateOf(false) }
    
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Theme label with dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Theme",
                color = colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Dropdown button
            Box {
                Row(
                    modifier = Modifier
                        .clickable { showDropdown = true }
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentTheme.name,
                        color = colorScheme.onSurface,
                        fontSize = 16.sp
                    )
                    Icon(
                        imageVector = if (showDropdown) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = "Theme dropdown",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false },
                    modifier = Modifier.background(colorScheme.surfaceVariant)
                ) {
                    DropdownMenuItem(
                        text = { 
                            Text(
                                "Light",
                                color = if (currentTheme == AppTheme.Light) colorScheme.onSurface else colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            ThemeState.setTheme(context, AppTheme.Light)
                            showDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { 
                            Text(
                                "Dark",
                                color = if (currentTheme == AppTheme.Dark) colorScheme.onSurface else colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            ThemeState.setTheme(context, AppTheme.Dark)
                            showDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { 
                            Text(
                                "OLED",
                                color = if (currentTheme == AppTheme.OLED) colorScheme.onSurface else colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            ThemeState.setTheme(context, AppTheme.OLED)
                            showDropdown = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DefaultTabSettingItem() {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(DefaultTabState.getDefaultTab(context)) }
    val colorScheme = MaterialTheme.colorScheme
    var showDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Default tab",
                color = colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Box {
                Row(
                    modifier = Modifier
                        .clickable { showDropdown = true }
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentTab == DefaultTab.MP3) "MP3" else "MP4",
                        color = colorScheme.onSurface,
                        fontSize = 16.sp
                    )
                    Icon(
                        imageVector = if (showDropdown) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = "Default tab dropdown",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false },
                    modifier = Modifier.background(colorScheme.surfaceVariant)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "MP3",
                                color = if (currentTab == DefaultTab.MP3) colorScheme.onSurface else colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            DefaultTabState.setDefaultTab(context, DefaultTab.MP3)
                            currentTab = DefaultTab.MP3
                            showDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "MP4",
                                color = if (currentTab == DefaultTab.MP4) colorScheme.onSurface else colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            DefaultTabState.setDefaultTab(context, DefaultTab.MP4)
                            currentTab = DefaultTab.MP4
                            showDropdown = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ScannedDirectoriesSettingItem() {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var dirs by remember { mutableStateOf(ScannedDirectoriesState.getDirectories(context).toList()) }
    var showList by remember { mutableStateOf(false) }

    val dirPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // ignore
            }
            ScannedDirectoriesState.addDirectory(context, uri.toString())
            dirs = ScannedDirectoriesState.getDirectories(context).toList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Scanned directories",
                color = colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "View scanned directories",
                    tint = colorScheme.onSurface,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable { showList = !showList }
                )
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add directory",
                    tint = colorScheme.onSurface,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable { dirPickerLauncher.launch(null) }
                )
            }
        }

    if (showList) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Determine which are defaults to disable deletion
                val defaults = ScannedDirectoriesState.getAllowedAudioDirectories(context)
                    .intersect(ScannedDirectoriesState.getAllowedVideoDirectories(context))
                    // Actually, simpler: ScannedDirectoriesState.isDefaultDirectory(path) logic
                
                dirs.forEach { dir ->
                    val isDefault = ScannedDirectoriesState.isDefaultDirectory(dir)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dir,
                            color = colorScheme.onSurface,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Delete button for user-added directories
                        if (!isDefault) {
                            IconButton(
                                onClick = {
                                    ScannedDirectoriesState.removeDirectory(context, dir)
                                    dirs = ScannedDirectoriesState.getDirectories(context).toList()
                                    // Trigger refresh
                                    // homeViewModel.refreshCounts() // This is not available here directly, 
                                    // but UI state change 'dirs' might suffice if scanning respects it dynamically 
                                    // or we need to restart usage.
                                    // Given context, we might need to assume relaunch or rely on ViewModel observing something.
                                    // For now, updating the list locally.
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove directory",
                                    tint = colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun GithubRepoSettingItem() {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.surfaceVariant)
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Lovedeep0-bit/MP7"))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Ignore if no browser found
                }
            }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Github Repository",
                color = colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "View source code",
                color = colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_github),
            contentDescription = "Github Repository",
            tint = colorScheme.onSurface,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(20.dp)
        )
    }
}
