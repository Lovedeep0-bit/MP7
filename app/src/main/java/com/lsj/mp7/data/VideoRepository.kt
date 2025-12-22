package com.lsj.mp7.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.lsj.mp7.utils.MediaScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * VideoRepository - matches the working MP4 app implementation
 */
class VideoRepository(private val context: Context) {
    private val mediaScanner = MediaScanner(context)
    private val sharedPrefs = context.getSharedPreferences("video_prefs", Context.MODE_PRIVATE)
    
    private val _videos = MutableLiveData<List<VideoFile>>()
    val videos: LiveData<List<VideoFile>> = _videos
    
    private val _folders = MutableLiveData<List<FolderItem>>()
    val folders: LiveData<List<FolderItem>> = _folders
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _scanProgress = MutableLiveData<String>()
    val scanProgress: LiveData<String> = _scanProgress
    
    suspend fun refreshVideoLibrary() {
        Log.d("VideoRepository", "=== refreshVideoLibrary START ===")
        _isLoading.postValue(true)
        _scanProgress.postValue("Scanning video files...")
        
        try {
            Log.d("VideoRepository", "Calling mediaScanner.scanVideos()...")
            val scannedVideos = mediaScanner.scanVideos()
            Log.d("VideoRepository", "Scanned ${scannedVideos.size} videos")
            
            if (scannedVideos.isEmpty()) {
                Log.w("VideoRepository", "WARNING: No videos found in Movies folder!")
                _scanProgress.postValue("No videos found in Movies folder")
                _videos.postValue(emptyList())
                _folders.postValue(emptyList())
                return
            }
            
            val enhancedVideos = scannedVideos.map { video ->
                val progress = getWatchProgress(video.id)
                val isCompleted = getCompletionStatus(video.id)
                val lastWatched = getLastWatchedTime(video.id)
                
                video.copy(
                    lastPlayPosition = getLastPlayPosition(video.id),
                    watchProgress = progress,
                    isCompleted = isCompleted,
                    lastWatched = lastWatched,
                    isWatched = lastWatched > 0
                )
            }
            
            Log.d("VideoRepository", "Posting ${enhancedVideos.size} videos to LiveData")
            _videos.postValue(enhancedVideos)
            _scanProgress.postValue("Organizing folders...")
            
            val foldersData = mediaScanner.scanFolders(enhancedVideos)
            Log.d("VideoRepository", "Organized into ${foldersData.size} folders")
            Log.d("VideoRepository", "Posting ${foldersData.size} folders to LiveData")
            _folders.postValue(foldersData)
            
            _scanProgress.postValue("Scan complete - ${enhancedVideos.size} videos found")
            Log.d("VideoRepository", "=== refreshVideoLibrary COMPLETE ===")
            
        } catch (e: Exception) {
            Log.e("VideoRepository", "ERROR scanning videos", e)
            e.printStackTrace()
            _scanProgress.postValue("Error: ${e.message}")
            _videos.postValue(emptyList())
            _folders.postValue(emptyList())
        } finally {
            _isLoading.postValue(false)
            Log.d("VideoRepository", "Loading completed, setting isLoading to false")
        }
    }
    
    fun saveLastPlayPosition(videoId: Long, position: Long) {
        sharedPrefs.edit().putLong("position_$videoId", position).apply()
    }
    
    fun getLastPlayPosition(videoId: Long): Long {
        return sharedPrefs.getLong("position_$videoId", 0L)
    }
    
    fun saveWatchProgress(videoId: Long, progress: Float, duration: Long) {
        val isCompleted = progress >= 0.95f
        sharedPrefs.edit().apply {
            putFloat("progress_$videoId", progress)
            putBoolean("completed_$videoId", isCompleted)
            putLong("last_watched_$videoId", System.currentTimeMillis())
            if (isCompleted) {
                putLong("completed_at_$videoId", System.currentTimeMillis())
            }
            apply()
        }
        Log.d("VideoRepository", "Saved progress for video $videoId: ${(progress * 100).toInt()}%")
    }
    
    fun getWatchProgress(videoId: Long): Float {
        return sharedPrefs.getFloat("progress_$videoId", 0f)
    }
    
    fun getCompletionStatus(videoId: Long): Boolean {
        return sharedPrefs.getBoolean("completed_$videoId", false)
    }
    
    fun getLastWatchedTime(videoId: Long): Long {
        return sharedPrefs.getLong("last_watched_$videoId", 0L)
    }
    
    fun saveLastPlayedTime(videoId: Long) {
        sharedPrefs.edit().putLong("position_${videoId}_last_played", System.currentTimeMillis()).apply()
    }
    
    suspend fun updateVideosAndFolders(updatedVideos: List<VideoFile>) {
        _videos.postValue(updatedVideos)
        val foldersData = mediaScanner.scanFolders(updatedVideos)
        _folders.postValue(foldersData)
    }
    
    /**
     * Refresh folders with updated progress data after a video is watched
     */
    suspend fun refreshFoldersWithProgress() {
        val currentVideos = _videos.value ?: return
        val enhancedVideos = currentVideos.map { video ->
            video.copy(
                lastPlayPosition = getLastPlayPosition(video.id),
                watchProgress = getWatchProgress(video.id),
                isCompleted = getCompletionStatus(video.id),
                lastWatched = getLastWatchedTime(video.id),
                isWatched = getLastWatchedTime(video.id) > 0
            )
        }
        _videos.postValue(enhancedVideos)
        val foldersData = mediaScanner.scanFolders(enhancedVideos)
        _folders.postValue(foldersData)
        Log.d("VideoRepository", "Refreshed folders with updated progress data")
    }
}
