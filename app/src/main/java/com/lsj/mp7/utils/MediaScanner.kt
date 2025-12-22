package com.lsj.mp7.utils

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.lsj.mp7.data.FolderItem
import com.lsj.mp7.data.VideoFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MediaScanner - scans videos from Movies folder and organizes them into folders
 * Matches the working MP4 app implementation
 */
class MediaScanner(private val context: Context) {
    
    private fun targetVolumes(): List<String> {
        val volumes = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 29) {
            volumes.addAll(MediaStore.getExternalVolumeNames(context))
        } else {
            volumes.add(MediaStore.VOLUME_EXTERNAL)
        }
        volumes.add(MediaStore.VOLUME_INTERNAL)
        return volumes.distinct()
    }
    
    /**
     * Scan all videos from Movies folder
     */
    suspend fun scanVideos(): List<VideoFile> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val volumes = targetVolumes()
        
        // Get allowed directories
        val allowedDirs = ScannedDirectoriesState.getAllowedVideoDirectories(context)
        Log.d("MediaScanner", "Allowed directories: $allowedDirs")
        
        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.RELATIVE_PATH,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.DATA
            )
        } else {
            arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.MIME_TYPE,
            )
        }
        
        // Scan ALL videos, then filter by path
        val selection = "${MediaStore.Video.Media.MIME_TYPE} LIKE ?"
        val selectionArgs = arrayOf("video/%")
        val sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC"
        val list = mutableListOf<VideoFile>()
        
        Log.d("MediaScanner", "=== Starting video scan ===")
        Log.d("MediaScanner", "Volumes: $volumes")
        
        for (vol in volumes) {
            try {
                val uri = MediaStore.Video.Media.getContentUri(vol)
                resolver.query(uri, projection, selection, selectionArgs, sortOrder).use { cursor ->
                    if (cursor != null) {
                        val count = cursor.count
                        
                        if (count > 0) {
                            val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                            val titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                            val displayIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                            val durationIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                            val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                            val widthIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                            val heightIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                            val dateIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                            val mimeIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                            val dataIdx = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                            
                            while (cursor.moveToNext()) {
                                val id = cursor.getLong(idIdx)
                                val contentUri = ContentUris.withAppendedId(uri, id)
                                
                                // Always use absolute path for filtering if available
                                val path = if (dataIdx >= 0) cursor.getString(dataIdx) else ""
                                
                                if (path.isBlank()) continue
                                
                                // Check if path is inside any allowed directory
                                val isAllowed = allowedDirs.any { allowedDir -> 
                                    path.startsWith(allowedDir, ignoreCase = true)
                                }
                                
                                if (!isAllowed) {
                                    // Log.d("MediaScanner", "Skipping video - path not allowed: $path")
                                    continue
                                }
                                
                                val rawTitle = cursor.getString(titleIdx)
                                val displayName = cursor.getString(displayIdx)
                                val cleanTitle = (displayName ?: rawTitle ?: "Untitled").substringBeforeLast('.')
                                
                                // Extract parent folder name BEFORE adding to list
                                val parentFolder = extractParentFolder(path, allowedDirs)
                                
                                list.add(
                                    VideoFile(
                                        id = id,
                                        title = cleanTitle,
                                        displayName = displayName ?: cleanTitle,
                                        duration = cursor.getLong(durationIdx),
                                        uri = contentUri.toString(),
                                        path = path,
                                        size = cursor.getLong(sizeIdx),
                                        width = cursor.getInt(widthIdx),
                                        height = cursor.getInt(heightIdx),
                                        dateAdded = cursor.getLong(dateIdx),
                                        mimeType = cursor.getString(mimeIdx),
                                        parentFolder = parentFolder
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MediaScanner", "Error scanning videos from volume $vol", e)
            }
        }
        
        Log.d("MediaScanner", "=== Scan complete ===")
        Log.d("MediaScanner", "Total videos scanned: ${list.size}")
        list
    }
    
    /**
     * Extract parent folder name from path based on allowed directories
     */
    private fun extractParentFolder(path: String?, allowedDirs: Set<String>): String {
        if (path.isNullOrEmpty()) return "Unknown"
        
        // Find the allowed directory that this path belongs to
        val matchedDir = allowedDirs.filter { 
            path.startsWith(it, ignoreCase = true) 
        }.maxByOrNull { it.length }
        
        if (matchedDir == null) return "Unknown"
        
        try {
            // content:// style paths might need different handling, but we filter by absolute path usually
            // remove the base path
            val relativePath = path.substring(matchedDir.length).replace('\\', '/').trim('/')
            
            val parts = relativePath.split('/').filter { it.isNotEmpty() }
            
            // If parts has more than 1 item (filename), it's in a subfolder
            // e.g. "Sub/video.mp4" -> "Sub"
            // e.g. "video.mp4" -> Use the name of the allowed dir itself
            
            if (parts.size > 1) {
                return parts[0]
            }
            
            // It's in the root of the allowed dir
            // Return the name of the allowed directory as the folder name
            return matchedDir.replace('\\', '/').split('/').lastOrNull { it.isNotEmpty() } ?: "Root"
            
        } catch (e: Exception) {
            return "Unknown"
        }
    }
    
    /**
     * Organize videos into folders
     */
    fun scanFolders(videos: List<VideoFile>): List<FolderItem> {
        if (videos.isEmpty()) {
            Log.d("MediaScanner", "No videos to organize into folders")
            return emptyList()
        }
        
        // Group by parent folder
        val grouped = videos.groupBy { it.parentFolder }
        
        val folders = grouped.map { (folderName, videoList) ->
            FolderItem(
                id = kotlin.math.abs(folderName.hashCode()).toLong(),
                name = folderName,
                path = videoList.firstOrNull()?.path ?: folderName,
                videoCount = videoList.size,
                videos = videoList
            )
        }.sortedBy { it.name.lowercase() }
        
        Log.d("MediaScanner", "Organized into ${folders.size} folders")
        folders.forEach { folder ->
            Log.d("MediaScanner", "Folder: ${folder.name}, Videos: ${folder.videoCount}")
        }
        
        return folders
    }
}

