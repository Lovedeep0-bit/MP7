package com.lsj.mp7.util

import android.content.Context
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.util.DebugLogger

object ImageLoaderProvider {
    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        val existing = instance
        if (existing != null) return existing
        return synchronized(this) {
            instance ?: ImageLoader.Builder(context.applicationContext)
                .components {
                    // Add VideoFrameDecoder for coil-video support
                    add(VideoFrameDecoder.Factory())
                    // Add custom VideoThumbnailFetcher for MediaStore thumbnails
                    add(VideoThumbnailFetcherFactory(context.applicationContext))
                }
                .crossfade(true)
                .respectCacheHeaders(false)
                .logger(DebugLogger())
                .build()
                .also { instance = it }
        }
    }
}


