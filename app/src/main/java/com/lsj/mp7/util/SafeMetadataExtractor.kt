package com.lsj.mp7.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.lsj.mp7.data.AudioTrack
import com.lsj.mp7.data.SubtitleTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Safe metadata extractor for audio files (MP3 only)
 */
class SafeMetadataExtractor(private val context: Context) {
    companion object {
        private const val MAX_CONCURRENT_OPERATIONS = 1 // Only one at a time
        private const val OPERATION_TIMEOUT_MS = 5000L // 5 second timeout
    }
    private val metadataSemaphore = Semaphore(MAX_CONCURRENT_OPERATIONS)

    suspend fun extractMetadata(uri: String): Unit? {
        return withContext(Dispatchers.IO) {
            try {
                if (!metadataSemaphore.tryAcquire()) {
                    return@withContext null
                }
                try {
                    withTimeout(OPERATION_TIMEOUT_MS) {
                        extractMetadataInternal(uri)
                    }
                } finally {
                    metadataSemaphore.release()
                }
            } catch (e: Exception) {
                SimpleErrorHandler.logError("extractMetadata", e)
                null
            }
        }
    }

    private suspend fun extractMetadataInternal(uri: String): Unit? {
        return try {
            val mediaRetriever = MediaMetadataRetriever()
            try {
                mediaRetriever.setDataSource(context, Uri.parse(uri))
                // Only extract duration for MP3
                val durationStr = mediaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val duration = durationStr?.toLongOrNull() ?: 0L
                // You can add more audio-only metadata extraction here if needed
                // For now, just return null or a simple data class if you want
                null
            } finally {
                mediaRetriever.release()
            }
        } catch (e: Exception) {
            SimpleErrorHandler.logError("extractMetadataInternal", e)
            null
        }
    }
}
