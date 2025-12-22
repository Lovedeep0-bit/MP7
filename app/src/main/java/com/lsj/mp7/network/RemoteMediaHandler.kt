package com.lsj.mp7.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

// 14. Network and Remote Media Support
class RemoteMediaHandler(private val context: Context) {
    private val httpClient = createHttpClient()
    
    private fun createHttpClient(): okhttp3.OkHttpClient {
        return okhttp3.OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .cache(okhttp3.Cache(File(context.cacheDir, "http_cache"), 50L * 1024L * 1024L)) // 50MB cache
            .build()
    }
    
    suspend fun isRemoteMediaAccessible(url: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val request = okhttp3.Request.Builder()
                .url(url)
                .head() // Only get headers, not the full content
                .build()
            
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.w("RemoteMediaHandler", "Remote media accessibility check failed for $url", e)
            false
        }
    }
    
    suspend fun getRemoteMediaInfo(url: String): RemoteMediaInfo? = withContext(Dispatchers.IO) {
        return@withContext try {
            val request = okhttp3.Request.Builder()
                .url(url)
                .head()
                .build()
            
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val contentLength = response.header("Content-Length")?.toLongOrNull() ?: 0L
                val contentType = response.header("Content-Type") ?: "unknown"
                val acceptRanges = response.header("Accept-Ranges") == "bytes"
                
                RemoteMediaInfo(
                    url = url,
                    contentLength = contentLength,
                    contentType = contentType,
                    supportsRangeRequests = acceptRanges,
                    isAccessible = true
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("RemoteMediaHandler", "Failed to get remote media info for $url", e)
            null
        }
    }
    
    suspend fun downloadThumbnail(url: String): ByteArray? = withContext(Dispatchers.IO) {
        return@withContext try {
            val request = okhttp3.Request.Builder()
                .url(url)
                .build()
            
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.bytes()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("RemoteMediaHandler", "Failed to download thumbnail from $url", e)
            null
        }
    }
    
    suspend fun getStreamingInfo(url: String): StreamingInfo? = withContext(Dispatchers.IO) {
        return@withContext try {
            val request = okhttp3.Request.Builder()
                .url(url)
                .head()
                .build()
            
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val contentLength = response.header("Content-Length")?.toLongOrNull()
                val contentType = response.header("Content-Type") ?: "unknown"
                val acceptRanges = response.header("Accept-Ranges") == "bytes"
                val contentRange = response.header("Content-Range")
                
                StreamingInfo(
                    url = url,
                    contentLength = contentLength,
                    contentType = contentType,
                    supportsRangeRequests = acceptRanges,
                    contentRange = contentRange,
                    isStreaming = contentLength == null || contentRange != null
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("RemoteMediaHandler", "Failed to get streaming info for $url", e)
            null
        }
    }
    
    fun clearCache() {
        try {
            httpClient.cache?.evictAll()
            Log.d("RemoteMediaHandler", "HTTP cache cleared")
        } catch (e: Exception) {
            Log.e("RemoteMediaHandler", "Failed to clear cache", e)
        }
    }
    
    data class RemoteMediaInfo(
        val url: String,
        val contentLength: Long,
        val contentType: String,
        val supportsRangeRequests: Boolean,
        val isAccessible: Boolean
    )
    
    data class StreamingInfo(
        val url: String,
        val contentLength: Long?,
        val contentType: String,
        val supportsRangeRequests: Boolean,
        val contentRange: String?,
        val isStreaming: Boolean
    )
}

// Network utilities
object NetworkUtils {
    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) {
            Log.e("NetworkUtils", "Error checking network availability", e)
            false
        }
    }
    
    fun getNetworkType(context: Context): String {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            when {
                capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
                capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            Log.e("NetworkUtils", "Error getting network type", e)
            "Unknown"
        }
    }
    
    fun shouldUseHighQualityStreaming(networkType: String): Boolean {
        return when (networkType) {
            "WiFi", "Ethernet" -> true
            "Cellular" -> false
            else -> false
        }
    }
}
