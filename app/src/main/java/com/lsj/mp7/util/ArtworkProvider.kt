package com.lsj.mp7.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap

object ArtworkProvider {
    private val memoryCache = LruCache<String, Bitmap>(100)
    private val io: CoroutineDispatcher = Dispatchers.IO
    private val pendingRequests = ConcurrentHashMap<String, Deferred<Bitmap?>>()

    suspend fun loadAudioArtwork(context: Context, uri: Uri): Bitmap? = coroutineScope {
        val key = uri.toString()
        
        // 1. Check memory cache first
        memoryCache.get(key)?.let { return@coroutineScope it }
        
        // 2. Join existing request if one is in flight
        val deferred = pendingRequests.getOrPut(key) {
            async(io) {
                try {
                    val mmr = MediaMetadataRetriever()
                    mmr.setDataSource(context, uri)
                    val art = mmr.embeddedPicture
                    mmr.release()
                    if (art != null) {
                        // Downsample to avoid OOM for high-res covers
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeByteArray(art, 0, art.size, options)
                        
                        // Target approx 512x512 for quality/memory balance
                        options.inSampleSize = calculateInSampleSize(options, 512, 512)
                        options.inJustDecodeBounds = false
                        
                        val bmp = BitmapFactory.decodeByteArray(art, 0, art.size, options)
                        if (bmp != null) memoryCache.put(key, bmp)
                        bmp
                    } else null
                } catch (_: Exception) {
                    null
                } finally {
                    pendingRequests.remove(key)
                }
            }
        }
        
        deferred.await()
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}


