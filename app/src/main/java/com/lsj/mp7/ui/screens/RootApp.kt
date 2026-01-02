package com.lsj.mp7.ui.screens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Observer
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.core.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavOptionsBuilder
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import coil.compose.rememberAsyncImagePainter
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import android.os.Build
import androidx.core.content.ContextCompat
import android.Manifest
import com.lsj.mp7.player.PlayerConnection
import com.lsj.mp7.viewmodel.HomeViewModel
import com.lsj.mp7.viewmodel.VideoListViewModel
import com.lsj.mp7.ui.screens.TabsRootScreen
import com.lsj.mp7.ui.screens.DefaultTab
import com.lsj.mp7.ui.screens.DefaultTabState
import com.lsj.mp7.ui.navigation.AppNavigator
import com.lsj.mp7.util.DurationFormatter
import androidx.compose.material.icons.filled.CheckCircle


@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun RootApp(startExpanded: Boolean = false) {

    val homeViewModel: HomeViewModel = viewModel()
    val uiState by homeViewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val context = LocalContext.current

    LaunchedEffect(Unit) { PlayerConnection.connect(context.applicationContext) }

    // Main navigation host
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val isAudioPlayerScreen = currentRoute?.startsWith("audio_player") == true
    val isPlaylistScreen = currentRoute?.startsWith("audio_list") == true
    val isVideoFolderScreen = currentRoute?.startsWith("video_list") == true
    val isSettingsOpen = SettingsState.isSettingsOpen
    
    val colorScheme = MaterialTheme.colorScheme
    val isImmersive = com.lsj.mp7.ui.screens.ImmersiveModeState.isImmersiveMode

    Scaffold(
        containerColor = colorScheme.background,
        contentWindowInsets = if (isImmersive) WindowInsets.displayCutout else WindowInsets.systemBars
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            var isPlayerExpanded by rememberSaveable { mutableStateOf(startExpanded) }

            LaunchedEffect(Unit) {
                AppNavigator.openNowPlaying.collectLatest {
                    isPlayerExpanded = true
                }
            }
            
            val defaultTab = remember { DefaultTabState.getDefaultTab(context) }

            // Handle back press to collapse player
            androidx.activity.compose.BackHandler(enabled = isPlayerExpanded) {
                isPlayerExpanded = false
            }

            val scope = rememberCoroutineScope()
            val initialPage = if (defaultTab == DefaultTab.MP3) 0 else 1
            val pagerState = rememberPagerState(initialPage = initialPage) { 2 }
            
            // Sync current page with back stack for correct handling if needed, 
            // though Pager handles main switching now.

            NavHost(
                navController = navController,
                startDestination = "home", // Unified home with Pager
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
                modifier = Modifier.fillMaxSize()
            ) {
                // Unified Home Screen with Swipe
                composable("home") {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondBoundsPageCount = 1 // Keep both tabs in memory for smooth swipe
                    ) { page ->
                        when (page) {
                            0 -> TabsRootScreen(
                                homeViewModel = homeViewModel,
                                onOpenAudio = { /* no-op navigate; playback handled without opening player */ },
                                onOpenAudioFolder = { folderName ->
                                    val name = java.net.URLEncoder.encode(folderName, "UTF-8")
                                    navController.navigate("audio_list/$name")
                                },
                                onOpenNowPlaying = {
                                    isPlayerExpanded = true
                                },
                                onNavigateToVideo = {
                                    // Use pager scroll instead of navigation
                                    scope.launch { 
                                        pagerState.animateScrollToPage(1) 
                                    }
                                }
                            )
                            1 -> VideoMainScreen(
                                onOpenVideoFolder = { folderName ->
                                    val name = java.net.URLEncoder.encode(folderName, "UTF-8")
                                    navController.navigate("video_list/$name")
                                },
                                navController = navController,
                                onNavigateToAudio = {
                                    // Use pager scroll instead of navigation
                                    scope.launch { 
                                        pagerState.animateScrollToPage(0) 
                                    }
                                }
                            )
                        }
                    }
                }

                // Audio Details
                composable("audio_list/{folder}") { backStackEntry ->
                    val arg = backStackEntry.arguments?.getString("folder") ?: return@composable
                    val folder = java.net.URLDecoder.decode(arg, "UTF-8")
                    
                    // Filter the pre-loaded audios from HomeViewModel's state
                    val allAudios = uiState.allAudios
                    val folderItems = remember(allAudios, folder) {
                        allAudios.filter { audio ->
                            val p = audio.path ?: ""
                            val parts = p.replace('\\', '/').split('/')
                            parts.getOrNull(parts.size - 2) == folder
                        }
                    }
                    
                    AudioListScreen(
                        title = folder, 
                        itemsList = folderItems,
                        onItemClick = { /* no-op navigate; playback handled without opening player */ },
                        onOpenNowPlaying = {
                            isPlayerExpanded = true
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "audio_player?title={title}&uri={uri}",
                    enterTransition = { slideInVertically(initialOffsetY = { it }) },
                    exitTransition = { slideOutVertically(targetOffsetY = { it }) },
                    popEnterTransition = { slideInVertically(initialOffsetY = { it }) },
                    popExitTransition = { slideOutVertically(targetOffsetY = { it }) }
                ) { backStackEntry ->
                    val rawTitle = backStackEntry.arguments?.getString("title") ?: ""
                    val title = try { java.net.URLDecoder.decode(rawTitle, "UTF-8") } catch (_: Exception) { rawTitle }
                    val rawUri = backStackEntry.arguments?.getString("uri") ?: return@composable
                    val uri = try { java.net.URLDecoder.decode(rawUri, "UTF-8") } catch (_: Exception) { rawUri }
                    AudioPlayerScreen(
                        title = title, 
                        uri = uri,
                        onBack = { navController.popBackStack() }
                    )
                }
                
                // Video Section - Main screen handled by Pager (page 1)

                composable("video_list/{folder}") { backStackEntry ->
                    val arg = backStackEntry.arguments?.getString("folder") ?: return@composable
                    val folder = java.net.URLDecoder.decode(arg, "UTF-8")
                    val context = LocalContext.current
                    val videoRepository = remember { com.lsj.mp7.data.VideoRepository(context) }
                    val lifecycleOwner = LocalLifecycleOwner.current
                    val scope = rememberCoroutineScope()
                    
                    // Activity result launcher for video player - refresh progress when returning
                    val videoPlayerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        // Refresh folders with updated progress immediately when video player returns
                        scope.launch {
                            videoRepository.refreshFoldersWithProgress()
                        }
                    }
                    
                    // Get videos for this folder with progress
                    var allFolders by remember { mutableStateOf<List<com.lsj.mp7.data.FolderItem>>(emptyList()) }
                    var isLoading by remember { mutableStateOf(false) }
                    
                    DisposableEffect(videoRepository) {
                        val observer = Observer<List<com.lsj.mp7.data.FolderItem>> { folderList ->
                            allFolders = folderList
                        }
                        val loadingObserver = Observer<Boolean> { loading ->
                            isLoading = loading
                        }
                        
                        videoRepository.folders.observe(lifecycleOwner, observer)
                        videoRepository.isLoading.observe(lifecycleOwner, loadingObserver)
                        
                        // Trigger initial observation
                        observer.onChanged(videoRepository.folders.value ?: emptyList())
                        loadingObserver.onChanged(videoRepository.isLoading.value ?: false)
                        
                        onDispose {
                            videoRepository.folders.removeObserver(observer)
                            videoRepository.isLoading.removeObserver(loadingObserver)
                        }
                    }
                    
                    // Refresh if folders are empty when screen appears
                    LaunchedEffect(folder) {
                        
                        // Always refresh on screen appear to ensure we have latest data
                        if (hasMediaPermissions(context)) {
                            // Refreshing video library...
                            scope.launch {
                                videoRepository.refreshVideoLibrary()
                            }
                        } else {
                            // No permissions, cannot refresh
                        }
                    }
                    
                    // Try to find folder - check exact match first, then case-insensitive
                    val folderItem = allFolders.find { it.name == folder } 
                        ?: allFolders.find { it.name.equals(folder, ignoreCase = true) }
                    
                    val videos = folderItem?.videos ?: emptyList()
                    
                    // Track if we've completed initial load to prevent showing "No videos found" during initial load
                    var hasCompletedInitialLoad by remember(folder) { mutableStateOf(false) }
                    var hasStartedLoading by remember(folder) { mutableStateOf(false) }
                    
                    LaunchedEffect(folder) {
                        hasCompletedInitialLoad = false // Reset when folder changes
                        hasStartedLoading = false
                    }
                    
                    // Mark that loading has started
                    LaunchedEffect(isLoading) {
                        if (isLoading) {
                            hasStartedLoading = true
                        }
                    }
                    
                    // Only mark as completed when we've started loading AND finished loading
                    // Add a small delay to ensure folder matching and data processing is complete
                    LaunchedEffect(isLoading, allFolders.size, videos.size) {
                        if (hasStartedLoading && !isLoading) {
                            // Wait a bit to ensure all data processing is complete
                            kotlinx.coroutines.delay(200)
                            // Only mark complete if we're still not loading and have checked the data
                            if (!isLoading) {
                                hasCompletedInitialLoad = true
                            }
                        }
                    }
                    
                    // Show loading if actively loading OR if videos are empty but we haven't completed initial load
                    // This prevents the "No videos found" message from flashing during initial load
                    val shouldShowLoading = isLoading || (videos.isEmpty() && !hasCompletedInitialLoad)
                    
                    if (folderItem != null) {
                    } else {
                    }
                    
                    VideoFolderDetailGrid(
                        folderName = folder,
                        videos = videos,
                        isLoading = shouldShowLoading,
                        onOpen = { video ->
                            openVideoPlayer(context, video, videoRepository, videoPlayerLauncher)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("video_player?title={title}&uri={uri}") { backStackEntry ->
                    val rawTitle = backStackEntry.arguments?.getString("title") ?: ""
                    val title = try { java.net.URLDecoder.decode(rawTitle, "UTF-8") } catch (_: Exception) { rawTitle }
                    val rawUri = backStackEntry.arguments?.getString("uri") ?: return@composable
                    val uri = try { java.net.URLDecoder.decode(rawUri, "UTF-8") } catch (_: Exception) { rawUri }
                    VideoPlayerScreen(
                        title = title,
                        uri = uri,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            
            // Show mini player for "home" (if on MP3 page) or "audio" routes
            // Only show on home if we are on the first page (MP3)
            val isMp3Page = if (currentRoute == "home") pagerState.currentPage == 0 else true
            val isMiniPlayerVisible = (currentRoute == "home" || currentRoute?.startsWith("audio") == true) && 
                isMp3Page &&
                currentRoute?.startsWith("audio_player") != true &&
                !isSettingsOpen

            AnimatedVisibility(
                visible = isMiniPlayerVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                com.lsj.mp7.ui.components.MiniPlayer(
                    onOpenNowPlaying = { isPlayerExpanded = true },
                    modifier = Modifier
                        .padding(horizontal = if (isImmersive) 0.dp else 12.dp)
                        .padding(bottom = 0.dp),
                    shape = if (isImmersive) androidx.compose.ui.graphics.RectangleShape else androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    enableFullBorder = !isImmersive
                )
            }

            // Animated Now Playing Overlay
            AnimatedVisibility(
                visible = isPlayerExpanded,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            ) {
                AudioPlayerScreen(
                    onBack = { isPlayerExpanded = false }
                )
            }

            // Settings Overlay
            AnimatedVisibility(
                visible = SettingsState.isSettingsOpen,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colorScheme.background,
                    tonalElevation = 8.dp
                ) {
                    SettingsScreen(
                        onBackClick = { SettingsState.closeSettings() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // Handle back press to close settings
                androidx.activity.compose.BackHandler {
                    SettingsState.closeSettings()
                }
            }
        }
    }
}

/**
 * Main Video Screen - shows video folders with progress tracking
 * Adapted from MP4 app MainActivity to Compose
 */
enum class VideoFolderSort { Alphabetical, VideoCount }

// Note: VideoFolderSort direction is handled separately with folderSortAscending state

@Composable
fun VideoMainScreen(
    onOpenVideoFolder: (String) -> Unit,
    navController: androidx.navigation.NavController,
    onNavigateToAudio: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // UI state - use key to ensure proper retention
    var isGrid by rememberSaveable(key = "mp4_view_style") { mutableStateOf(true) } // Videos use grid view (current view) by default
    var isSearchVisible by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var folderSort by rememberSaveable { mutableStateOf(VideoFolderSort.Alphabetical) }
    var folderSortAscending by rememberSaveable { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val videoRepository = remember { com.lsj.mp7.data.VideoRepository(context) }
    
    // State for folders and loading
    var folders by remember { mutableStateOf<List<com.lsj.mp7.data.FolderItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasCompletedInitialLoad by remember { mutableStateOf(false) }
    
    // Permission handling
    val requiredPermissions = remember { getRequiredMediaPermissions() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Activity result launcher for video player - refresh progress when returning
    val videoPlayerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Refresh folders with updated progress immediately when video player returns
        scope.launch {
            videoRepository.refreshFoldersWithProgress()
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        
        if (granted) {
            // Permissions granted, refresh video library
            scope.launch {
                videoRepository.refreshVideoLibrary()
            }
        } else {
            // Show snackbar with action to open settings
            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", context.packageName, null)
            }
            scope.launch {
                val res = snackbarHostState.showSnackbar(
                    message = "Video permission required to scan media",
                    actionLabel = "Open Settings"
                )
                if (res == SnackbarResult.ActionPerformed) {
                    context.startActivity(intent)
                }
            }
        }
    }
    
    // Observe LiveData - ensure it triggers UI updates
    DisposableEffect(videoRepository) {
        val foldersObserver = Observer<List<com.lsj.mp7.data.FolderItem>> { folderList ->
            folders = folderList
        }
        val loadingObserver = Observer<Boolean> { loading ->
            val wasLoading = isLoading
            isLoading = loading
            // Track when loading completes (transitions from true to false)
            if (wasLoading && !loading) {
                hasCompletedInitialLoad = true
            }
        }
        
        // Observe immediately
        videoRepository.folders.observe(lifecycleOwner, foldersObserver)
        videoRepository.isLoading.observe(lifecycleOwner, loadingObserver)
        
        // Trigger initial observation
        foldersObserver.onChanged(videoRepository.folders.value ?: emptyList())
        loadingObserver.onChanged(videoRepository.isLoading.value ?: false)
        
        onDispose {
            videoRepository.folders.removeObserver(foldersObserver)
            videoRepository.isLoading.removeObserver(loadingObserver)
        }
    }
    
    // Check permissions and refresh video library on first load
    LaunchedEffect(Unit) {
        val hasPermissions = hasMediaPermissions(context)
        
        // Check each permission individually
        if (Build.VERSION.SDK_INT >= 33) {
            val hasAudio = androidx.core.content.ContextCompat.checkSelfPermission(
                context, 
                android.Manifest.permission.READ_MEDIA_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasVideo = androidx.core.content.ContextCompat.checkSelfPermission(
                context, 
                android.Manifest.permission.READ_MEDIA_VIDEO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            val hasStorage = androidx.core.content.ContextCompat.checkSelfPermission(
                context, 
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        
        if (hasPermissions) {
            scope.launch {
                videoRepository.refreshVideoLibrary()
            }
        } else {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }
    
    // Also check when permissions might have changed (e.g., user granted from settings)
    LaunchedEffect(hasMediaPermissions(context)) {
        val hasPerms = hasMediaPermissions(context)
        if (hasPerms && folders.isEmpty() && !isLoading) {
            scope.launch {
                videoRepository.refreshVideoLibrary()
            }
        }
    }
    
    // Collect all videos from all folders for search
    val allVideos = remember(folders) {
        folders.flatMap { it.videos }
    }
    
    // Apply search and sort filters for folders
    val filteredFolders = remember(folders, searchQuery, folderSort, folderSortAscending) {
        val filtered = if (searchQuery.isBlank()) {
            folders
        } else {
            folders.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
        when (folderSort) {
            VideoFolderSort.Alphabetical -> {
                if (folderSortAscending) {
                    filtered.sortedBy { it.name.lowercase() }
                } else {
                    filtered.sortedByDescending { it.name.lowercase() }
                }
            }
            VideoFolderSort.VideoCount -> {
                if (folderSortAscending) {
                    filtered.sortedBy { it.videoCount }
                } else {
                    filtered.sortedByDescending { it.videoCount }
                }
            }
        }
    }
    
    // Filter videos by search query
    val filteredVideos = remember(allVideos, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allVideos.filter { 
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.displayName.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    val colorScheme = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
        VideoFolderScreenWithProgress(
                folders = filteredFolders,
                videos = filteredVideos,
            isLoading = isLoading || (folders.isEmpty() && !hasCompletedInitialLoad && hasMediaPermissions(context)),
                isGrid = isGrid,
                isSearchVisible = isSearchVisible,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                folderSort = folderSort,
                onFolderSortChange = { folderSort = it },
                folderSortAscending = folderSortAscending,
                onFolderSortDirectionChange = { folderSortAscending = it },
                onSearchClick = { 
                    isSearchVisible = !isSearchVisible 
                    if (!isSearchVisible) searchQuery = ""
                },
                onGridClick = { isGrid = !isGrid },
                onSettingsClick = { SettingsState.openSettings() },
                onMenuClick = { /* Unused */ },
            onFolderClick = { folder ->
                // Always navigate to folder view - pass folder name and ensure videos are loaded
                val name = java.net.URLEncoder.encode(folder.name, "UTF-8")
                navController.navigate("video_list/$name")
            },
            onVideoClick = { video ->
                openVideoPlayer(context, video, videoRepository, videoPlayerLauncher)
            },
            onNavigateToAudio = onNavigateToAudio
        )
        }
    }
}

/**
 * Helper function to open video player (matches MainActivity.openVideoPlayer)
 */
private fun openVideoPlayer(
    context: android.content.Context,
    video: com.lsj.mp7.data.VideoFile,
    repository: com.lsj.mp7.data.VideoRepository,
    launcher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>
) {
    val intent = android.content.Intent(context, VideoPlayerActivity::class.java).apply {
        putExtra("video_uri", video.uri)
        putExtra("video_title", video.title)
        putExtra("video_id", video.id)
        
        // Pass last position for resume playback (unless completed)
        val lastPosition = if (video.isCompleted) {
            0L // Start from beginning if completed
        } else {
            repository.getLastPlayPosition(video.id)
        }
        putExtra("last_position", lastPosition)
    }
    launcher.launch(intent)
}

@Composable
private fun VideosHeader(
    onSettingsClick: () -> Unit,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    isGrid: Boolean,
    onGridClick: () -> Unit,
    isSearchVisible: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    folderSort: VideoFolderSort,
    onFolderSortChange: (VideoFolderSort) -> Unit,
    folderSortAscending: Boolean,
    onFolderSortDirectionChange: (Boolean) -> Unit,
    onNavigateToAudio: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.fillMaxWidth()) {
        // Header row - Layout: Search, Sort, MP4, Grid/List, Settings
        var showSortMenu by remember { mutableStateOf(false) }
        
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
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Alphabetical",
                                        color = colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onFolderSortChange(VideoFolderSort.Alphabetical)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Video Count",
                                        color = colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onFolderSortChange(VideoFolderSort.VideoCount)
                                    showSortMenu = false
                                }
                            )
                            
                            // Add divider before direction buttons
                            HorizontalDivider(
                                color = colorScheme.outline,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            
                            // Ascending/Descending buttons
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
                                            onFolderSortDirectionChange(true)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = "Ascending",
                                            tint = if (folderSortAscending) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            onFolderSortDirectionChange(false)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowDownward,
                                            contentDescription = "Descending",
                                            tint = if (!folderSortAscending) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Center: Left arrow, "MP4" title, Right arrow
            if (!isSearchVisible) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onNavigateToAudio,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Go to MP3",
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "MP4",
                        color = colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    Spacer(Modifier.size(24.dp)) // Maintain balance with left icon
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
        
        // Search bar overlay (when visible)
        AnimatedVisibility(
            visible = isSearchVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
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

/**
 * Video Folder Screen with Progress Tracking
 * Matches the UI shown in the image - full-width cards with large thumbnails and text overlay
 */
@Composable
private fun VideoFolderScreenWithProgress(
    folders: List<com.lsj.mp7.data.FolderItem>,
    videos: List<com.lsj.mp7.data.VideoFile>,
    isLoading: Boolean,
    isGrid: Boolean,
    isSearchVisible: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    folderSort: VideoFolderSort,
    onFolderSortChange: (VideoFolderSort) -> Unit,
    folderSortAscending: Boolean,
    onFolderSortDirectionChange: (Boolean) -> Unit,
    onSearchClick: () -> Unit,
    onGridClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMenuClick: () -> Unit,
    onFolderClick: (com.lsj.mp7.data.FolderItem) -> Unit,
    onVideoClick: (com.lsj.mp7.data.VideoFile) -> Unit,
    onNavigateToAudio: () -> Unit
) {
        val colorScheme = MaterialTheme.colorScheme
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
        ) {
        // Header with navigation bar
        VideosHeader(
            onSettingsClick = onSettingsClick,
            onMenuClick = onMenuClick,
            onSearchClick = onSearchClick,
            isGrid = isGrid,
            onGridClick = onGridClick,
            isSearchVisible = isSearchVisible,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            folderSort = folderSort,
            onFolderSortChange = onFolderSortChange,
            folderSortAscending = folderSortAscending,
            onFolderSortDirectionChange = onFolderSortDirectionChange,
            onNavigateToAudio = onNavigateToAudio
        )
        
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (folders.isEmpty() && videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Videocam,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No results found" else "No videos found in Movies folder",
                        color = colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // Show folders if available
                if (folders.isNotEmpty()) {
                    if (isGrid) {
                        // Grid view: Full-width cards with thumbnails
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 8.dp)
                        ) {
                            items(folders) { folder ->
                                FolderItemCardWithThumbnail(
                                    folder = folder,
                                    onClick = { onFolderClick(folder) }
                                )
                            }
                        }
                    } else {
                        // List view: Compact items matching MP3 section style
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            items(folders) { folder ->
                                VideoFolderListItem(
                                    folder = folder,
                                    onClick = { onFolderClick(folder) }
                                )
                            }
                        }
                    }
                }
                
                // Show videos if search query is active and videos are found
                if (videos.isNotEmpty()) {
                    val topPadding = if (folders.isNotEmpty()) 0.dp else 8.dp
                    VideoList(
                        videos = videos,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = topPadding, bottom = 8.dp),
                        onPlayVideo = { video ->
                            onVideoClick(video)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Video Folder List Item - Compact list item matching MP3 section style
 */
@Composable
private fun VideoFolderListItem(
    folder: com.lsj.mp7.data.FolderItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    // Select a video for thumbnail (same logic as FolderItemCardWithThumbnail)
    val randomVideo = remember(folder.name, folder.videos.size) {
        if (folder.videos.isEmpty()) return@remember null
        val hash = folder.name.hashCode()
        val combinedHash = hash xor ThumbnailSessionSeed.seed
        val index = kotlin.math.abs(combinedHash) % folder.videos.size
        folder.videos[index]
    }
    
    val videoUri = remember(randomVideo) {
        randomVideo?.uri?.let { uri ->
            try {
                android.net.Uri.parse(uri)
            } catch (e: Exception) {
                null
            }
        }
    }
    
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
        // Thumbnail or placeholder
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            if (videoUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(videoUri)
                        .allowHardware(false)
                        .crossfade(true)
                        .build(),
                    imageLoader = com.lsj.mp7.util.ImageLoaderProvider.get(context),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    placeholder = remember {
                        androidx.compose.ui.graphics.painter.ColorPainter(colorScheme.surfaceVariant)
                    },
                    error = remember {
                        androidx.compose.ui.graphics.painter.ColorPainter(colorScheme.surfaceVariant)
                    }
                )
            } else {
                    Icon(
                    Icons.Default.Videocam,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        // Folder info
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
                text = "${folder.videoCount} ${if (folder.videoCount == 1) "video" else "videos"}",
                color = colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

/**
 * Video List - displays search results for videos
 */
@Composable
private fun VideoList(
    videos: List<com.lsj.mp7.data.VideoFile>,
    modifier: Modifier = Modifier,
    onPlayVideo: (com.lsj.mp7.data.VideoFile) -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        itemsIndexed(videos) { index, video ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.surfaceVariant)
                    .border(1.dp, colorScheme.outline, RoundedCornerShape(8.dp))
                    .clickable { onPlayVideo(video) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Video thumbnail
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(colorScheme.surface)
                ) {
                    val videoUri = try {
                        android.net.Uri.parse(video.uri)
                    } catch (e: Exception) {
                        null
                    }
                    
                    if (videoUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(videoUri)
                                .allowHardware(false)
                                .crossfade(true)
                                .build(),
                            imageLoader = com.lsj.mp7.util.ImageLoaderProvider.get(context),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                            placeholder = remember {
                                androidx.compose.ui.graphics.painter.ColorPainter(colorScheme.surfaceVariant)
                            },
                            error = remember {
                                androidx.compose.ui.graphics.painter.ColorPainter(colorScheme.surfaceVariant)
                            }
                        )
                    } else {
                        // Fallback icon
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Videocam,
                                contentDescription = null,
                                    tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                
                // Video info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = video.title,
                        color = colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (video.duration > 0) {
                        Text(
                            text = DurationFormatter.format(video.duration),
                                color = colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Session-level random seed for thumbnail selection.
 * Initialized once per app start, changes on app restart.
 */
private object ThumbnailSessionSeed {
    val seed: Int = kotlin.random.Random.nextInt()
}

/**
 * Folder Item Card - Full width card with large thumbnail and text overlay (matches image)
 */
@Composable
private fun FolderItemCardWithThumbnail(
    folder: com.lsj.mp7.data.FolderItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    // Full-width card with large thumbnail
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        // Thumbnail from random video - full width, large aspect ratio
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f) // Wide video aspect ratio
                .clip(RoundedCornerShape(12.dp))
                .background(colorScheme.surfaceVariant)
        ) {
            if (folder.videos.isNotEmpty()) {
                // Select a video based on folder name + session seed
                // This ensures the same folder shows the same thumbnail during the session,
                // but changes when the app is restarted (new session seed)
                val randomVideo = remember(folder.name, folder.videos.size) {
                    // Combine folder name hash with session seed for selection
                    val hash = folder.name.hashCode()
                    val combinedHash = hash xor ThumbnailSessionSeed.seed
                    val index = kotlin.math.abs(combinedHash) % folder.videos.size
                    folder.videos[index]
                }
                val videoUri = try {
                    android.net.Uri.parse(randomVideo.uri)
                } catch (e: Exception) {
                    android.util.Log.e("FolderItemCard", "Failed to parse URI: ${randomVideo.uri}", e)
                    null
                }
                
                if (videoUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(videoUri)
                            .allowHardware(false)
                            .crossfade(true)
                            .build(),
                        imageLoader = com.lsj.mp7.util.ImageLoaderProvider.get(context),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        placeholder = remember {
                            androidx.compose.ui.graphics.painter.ColorPainter(colorScheme.surfaceVariant)
                        },
                        error = remember {
                            androidx.compose.ui.graphics.painter.ColorPainter(colorScheme.surfaceVariant)
                        }
                    )
                } else {
                    // Fallback placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                
                // Progress bar for independent videos in main UI
                if (folder.name == "Movies" && folder.videoCount == 1) {
                    val video = folder.videos.firstOrNull() ?: return@Box
                    if (video.watchProgress in 0f..1f && video.watchProgress > 0f && !video.isCompleted) {
                        LinearProgressIndicator(
                            progress = video.watchProgress,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .height(4.dp),
                            color = colorScheme.primary,
                            trackColor = colorScheme.primary.copy(alpha = 0.3f)
                        )
                    }
                }
                
                // Tick mark for completed folder (all videos completed) or completed single video
                val allVideosCompleted = if (folder.name == "Movies" && folder.videoCount == 1) {
                    folder.videos.first().isCompleted
                } else {
                    folder.videos.isNotEmpty() && folder.videos.all { it.isCompleted }
                }
                
                if (allVideosCompleted) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                    tint = colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(24.dp)
                    )
                }
            }
            
            // Text overlay on bottom-left (matches image)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.BottomStart)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x80000000) // Dark gradient at bottom for text readability
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
                    .padding(start = 16.dp, bottom = 16.dp, end = 16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    // For single videos in "Movies" folder, show video title instead of folder name
                    val displayTitle = if (folder.name == "Movies" && folder.videoCount == 1 && folder.videos.isNotEmpty()) {
                        // Remove file extension from title
                        val videoTitle = folder.videos.first().title
                        val lastDotIndex = videoTitle.lastIndexOf('.')
                        if (lastDotIndex > 0) {
                            videoTitle.substring(0, lastDotIndex)
                        } else {
                            videoTitle
                        }
                    } else {
                        folder.name
                    }
                    
                    // Folder name or video title - large white text
                    Text(
                        text = displayTitle,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    
                    // For single videos in "Movies" folder, show duration instead of count
                    val displaySubtext = if (folder.name == "Movies" && folder.videoCount == 1 && folder.videos.isNotEmpty()) {
                        DurationFormatter.format(folder.videos.first().duration)
                    } else {
                        "${folder.videoCount} ${if (folder.videoCount == 1) "video" else "videos"}"
                    }
                    
                    // Video count or duration - smaller white text
                    Text(
                        text = displaySubtext,
                        color = Color(0xFFE0E0E0),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}


/**
 * Folder detail grid screen (matches screenshot: grid with thumbnail, title, duration)
 */
@Composable
private fun VideoFolderDetailGrid(
    folderName: String,
    videos: List<com.lsj.mp7.data.VideoFile>,
    isLoading: Boolean = false,
    onOpen: (com.lsj.mp7.data.VideoFile) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .padding(paddingValues)
        ) {
            // Header: Back arrow + folder name - positioned same as main UI nav bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Back arrow button
                IconButton(onClick = onBack) {
                Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                    tint = colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                )
                }
                // Folder name
                Text(
                    text = folderName,
                    color = colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (videos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                            text = "No videos found in '$folderName'",
                            color = colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Videos: ${videos.size}",
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                val sortedVideos = remember(videos) {
                    videos.sortedWith(compareBy<String> { it.lowercase() }.let { cmp ->
                        Comparator { a, b -> cmp.compare(a.title, b.title) }
                    })
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sortedVideos) { video ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(video) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1E1E1E))
                            ) {
                                val videoUri = try {
                                    android.net.Uri.parse(video.uri)
                                } catch (e: Exception) {
                                    android.util.Log.e("VideoFolderDetail", "Failed to parse URI: ${video.uri}", e)
                                    null
                                }
                                
                                if (videoUri != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(videoUri)
                                            .allowHardware(false)
                                            .crossfade(true)
                                            .build(),
                                        imageLoader = com.lsj.mp7.util.ImageLoaderProvider.get(
                                            context
                                        ),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        placeholder = remember {
                                            androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF1E1E1E))
                                        },
                                        error = remember {
                                            androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF1E1E1E))
                                        }
                                    )
                                } else {
                                    // Fallback placeholder
                            Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                    .background(colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Videocam,
                                            contentDescription = null,
                                            tint = colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }

                                // Watch progress bar (white) and completed tick
                                if (video.watchProgress in 0f..1f && video.watchProgress > 0f && !video.isCompleted) {
                                    LinearProgressIndicator(
                                        progress = video.watchProgress,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .fillMaxWidth()
                                            .height(4.dp),
                                        color = colorScheme.primary,
                                        trackColor = colorScheme.primary.copy(alpha = 0.3f)
                                    )
                                }
                                if (video.isCompleted) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                    tint = colorScheme.primary,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(12.dp)
                                            .size(24.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = video.title,
                                color = colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = DurationFormatter.format(video.duration),
                                color = colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

