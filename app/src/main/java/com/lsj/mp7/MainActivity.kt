package com.lsj.mp7

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.lsj.mp7.util.MemoryManager
import com.lsj.mp7.player.PlayerConnection
import com.lsj.mp7.ui.screens.RootApp
import com.lsj.mp7.ui.navigation.AppNavigator
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

// 19. MainActivity with Proper Setup
class MainActivity : ComponentActivity() {
    private lateinit var memoryManager: MemoryManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize memory manager
        memoryManager = MemoryManager.getInstance()
        memoryManager.startMemoryMonitoring(this)
        
        // Initialize theme state
        com.lsj.mp7.ui.screens.ThemeState.initialize(this)
        
        // Initialize immersive mode state
        com.lsj.mp7.ui.screens.ImmersiveModeState.initialize(this)
        
        // Enable edge-to-edge display with transparent status bar
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            )
        )

        // Robust immersive mode: Allow window to extend into cutout area
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = 
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        setContent {
            // Observe current theme and immersive mode
            val currentTheme = com.lsj.mp7.ui.screens.ThemeState.currentTheme
            val isImmersive = com.lsj.mp7.ui.screens.ImmersiveModeState.isImmersiveMode
            
            // Dynamic system bars based on theme
            androidx.compose.runtime.LaunchedEffect(currentTheme) {
                val isDark = currentTheme == com.lsj.mp7.ui.screens.AppTheme.Dark || 
                             currentTheme == com.lsj.mp7.ui.screens.AppTheme.OLED
                
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        androidx.activity.SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (isDark) {
                        androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        androidx.activity.SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
            }
            
            // Apply immersive mode logic
            androidx.compose.runtime.LaunchedEffect(isImmersive) {
                val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                // Use BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE for immersive mode so bars float over content
                // when swiped, preserving the immersive feel.
                windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (isImmersive) {
                    windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                } else {
                    windowInsetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                }
            }

            com.lsj.mp7.ui.theme.Mp7Theme(appTheme = currentTheme) {
                // Check if we should start with player expanded
                val shouldStartExpanded = intent?.action == android.content.Intent.ACTION_VIEW && 
                                          (intent.type?.startsWith("audio/") == true || intent.type == "application/ogg")
                RootApp(startExpanded = shouldStartExpanded)
            }
        }

        checkAndHandleAudioIntent(intent)
        
        if (intent?.action == "com.lsj.mp7.OPEN_NOW_PLAYING") {
            AppNavigator.triggerOpenNowPlaying()
        }
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        checkAndHandleAudioIntent(intent)
        
        if (intent?.action == "com.lsj.mp7.OPEN_NOW_PLAYING") {
            AppNavigator.triggerOpenNowPlaying()
        }
    }

    private fun checkAndHandleAudioIntent(intent: android.content.Intent?) {
        if (intent?.action == android.content.Intent.ACTION_VIEW) {
            val uri = intent.data
            val type = intent.type ?: ""
            if (uri != null && (type.startsWith("audio/") || type == "application/ogg")) {
                // We need to wait for PlayerConnection to be ready. 
                // Since this runs on UI thread, we can launch a coroutine scope if needed, 
                // but Controller might be null initially in onCreate.
                // However, PlayerConnection.connect(this) is called in onStart. 
                // We should defer this slightly or handle it when controller becomes available.
                
                // For simplicity, launch a coroutine to wait for controller
                lifecycleScope.launch {
                    // Wait for controller
                     while (PlayerConnection.controller.value == null) {
                         delay(100)
                     }
                     val controller = PlayerConnection.controller.value
                     
                     if (controller != null) {
                        // Extract file name
                        var fileName = "External Audio"
                        try {
                            if (uri.scheme == "content") {
                                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                    if (cursor.moveToFirst()) {
                                        val nameIndex = cursor.getColumnIndex("_display_name")
                                        if (nameIndex != -1) {
                                            fileName = cursor.getString(nameIndex)
                                            // Remove extension if present
                                            val lastDot = fileName.lastIndexOf('.')
                                            if (lastDot > 0) fileName = fileName.substring(0, lastDot)
                                        }
                                    }
                                }
                            } else {
                                fileName = uri.lastPathSegment ?: "External Audio"
                            }
                        } catch (e: Exception) {
                            // Fallback to default
                        }

                        val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(fileName)
                            .setArtist("External Source")
                            .build()
                            
                        val mediaItem = androidx.media3.common.MediaItem.Builder()
                            .setUri(uri)
                            .setMediaMetadata(mediaMetadata)
                            .build()
                            
                        controller.setMediaItem(mediaItem)
                        controller.prepare()
                        controller.play()
                        
                        // Expand the player overlay
                        AppNavigator.triggerOpenNowPlaying()
                     }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Ensure MediaController connects to the service for UI controls
        PlayerConnection.connect(this)
    }

    override fun onStop() {
        // Keep service running but release the Activity-bound controller
        PlayerConnection.disconnect()
        super.onStop()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        memoryManager.stopMemoryMonitoring()
    }
}
