package com.lsj.mp7.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.os.Build
import kotlin.math.abs

class MediaRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    data class AudioResult(
        val all: List<AudioFile>,
        val folders: List<AudioFolder>
    )

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
    suspend fun getFullAudioData(): AudioResult = withContext(ioDispatcher) {
        val resolver = context.contentResolver
        val volumes = targetVolumes()
        val allowedDirs = com.lsj.mp7.utils.ScannedDirectoriesState.getAllowedAudioDirectories(context).toHashSet()
        
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
        )
        val selection = "${MediaStore.Audio.Media.MIME_TYPE} LIKE ?"
        val selectionArgs = arrayOf("audio/%")
        val sortOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC"
        val list = ArrayList<AudioFile>()
        
        for (vol in volumes) {
            try {
                val uri = MediaStore.Audio.Media.getContentUri(vol)
                resolver.query(uri, projection, selection, selectionArgs, sortOrder).use { cursor ->
                    if (cursor != null) {
                        val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                        val titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                        val displayIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                        val artistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                        val albumIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                        val durationIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                        val dataIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                        
                        while (cursor.moveToNext()) {
                            val path = cursor.getString(dataIdx) ?: ""
                            
                            // Highly optimized prefix check using HashSet of allowed roots
                            if (path.isBlank()) continue
                            var allowed = false
                            for (dir in allowedDirs) {
                                if (path.startsWith(dir, ignoreCase = true)) {
                                    allowed = true
                                    break
                                }
                            }
                            if (!allowed) continue
                            
                            val id = cursor.getLong(idIdx)
                            val contentUri = ContentUris.withAppendedId(uri, id)
                            val rawTitle = cursor.getString(titleIdx)
                            val displayName = cursor.getString(displayIdx)
                            val cleanTitle = (displayName ?: rawTitle ?: "Untitled").substringBeforeLast('.')
                            
                            list.add(
                                AudioFile(
                                    id = id,
                                    title = cleanTitle,
                                    artist = cursor.getString(artistIdx),
                                    album = cursor.getString(albumIdx),
                                    duration = cursor.getLong(durationIdx),
                                    uri = contentUri.toString(),
                                    path = path,
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Compute folders from the single scan list
        val groupedByFolder = list.groupBy { audio ->
            val p = audio.path ?: "Other"
            val parts = p.replace('\\', '/').split('/')
            if (parts.size >= 2) parts[parts.size - 2] else "Other"
        }
        
        val folderList = groupedByFolder.map { (name, files) ->
            AudioFolder(
                id = abs(name.hashCode()).toLong(),
                name = name,
                path = files.firstOrNull()?.path ?: "",
                audioCount = files.size,
                totalDuration = files.sumOf { it.duration }
            )
        }.sortedBy { it.name.lowercase() }
        
        AudioResult(list, folderList)
    }

    suspend fun scanAudio(): List<AudioFile> = getFullAudioData().all

    private fun isInFolder(name: String, bucket: String?, rel: String?): Boolean {
        val n = name.lowercase()
        val b = bucket?.lowercase() ?: ""
        val r = rel?.lowercase() ?: ""
        return b == name.lowercase() || r.contains("/$n/") || r.endsWith("$n/") || r.startsWith("$n/")
    }

    // Video-related APIs removed for MP3-only build

    suspend fun audiosInMusicFolder(): List<AudioFile> = withContext(ioDispatcher) {
        // Now redirects to the main scan function which respects ScannedDirectoriesState
        scanAudio()
    }

    suspend fun audioFolders(): List<AudioFolder> = getFullAudioData().folders

    private fun extractImmediateChild(relativePath: String?, rootName: String): String? {
        if (relativePath.isNullOrBlank()) return null
        val rp = relativePath.trim()
        val lower = rp.lowercase()
        val rootLower = rootName.lowercase() + "/"
        val idx = lower.indexOf(rootLower)
        if (idx < 0) return null
        val after = rp.substring(idx + rootLower.length)
        val slash = after.indexOf('/')
        if (slash <= 0) return null
        return after.substring(0, slash)
    }

    // Subfolder grouping not needed for MP3-only UI; keep a simple folder list

    // Video scanning functions - scan all videos
    suspend fun scanVideos(): List<VideoFile> = withContext(ioDispatcher) {
        scanVideosInternal(null)
    }
    
    // Scan videos specifically from Movies folder
    suspend fun scanVideosInMoviesFolder(): List<VideoFile> = withContext(ioDispatcher) {
        scanVideosInternal("Movies")
    }
    
    private suspend fun scanVideosInternal(targetFolder: String?): List<VideoFile> = withContext(ioDispatcher) {
        val resolver = context.contentResolver
        val volumes = targetVolumes()
        
        // Use RELATIVE_PATH for Android 10+ compatibility (instead of DATA which is deprecated)
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
        
        // Build selection query
        val selectionBuilder = StringBuilder("${MediaStore.Video.Media.MIME_TYPE} LIKE ?")
        val selectionArgsList = mutableListOf<String>("video/%")
        
        // Filter by Movies folder if specified
        if (targetFolder != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+, use RELATIVE_PATH
                selectionBuilder.append(" AND ${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?")
                selectionArgsList.add("$targetFolder/%")
            } else {
                // For older versions, use DATA
                selectionBuilder.append(" AND ${MediaStore.Video.Media.DATA} LIKE ?")
                selectionArgsList.add("%$targetFolder%")
            }
        }
        
        val selection = selectionBuilder.toString()
        val selectionArgs = selectionArgsList.toTypedArray()
        val sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC"
        val list = mutableListOf<VideoFile>()
        
        android.util.Log.d("MediaRepository", "Scanning videos - targetFolder: $targetFolder, volumes: $volumes")
        
        for (vol in volumes) {
            try {
                val uri = MediaStore.Video.Media.getContentUri(vol)
                android.util.Log.d("MediaRepository", "Querying volume: $vol with selection: $selection, args: ${selectionArgs.joinToString()}")
                resolver.query(uri, projection, selection, selectionArgs, sortOrder).use { cursor ->
                    if (cursor != null) {
                        val count = cursor.count
                        android.util.Log.d("MediaRepository", "Found $count videos in volume $vol")
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
                            
                            val pathIdx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                            } else {
                                cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                            }
                            
                            while (cursor.moveToNext()) {
                                val id = cursor.getLong(idIdx)
                                val contentUri = ContentUris.withAppendedId(uri, id)
                                val path = if (pathIdx >= 0) cursor.getString(pathIdx) ?: "" else ""
                                
                                // Additional filter for path-based checking (in case query filter isn't perfect)
                                if (targetFolder != null) {
                                    val pathLower = path.lowercase()
                                    val targetLower = targetFolder.lowercase()
                                    var shouldInclude = false
                                    
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        // RELATIVE_PATH format can be:
                                        // - "Movies/FolderName/" (videos in subfolder)
                                        // - "Movies/" (videos directly in Movies root)
                                        // - "DCIM/Movies/" (some devices)
                                        // - "Download/Movies/" (some devices)
                                        shouldInclude = pathLower.startsWith("$targetLower/") || 
                                                       pathLower.contains("/$targetLower/") ||
                                                       pathLower == "$targetLower/"
                                    } else {
                                        // DATA format: full path with "Movies" somewhere
                                        // Examples: "/storage/emulated/0/Movies/..." or ".../Movies/..."
                                        shouldInclude = pathLower.contains("/$targetLower/") || 
                                                       pathLower.contains("\\$targetLower\\") ||
                                                       pathLower.endsWith("/$targetLower") ||
                                                       pathLower.endsWith("\\$targetLower")
                                    }
                                    
                                    if (!shouldInclude) {
                                        android.util.Log.d("MediaRepository", "Skipping video: path doesn't match Movies folder - $path")
                                        continue
                                    }
                                }
                                
                                val rawTitle = cursor.getString(titleIdx)
                                val displayName = cursor.getString(displayIdx)
                                val cleanTitle = (displayName ?: rawTitle ?: "Untitled").substringBeforeLast('.')
                                
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
                                        mimeType = cursor.getString(mimeIdx)
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Log error but continue with other volumes
                android.util.Log.e("MediaRepository", "Error scanning videos from volume $vol", e)
                e.printStackTrace()
            }
        }
        android.util.Log.d("MediaRepository", "Total videos scanned: ${list.size}")
        list
    }

    suspend fun videoFolders(): List<VideoFolder> = withContext(ioDispatcher) {
        val items = scanVideos()
        if (items.isEmpty()) return@withContext emptyList()
        
        // Group by parent folder name derived from path
        val grouped = items.groupBy { video ->
            val p = video.path ?: ""
            if (p.isEmpty()) {
                "Other"
            } else {
                val parts = p.replace('\\', '/').split('/').filter { it.isNotEmpty() }
                // Get the last folder name from the path
                if (parts.isNotEmpty()) {
                    // For Android 10+, RELATIVE_PATH format is like "Movies/FolderName/"
                    // We want the folder name (second to last part)
                    if (parts.size >= 2) {
                        parts[parts.size - 2]
                    } else {
                        parts.lastOrNull() ?: "Other"
                    }
                } else {
                    "Other"
                }
            }
        }
        
        grouped.map { (name, list) ->
            val totalDuration = list.sumOf { it.duration }
            VideoFolder(
                id = abs(name.hashCode()).toLong(),
                name = name,
                path = list.firstOrNull()?.path ?: name,
                videoCount = list.size,
                totalDuration = totalDuration
            )
        }.sortedBy { it.name.lowercase() }
    }
    
    // Get folders and videos from Movies folder
    suspend fun moviesFolderContents(): List<FolderItem> = withContext(ioDispatcher) {
        val items = scanVideosInMoviesFolder()
        if (items.isEmpty()) return@withContext emptyList()
        
        // Group videos: those in subfolders vs those directly in Movies root
        val grouped = items.groupBy { video ->
            val p = video.path ?: ""
            if (p.isEmpty()) {
                "Movies" // Root videos
            } else {
                val parts = p.replace('\\', '/').split('/').filter { it.isNotEmpty() }
                if (parts.isNotEmpty()) {
                    // For Android 10+, RELATIVE_PATH format is like "Movies/FolderName/"
                    // If has 2+ parts, it's in a subfolder; otherwise it's in root
                    if (parts.size >= 2 && parts[0].equals("Movies", ignoreCase = true)) {
                        // In subfolder: "Movies/FolderName/" -> return "FolderName"
                        parts[1]
                    } else {
                        // Directly in Movies root
                        "Movies"
                    }
                } else {
                    "Movies"
                }
            }
        }
        
        grouped.map { (name, list) ->
            FolderItem(
                id = abs(name.hashCode()).toLong(),
                name = name,
                path = list.firstOrNull()?.path ?: name,
                videoCount = list.size,
                videos = list
            )
        }.sortedBy { it.name.lowercase() }
    }

    suspend fun videosInFolder(folderName: String): List<VideoFile> = withContext(ioDispatcher) {
        val allVideos = scanVideos()
        allVideos.filter { video ->
            val p = video.path ?: return@filter false
            val parts = p.replace('\\', '/').split('/')
            parts.getOrNull(parts.size - 2) == folderName
        }
    }
}


