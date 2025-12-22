package com.lsj.mp7.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import java.io.ByteArrayOutputStream

/**
 * Custom Fetcher for video thumbnails using MediaStore.Thumbnails API
 */
class VideoThumbnailFetcher(
    private val context: Context,
    private val data: Uri
) : Fetcher {
    
    override suspend fun fetch(): SourceResult? {
        return withContext(Dispatchers.IO) {
            try {
                // Try to get thumbnail from MediaStore
                val thumbnail = getVideoThumbnail(data)
                if (thumbnail != null) {
                    // Convert bitmap to byte array
                    val bytes = bitmapToByteArray(thumbnail)
                    // Create BufferedSource from byte array
                    val bufferedSource = bytes.inputStream().source().buffer()
                    // Create ImageSource from BufferedSource
                    val imageSource = ImageSource(bufferedSource, context)
                    SourceResult(
                        source = imageSource,
                        mimeType = "image/jpeg",
                        dataSource = DataSource.MEMORY
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoThumbnailFetcher", "Failed to fetch thumbnail for $data", e)
                null
            }
        }
    }
    
    private fun getVideoThumbnail(uri: Uri): Bitmap? {
        return try {
            // Extract video ID from content URI
            val videoId = ContentUris.parseId(uri)
            
            // Get thumbnail from MediaStore (will create if doesn't exist)
            val thumbnail = MediaStore.Video.Thumbnails.getThumbnail(
                context.contentResolver,
                videoId,
                MediaStore.Video.Thumbnails.MINI_KIND,
                null
            )
            
            if (thumbnail != null) {
                android.util.Log.d("VideoThumbnailFetcher", "Got thumbnail from MediaStore for video ID: $videoId")
            } else {
                android.util.Log.w("VideoThumbnailFetcher", "No thumbnail available for video ID: $videoId")
            }
            
            thumbnail
        } catch (e: Exception) {
            android.util.Log.e("VideoThumbnailFetcher", "Error getting thumbnail for $uri", e)
            null
        }
    }
    
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return stream.toByteArray()
    }
}

/**
 * Factory for VideoThumbnailFetcher
 */
class VideoThumbnailFetcherFactory(private val context: Context) : Fetcher.Factory<Uri> {
    override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
        // Handle content://media URIs (MediaStore video URIs)
        if (data.scheme == "content") {
            val authority = data.authority
            if (authority == "media" || authority?.contains("media") == true) {
                return VideoThumbnailFetcher(context, data)
            }
        }
        return null
    }
}

