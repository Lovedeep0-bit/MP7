package com.lsj.mp7.player

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import com.lsj.mp7.data.VideoFile

/**
 * VideoPlayerManager - Manages ExoPlayer lifecycle for video playback
 * Matches the interface from your MP4 app
 */
class VideoPlayerManager(private val context: Context) {
    private var player: ExoPlayer? = null
    
    /**
     * Initialize and return the ExoPlayer instance
     */
    fun initializePlayer(): ExoPlayer {
        if (player == null) {
            player = ExoPlayer.Builder(context).build()
        }
        return player!!
    }
    
    /**
     * Get the current player instance
     */
    fun getPlayer(): ExoPlayer? {
        return player
    }
    
    /**
     * Prepare video for playback
     */
    fun prepareVideo(videoFile: VideoFile) {
        val exoPlayer = initializePlayer()

        val builder = MediaItem.Builder().setUri(Uri.parse(videoFile.uri))

        // Attach sidecar subtitles if found
        val subs = findSidecarSubtitles(videoFile)
        if (subs.isNotEmpty()) {
            builder.setSubtitleConfigurations(subs)
        }

        exoPlayer.setMediaItem(builder.build())
        exoPlayer.prepare()
    }

    private fun findSidecarSubtitles(videoFile: VideoFile): List<MediaItem.SubtitleConfiguration> {
        val resolver = context.contentResolver

        // Derive folder and base name from path/displayName
        val display = videoFile.displayName
        val base = display.substringBeforeLast('.')

        val subs = mutableListOf<MediaItem.SubtitleConfiguration>()

        // Build query depending on API level
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Files.FileColumns.RELATIVE_PATH else MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Files.getContentUri("external")

        val selectionBuilder = StringBuilder("${MediaStore.Files.FileColumns.MIME_TYPE} IN (?,?,?)")
        val args = mutableListOf(
            MimeTypes.APPLICATION_SUBRIP,
            MimeTypes.TEXT_VTT,
            "text/x-ssa"
        )

        // Match same basename
        selectionBuilder.append(" AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?")
        args.add("$base%")

        val videoDir = (videoFile.path ?: "").replace('\\','/').let { p ->
            if (p.isBlank()) "" else p.substringBeforeLast('/') + "/"
        }
        val videoDirName = if (videoDir.isNotBlank()) videoDir.trimEnd('/').substringAfterLast('/') else null

        resolver.query(uri, projection, selectionBuilder.toString(), args.toTypedArray(), null)?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameIdx = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeIdx = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val pathIdx = c.getColumnIndexOrThrow(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Files.FileColumns.RELATIVE_PATH else MediaStore.Files.FileColumns.DATA
            )
            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                val mime = c.getString(mimeIdx) ?: ""
                val pathStr = c.getString(pathIdx) ?: ""

                // Post-filter to same directory when possible
                if (!videoDirName.isNullOrBlank()) {
                    val normalized = pathStr.replace('\\','/')
                    if (!normalized.contains("/$videoDirName/")) {
                        continue
                    }
                }
                val subUri = Uri.withAppendedPath(uri, id.toString())
                val lang = nameIdx.let { idx -> c.getString(idx) }?.let { filename ->
                    // crude language hint from filename e.g., movie.en.srt -> en
                    val parts = filename.split('.')
                    if (parts.size >= 3) parts[parts.size-2] else null
                }

                val cfgBuilder = MediaItem.SubtitleConfiguration.Builder(subUri)
                    .setMimeType(mime)
                    .setSelectionFlags(0)
                if (!lang.isNullOrBlank()) cfgBuilder.setLanguage(lang)
                subs.add(cfgBuilder.build())
            }
        }

        return subs
    }
    
    /**
     * Release the player and cleanup resources
     */
    fun release() {
        player?.release()
        player = null
    }
}

