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
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        setContent {
            // Observe current theme from ThemeState so UI recomposes when it changes
            val currentTheme = com.lsj.mp7.ui.screens.ThemeState.currentTheme
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
