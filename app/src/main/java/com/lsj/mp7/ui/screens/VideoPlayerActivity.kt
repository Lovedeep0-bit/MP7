package com.lsj.mp7.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import android.view.View
import android.view.ViewGroup
import android.media.AudioManager
import android.widget.ProgressBar
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import com.lsj.mp7.R
import com.lsj.mp7.data.VideoFile
import com.lsj.mp7.data.VideoRepository
import com.lsj.mp7.player.VideoPlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class AspectRatio(val displayName: String, val resizeMode: Int) {
    ORIGINAL("Original", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FIT_SCREEN("Fit Screen", AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
}

class VideoPlayerActivity : AppCompatActivity() {
    
    private lateinit var playerView: PlayerView
    private lateinit var videoPlayerManager: VideoPlayerManager
    private lateinit var videoRepository: VideoRepository
    private var videoFile: VideoFile? = null
    
    private lateinit var playPauseButton: ImageButton
    private lateinit var skipBackwardButton: ImageButton
    private lateinit var skipForwardButton: ImageButton
    private lateinit var tracksButton: ImageButton
    private lateinit var aspectRatioButton: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var currentTimeText: TextView
    private lateinit var totalTimeText: TextView
    private lateinit var titleText: TextView
    
    private var isControlsVisible = true
    private var showRemainingTime = false
    private var tracksDialog: AlertDialog? = null
    private var currentAspectRatio = AspectRatio.ORIGINAL
    private val hideControlsRunnable = Runnable { hideControls() }
    private val handler = Handler(Looper.getMainLooper())
    private val progressUpdateRunnable = Runnable { updateProgress() }
    
    // Gesture-related state
    private lateinit var audioManager: AudioManager
    
    private var gestureStartY = 0f
    private var gestureStartX = 0f
    private var isGestureActive = false
    private var gestureType = GestureType.NONE
    private var gestureStartBrightness = 0.5f
    private var gestureStartVolume = 0
    private var currentBrightness = 0.5f
    
    private enum class GestureType {
        NONE, BRIGHTNESS, VOLUME
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Remove hardcoded landscape to allow dynamic orientation
        // requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // Fullscreen setup
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        // Note: You'll need to create the XML layout file
        // For now, using a programmatic approach - see layout creation instructions below
        setContentView(R.layout.activity_video_player)
        
        initializeViews()
        setupPlayer()
        setupControls()
        
        // Load video from intent
        loadVideoFromIntent()
    }
    
    private fun initializeViews() {
        playerView = findViewById(R.id.playerView)
        playPauseButton = findViewById(R.id.playPauseButton)
        skipBackwardButton = findViewById(R.id.skipBackwardButton)
        skipForwardButton = findViewById(R.id.skipForwardButton)
        tracksButton = findViewById(R.id.tracksButton)
        aspectRatioButton = findViewById(R.id.aspectRatioButton)
        seekBar = findViewById(R.id.seekBar)
        currentTimeText = findViewById(R.id.currentTimeText)
        totalTimeText = findViewById(R.id.totalTimeText)
        titleText = findViewById(R.id.titleText)
        
        // Audio manager for volume control
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        
        // Initialize brightness from current window setting
        val layoutParams = window.attributes
        currentBrightness = if (layoutParams.screenBrightness < 0) 0.5f else layoutParams.screenBrightness
        
        // Hide default controls
        playerView.useController = false
    }
    
    private fun setupPlayer() {
        videoPlayerManager = VideoPlayerManager(this)
        videoRepository = VideoRepository(this)
        val player = videoPlayerManager.initializePlayer()
        
        playerView.player = player
        // Default to original aspect ratio fit
        applyAspectRatio(AspectRatio.ORIGINAL)
        
        // Player event listener
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlayPauseButton()
                
                if (playbackState == Player.STATE_READY) {
                    updateTimeDisplay()
                    seekBar.max = (player.duration / 1000).toInt()
                }

                if (playbackState == Player.STATE_ENDED) {
                    // Clear keep screen on when playback ends
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    // Mark video as completed and save progress
                    videoFile?.let { video ->
                        videoRepository.saveWatchProgress(video.id, 1.0f, player.duration)
                        videoRepository.saveLastPlayPosition(video.id, 0L) // Reset position for completed videos
                        
                        // Refresh folders with updated progress in background
                        CoroutineScope(Dispatchers.IO).launch {
                            videoRepository.refreshFoldersWithProgress()
                        }
                    }
                    // Auto-close when video finishes
                    finish()
                }
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayPauseButton()
                
                if (isPlaying) {
                    // Keep screen on while playing
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    startSeekBarUpdate()
                    startProgressTracking()
                    scheduleControlsHide()
                } else {
                    // Allow screen to turn off when paused/buffering
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    handler.removeCallbacks(hideControlsRunnable)
                    handler.removeCallbacks(progressUpdateRunnable)
                }
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val isVertical = videoSize.height > videoSize.width

                    
                    requestedOrientation = if (isVertical) {
                        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                    } else {
                        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                    }
                }
            }
        })
    }
    
    private fun setupControls() {
        playPauseButton.setOnClickListener {
            val player = videoPlayerManager.getPlayer()
            if (player?.isPlaying == true) {
                player.pause()
            } else {
                player?.play()
            }
        }
        
        skipBackwardButton.setOnClickListener {
            val player = videoPlayerManager.getPlayer()
            player?.let { p ->
                val currentPosition = p.currentPosition
                val newPosition = maxOf(0, currentPosition - 10000) // Skip back 10 seconds
                p.seekTo(newPosition)

            }
        }
        
        skipForwardButton.setOnClickListener {
            val player = videoPlayerManager.getPlayer()
            player?.let { p ->
                val currentPosition = p.currentPosition
                val duration = p.duration
                val newPosition = minOf(duration, currentPosition + 10000) // Skip forward 10 seconds
                p.seekTo(newPosition)

            }
        }
        
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val player = videoPlayerManager.getPlayer()
                    val seekPosition = progress * 1000L
                    
                    // Only seek if player is ready and position is valid
                    if (player != null && player.playbackState == Player.STATE_READY && seekPosition < player.duration) {
                        player.seekTo(seekPosition)

                    }
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                handler.removeCallbacks(hideControlsRunnable)
                isUserSeeking = true
            }
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                scheduleControlsHide()
            }
        })
        
        playerView.setOnClickListener {
            if (isControlsVisible) {
                hideControls()
            } else {
                showControls()
                scheduleControlsHide()
            }
        }
        
        totalTimeText.setOnClickListener {
            showRemainingTime = !showRemainingTime
            updateTimeDisplay()
        }
        
        tracksButton.setOnClickListener {
            showTracksDialog()
        }
        
        aspectRatioButton.setOnClickListener {
            toggleAspectRatio()
        }
    }
    
    private fun loadVideoFromIntent() {
        val action = intent.action
        var videoUri: Uri? = null
        var videoTitle: String = "Video"
        var videoId: Long = -1L
        var lastPosition: Long = 0L

        if (action == Intent.ACTION_VIEW && intent.data != null) {
             // Handle open from external app
             videoUri = intent.data
             // Use ContentResolver to query for display name if possible
             videoTitle = try {
                 var name: String? = null
                 if (videoUri?.scheme == "content") {
                    contentResolver.query(videoUri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                           val nameIndex = cursor.getColumnIndex("_display_name")
                           if (nameIndex != -1) {
                               name = cursor.getString(nameIndex)
                           }
                        }
                    }
                 }
                 name ?: videoUri?.lastPathSegment ?: "External Video"
             } catch (e: Exception) {
                 "External Video"
             }
             // For external videos, we generate a hash-based ID simple enough to track progress locally if needed
             // or just use -1L to treat as ephemeral
             videoId = videoUri.toString().hashCode().toLong()
             
             // Try to find if we have existing progress for this URI-based ID if desired, 
             // but 'videoRepository' uses SQL IDs. 
             // We can check if we can match this URI to a file in DB, but that's expensive.
             // We'll treat it as ephemeral or new.
        } else {
             // Internal navigation
             videoUri = intent.getStringExtra("video_uri")?.let { Uri.parse(it) }
             videoTitle = intent.getStringExtra("video_title") ?: "Video"
             videoId = intent.getLongExtra("video_id", -1L)
             lastPosition = intent.getLongExtra("last_position", 0L)
        }
        

        
        if (videoUri != null && videoId != -1L) {
            videoFile = VideoFile(
                id = videoId,
                title = videoTitle,
                displayName = videoTitle,
                duration = 0L,
                uri = videoUri.toString(),
                path = "",
                size = 0L,
                width = 0,
                height = 0,
                dateAdded = 0L,
                mimeType = "",
                watchProgress = 0f,
                isCompleted = false,
                lastWatched = 0L
            )
            
            titleText.text = removeFileExtension(videoTitle)
            videoPlayerManager.prepareVideo(videoFile!!)
            
            // For external, maybe check database for progress if we could? 
            // Currently assuming start from 0 for external unless we do a complex lookup.
            val shouldResume = lastPosition > 0 && !videoRepository.getCompletionStatus(videoId)
            val player = videoPlayerManager.getPlayer()
            
            if (shouldResume) {
                // Don't start playing immediately - seek first, then play
                player?.playWhenReady = false
                
                player?.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY && player.duration > 0) {
                            // Ensure we don't seek beyond video duration
                            val seekPosition = lastPosition.coerceAtMost(player.duration - 1000) // Leave 1 second buffer
                            if (seekPosition > 0) {
                                // Seek first, then start playing after seek completes
                                player.seekTo(seekPosition)

                                
                                // Wait for seek to complete, then start playing
                                Handler(Looper.getMainLooper()).postDelayed({
                                    player.playWhenReady = true
                                    player.play()
                                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                    player.removeListener(this)
                                }, 100) // Small delay to ensure seek is processed
                            } else {
                                // No valid position, start playing normally
                                player.playWhenReady = true
                                player.play()
                                player.removeListener(this)
                            }
                        }
                    }
                })
            } else {
                // No resume needed, start playing immediately
                player?.let { p ->
                    p.playWhenReady = true
                    p.play()
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                if (videoRepository.getCompletionStatus(videoId)) {
                }
            }
            
            // Save play time
            videoRepository.saveLastPlayedTime(videoId)
        }
    }
    
    private fun updatePlayPauseButton() {
        val player = videoPlayerManager.getPlayer()
        playPauseButton.setImageResource(
            if (player?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play
        )
    }
    
    private var isUserSeeking = false
    
    private fun startSeekBarUpdate() {
        val updateSeekBar = object : Runnable {
            override fun run() {
                val player = videoPlayerManager.getPlayer()
                if (player != null && player.isPlaying && !isUserSeeking) {
                    val currentPosition = (player.currentPosition / 1000).toInt()
                    seekBar.progress = currentPosition
                    currentTimeText.text = formatTime(player.currentPosition)
                    updateTimeDisplay()
                    
                    handler.postDelayed(this, 1000)
                } else if (player != null && !isUserSeeking) {
                    // Update time even when paused
                    currentTimeText.text = formatTime(player.currentPosition)
                    updateTimeDisplay()
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(updateSeekBar)
    }
    
    private fun startProgressTracking() {
        handler.post(progressUpdateRunnable)
    }
    
    private fun updateProgress() {
        val player = videoPlayerManager.getPlayer()
        videoFile?.let { video ->
            if (player != null && player.duration > 0) {
                val currentPosition = player.currentPosition
                val duration = player.duration
                val progress = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                
                // Save progress and position every 5 seconds
                videoRepository.saveWatchProgress(video.id, progress, duration)
                videoRepository.saveLastPlayPosition(video.id, currentPosition)
                
                // Check if video is completed (95% threshold)
                val isCompleted = progress >= 0.95f
                if (isCompleted && !video.isCompleted) {
                    // Video just completed, refresh folders in background
                    CoroutineScope(Dispatchers.IO).launch {
                        videoRepository.refreshFoldersWithProgress()
                    }
                }
                
                // Schedule next update
                handler.postDelayed(progressUpdateRunnable, 5000)
            }
        }
    }
    
    private fun showControls() {
        findViewById<View>(R.id.controlsLayout).visibility = View.VISIBLE
        isControlsVisible = true
    }
    
    private fun hideControls() {
        findViewById<View>(R.id.controlsLayout).visibility = View.GONE
        isControlsVisible = false
    }
    
    private fun scheduleControlsHide() {
        handler.removeCallbacks(hideControlsRunnable)
        handler.postDelayed(hideControlsRunnable, 3000)
    }
    
    private fun formatTime(timeMs: Long): String {
        val seconds = timeMs / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }
    
    private fun removeFileExtension(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf('.')
        return if (lastDotIndex > 0) {
            fileName.substring(0, lastDotIndex)
        } else {
            fileName
        }
    }
    
    private fun updateTimeDisplay() {
        val player = videoPlayerManager.getPlayer()
        if (player != null && player.duration > 0) {
            if (showRemainingTime) {
                val remainingTime = player.duration - player.currentPosition
                totalTimeText.text = "-${formatTime(remainingTime)}"
            } else {
                totalTimeText.text = formatTime(player.duration)
            }
        }
    }
    
    private fun showTracksDialog() {
        val player = videoPlayerManager.getPlayer() ?: return
        
        // Close existing dialog if open
        tracksDialog?.dismiss()
        
        // Note: You'll need to create R.layout.dialog_tracks
        // For now, creating a basic dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_tracks, null)
        val audioTracksList = dialogView.findViewById<ListView>(R.id.audioTracksList)
        val subtitleTracksList = dialogView.findViewById<ListView>(R.id.subtitleTracksList)
        
        // Create adapters
        val audioAdapter = TrackAdapter(this, mutableListOf())
        val subtitleAdapter = TrackAdapter(this, mutableListOf())
        
        audioTracksList.adapter = audioAdapter
        subtitleTracksList.adapter = subtitleAdapter
        
        // Function to update track lists
        fun updateTrackLists() {
            val tracks = player.currentTracks
            
            // Update audio tracks
            val audioTracks = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
            val audioTrackItems = mutableListOf<TrackItem>()
            
            val isAudioDisabled = player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_AUDIO)
            audioTrackItems.add(TrackItem("Disable track", isAudioDisabled, null) {
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    .build()
                // Add small delay to allow ExoPlayer to process the change
                handler.postDelayed({ updateTrackLists() }, 100)
            })
            
            audioTracks.forEach { group ->
                group.mediaTrackGroup.let { trackGroup ->
                    for (i in 0 until trackGroup.length) {
                        val format = trackGroup.getFormat(i)
                        val trackName = getTrackDisplayName(format, "Audio Track ${i + 1}")
                        val isSelected = group.isTrackSelected(i) && !isAudioDisabled
                        audioTrackItems.add(TrackItem(trackName, isSelected, format) {
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                .setOverrideForType(
                                    androidx.media3.common.TrackSelectionOverride(trackGroup, i)
                                )
                                .build()
                            // Add small delay to allow ExoPlayer to process the change
                            handler.postDelayed({ updateTrackLists() }, 100)
                        })
                    }
                }
            }
            
            // Update subtitle tracks
            val subtitleTracks = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
            val subtitleTrackItems = mutableListOf<TrackItem>()
            
            val isSubtitleDisabled = player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
            subtitleTrackItems.add(TrackItem("Disable track", isSubtitleDisabled, null) {
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build()
                // Add small delay to allow ExoPlayer to process the change
                handler.postDelayed({ updateTrackLists() }, 100)
            })
            
            subtitleTracks.forEach { group ->
                group.mediaTrackGroup.let { trackGroup ->
                    for (i in 0 until trackGroup.length) {
                        val format = trackGroup.getFormat(i)
                        val trackName = getTrackDisplayName(format, "Subtitle Track ${i + 1}")
                        val isSelected = group.isTrackSelected(i) && !isSubtitleDisabled
                        subtitleTrackItems.add(TrackItem(trackName, isSelected, format) {
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                .setOverrideForType(
                                    androidx.media3.common.TrackSelectionOverride(trackGroup, i)
                                )
                                .build()
                            // Add small delay to allow ExoPlayer to process the change
                            handler.postDelayed({ updateTrackLists() }, 100)
                        })
                    }
                }
            }
            
            // Update adapters
            audioAdapter.clear()
            audioAdapter.addAll(audioTrackItems)
            audioAdapter.notifyDataSetChanged()
            
            subtitleAdapter.clear()
            subtitleAdapter.addAll(subtitleTrackItems)
            subtitleAdapter.notifyDataSetChanged()
        }
        
        // Initial update
        updateTrackLists()
        
        // Add track change listener
        val trackChangeListener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                // Update dialog when tracks change
                if (tracksDialog?.isShowing == true) {
                    updateTrackLists()
                }
            }
        }
        player.addListener(trackChangeListener)
        
        // Create and show dialog
        tracksDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create().apply {
                setOnDismissListener {
                    player.removeListener(trackChangeListener)
                    tracksDialog = null
                }
                show()
                window?.setBackgroundDrawableResource(android.R.color.transparent)
            }

        // Handle in-layout close button to avoid default gray button bar
        dialogView.findViewById<TextView>(R.id.closeButton)?.setOnClickListener {
            tracksDialog?.dismiss()
        }
    }
    
    private fun toggleAspectRatio() {
        val aspectRatios = AspectRatio.values()
        val currentIndex = currentAspectRatio.ordinal
        val nextIndex = (currentIndex + 1) % aspectRatios.size
        currentAspectRatio = aspectRatios[nextIndex]
        applyAspectRatio(currentAspectRatio)
    }
    
    private fun applyAspectRatio(aspectRatio: AspectRatio) {
        when (aspectRatio) {
            AspectRatio.ORIGINAL -> {
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                // Reset scaling for original aspect ratio
                playerView.scaleX = 1.0f
                playerView.scaleY = 1.0f
            }
            AspectRatio.FIT_SCREEN -> {
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                // Reset scaling for fit screen
                playerView.scaleX = 1.0f
                playerView.scaleY = 1.0f
            }
        }
    }
    
    private fun getTrackDisplayName(format: Format, defaultName: String): String {
        val label = format.label
        val language = format.language
        val codec = format.codecs
        
        return when {
            !label.isNullOrEmpty() -> {
                val languageInfo = if (!language.isNullOrEmpty() && language != "und") {
                    " - [${language.uppercase()}]"
                } else ""
                "$label$languageInfo"
            }
            !language.isNullOrEmpty() && language != "und" -> {
                val codecInfo = if (!codec.isNullOrEmpty()) " - [$codec]" else ""
                "${language.uppercase()}$codecInfo"
            }
            else -> defaultName
        }
    }
    
    private data class TrackItem(
        val name: String,
        val isSelected: Boolean,
        val format: Format?,
        val action: () -> Unit
    )
    
    private class TrackAdapter(
        private val context: android.content.Context,
        private val items: MutableList<TrackItem>
    ) : ArrayAdapter<TrackItem>(context, R.layout.item_track, items) {
        
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_track, parent, false)
            val item = items[position]
            
            val trackItemLayout = view.findViewById<LinearLayout>(R.id.trackItemLayout)
            val trackName = view.findViewById<TextView>(R.id.trackName)
            
            // Set background based on selection state
            trackItemLayout.setBackgroundResource(
                if (item.isSelected) R.drawable.track_selected_background 
                else R.drawable.track_default_background
            )
            
            trackName.text = item.name
            trackName.setTextColor(
                if (item.isSelected) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            )
            
            view.setOnClickListener { item.action() }
            
            return view
        }
    }
    
    override fun onPause() {
        super.onPause()
        videoPlayerManager.getPlayer()?.pause()
        
        // Save progress when pausing
        val player = videoPlayerManager.getPlayer()
        videoFile?.let { video ->
            if (player != null && player.duration > 0) {
                val currentPosition = player.currentPosition
                val duration = player.duration
                val progress = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                videoRepository.saveWatchProgress(video.id, progress, duration)
                videoRepository.saveLastPlayPosition(video.id, currentPosition)
                
                // Refresh folders with updated progress in background
                CoroutineScope(Dispatchers.IO).launch {
                    videoRepository.refreshFoldersWithProgress()
                }
            }
        }
        // Clear keep screen on when activity is paused
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val touchZoneWidth = screenWidth / 3 // Left and right thirds for gestures
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                gestureStartX = event.x
                gestureStartY = event.y
                isGestureActive = false
                gestureType = GestureType.NONE
                
                // Store starting values
                gestureStartBrightness = currentBrightness
                gestureStartVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            }
            MotionEvent.ACTION_MOVE -> {
                val cumulativeDeltaY = gestureStartY - event.y
                val deltaX = kotlin.math.abs(event.x - gestureStartX)
                
                // Only activate gesture if vertical movement is significant and more than horizontal
                if (kotlin.math.abs(cumulativeDeltaY) > 30 && kotlin.math.abs(cumulativeDeltaY) > deltaX) {
                    if (!isGestureActive) {
                        isGestureActive = true
                        // Determine gesture type based on starting position
                        val isLeftSide = gestureStartX < touchZoneWidth
                        val isRightSide = gestureStartX > screenWidth - touchZoneWidth
                        
                        gestureType = when {
                            isLeftSide -> GestureType.BRIGHTNESS
                            isRightSide -> GestureType.VOLUME
                            else -> GestureType.NONE
                        }
                    }
                    
                    // Use cumulative delta from start position
                    when (gestureType) {
                        GestureType.BRIGHTNESS -> adjustBrightness(cumulativeDeltaY, screenHeight)
                        GestureType.VOLUME -> adjustVolume(cumulativeDeltaY, screenHeight)
                        GestureType.NONE -> {}
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isGestureActive) {
                    isGestureActive = false
                    gestureType = GestureType.NONE
                    return true
                }
            }
        }
        
        return if (isGestureActive) true else super.dispatchTouchEvent(event)
    }
    
    private fun adjustBrightness(cumulativeDelta: Float, screenHeight: Int) {
        // Full screen swipe = 100% change
        val percentChange = cumulativeDelta / screenHeight
        
        currentBrightness = (gestureStartBrightness + percentChange).coerceIn(0.01f, 1f)
        
        val layoutParams = window.attributes
        layoutParams.screenBrightness = currentBrightness
        window.attributes = layoutParams
    }
    
    private fun adjustVolume(cumulativeDelta: Float, screenHeight: Int) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        
        // Full screen swipe = 100% volume change
        val percentChange = cumulativeDelta / screenHeight
        val volumeChange = (percentChange * maxVolume).toInt()
        
        val newVolume = (gestureStartVolume + volumeChange).coerceIn(0, maxVolume)
        
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
    }
    

    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        // Ensure flag is cleared on destroy
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Save final progress before destroying and refresh folders
        val player = videoPlayerManager.getPlayer()
        videoFile?.let { video ->
            if (player != null && player.duration > 0) {
                val currentPosition = player.currentPosition
                val duration = player.duration
                val progress = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                videoRepository.saveWatchProgress(video.id, progress, duration)
                videoRepository.saveLastPlayPosition(video.id, currentPosition)
                
                // Refresh folders with updated progress in background
                CoroutineScope(Dispatchers.IO).launch {
                    videoRepository.refreshFoldersWithProgress()
                }
            }
        }
        
        videoPlayerManager.release()
    }
}
