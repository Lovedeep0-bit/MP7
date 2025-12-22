package com.lsj.mp7.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.lang.Runtime
import com.lsj.mp7.BuildConfig

// 15. Application-wide Extensions for Better Performance

// Memory management extensions
val Runtime.mp7MemoryUsageRatio: Float
    get() {
        val used = totalMemory() - freeMemory()
        return used.toFloat() / maxMemory()
    }

val Runtime.mp7AvailableMemoryMB: Long
    get() = (maxMemory() - (totalMemory() - freeMemory())) / (1024 * 1024)

// Context extensions for better performance
fun Context.getOptimalThumbnailSize(): Pair<Int, Int> {
    val runtime = Runtime.getRuntime()
    val availableMemory = runtime.mp7AvailableMemoryMB
    return when {
        availableMemory < 50 -> Pair(160, 90)
        availableMemory < 100 -> Pair(200, 112)
        availableMemory < 200 -> Pair(240, 135)
        else -> Pair(320, 180)
    }
}

fun Context.getOptimalCacheSize(): Int {
    val runtime = Runtime.getRuntime()
    val availableMemory = runtime.mp7AvailableMemoryMB
    return when {
        availableMemory < 50 -> 10
        availableMemory < 100 -> 20
        availableMemory < 200 -> 30
        else -> 50
    }
}

fun Context.shouldPreloadThumbnails(): Boolean {
    val runtime = Runtime.getRuntime()
    return runtime.mp7AvailableMemoryMB > 100
}

fun Context.getOptimalConcurrentOperations(): Int {
    val runtime = Runtime.getRuntime()
    val availableMemory = runtime.mp7AvailableMemoryMB
    return when {
        availableMemory < 50 -> 1
        availableMemory < 100 -> 2
        else -> 3
    }
}

// File extensions for better performance
fun File.ensureDirectoryExists(): Boolean {
    return try {
        if (!exists()) {
            mkdirs()
        } else {
            true
        }
    } catch (e: Exception) {
        Log.e("FileExtensions", "Failed to create directory: $absolutePath", e)
        false
    }
}

fun File.getSizeInMB(): Long {
    return try {
        if (isDirectory) {
            listFiles()?.sumOf { it.getSizeInMB() } ?: 0L
        } else {
            length() / (1024 * 1024)
        }
    } catch (e: Exception) {
        Log.e("FileExtensions", "Failed to get file size: $absolutePath", e)
        0L
    }
}

fun File.deleteRecursively(): Boolean {
    return try {
        if (isDirectory) {
            listFiles()?.forEach { it.deleteRecursively() }
        }
        delete()
    } catch (e: Exception) {
        Log.e("FileExtensions", "Failed to delete file: $absolutePath", e)
        false
    }
}


// Performance measurement extensions
inline fun <T> measureTime(operation: String, block: () -> T): T {
    val startTime = System.nanoTime()
    val result = block()
    val duration = System.nanoTime() - startTime
    
    if (BuildConfig.DEBUG) {
        Log.d("PerformanceExtensions", "$operation took ${duration / 1_000_000}ms")
    }
    
    return result
}

suspend inline fun <T> measureTimeAsync(operation: String, block: suspend () -> T): T {
    val startTime = System.nanoTime()
    val result = block()
    val duration = System.nanoTime() - startTime
    
    if (BuildConfig.DEBUG) {
        Log.d("PerformanceExtensions", "$operation took ${duration / 1_000_000}ms")
    }
    
    return result
}

// Memory optimization extensions
fun optimizeMemoryUsage(context: Context) {
    try {
        val runtime = Runtime.getRuntime()
        val usageRatio = runtime.mp7MemoryUsageRatio
        
        when {
            usageRatio > 0.9f -> {
                Log.w("MemoryOptimization", "Critical memory usage: ${(usageRatio * 100).toInt()}%")
                // Emergency cleanup
                System.gc()
                System.runFinalization()
            }
            usageRatio > 0.8f -> {
                Log.w("MemoryOptimization", "High memory usage: ${(usageRatio * 100).toInt()}%")
                // Standard cleanup
                System.gc()
            }
            usageRatio > 0.7f -> {
                Log.i("MemoryOptimization", "Moderate memory usage: ${(usageRatio * 100).toInt()}%")
                // Light cleanup
            }
        }
    } catch (e: Exception) {
        Log.e("MemoryOptimization", "Error optimizing memory", e)
    }
}


// Cache management extensions
fun Context.getCacheSize(): Long {
    return try {
        val cacheDir = cacheDir
        cacheDir.getSizeInMB()
    } catch (e: Exception) {
        Log.e("CacheExtensions", "Failed to get cache size", e)
        0L
    }
}

fun Context.clearCache(): Boolean {
    return try {
        val cacheDir = cacheDir
        cacheDir.deleteRecursively()
        cacheDir.ensureDirectoryExists()
        true
    } catch (e: Exception) {
        Log.e("CacheExtensions", "Failed to clear cache", e)
        false
    }
}

fun Context.getCacheFiles(): List<File> {
    return try {
        val cacheDir = cacheDir
        cacheDir.listFiles()?.toList() ?: emptyList()
    } catch (e: Exception) {
        Log.e("CacheExtensions", "Failed to list cache files", e)
        emptyList()
    }
}

// Application lifecycle extensions
fun Context.isAppInForeground(): Boolean {
    return try {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningTasks = activityManager.getRunningTasks(1)
        if (runningTasks.isNotEmpty()) {
            val topActivity = runningTasks[0].topActivity
            topActivity?.packageName == packageName
        } else {
            false
        }
    } catch (e: Exception) {
        Log.e("AppLifecycleExtensions", "Failed to check app foreground state", e)
        false
    }
}

// Performance monitoring extensions
fun Context.getPerformanceStats(): Map<String, String> {
    return try {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val usageRatio = usedMemory.toFloat() / maxMemory
        
        mapOf(
            "memory_usage_mb" to "${usedMemory / (1024 * 1024)}",
            "max_memory_mb" to "${maxMemory / (1024 * 1024)}",
            "usage_ratio" to "${(usageRatio * 100).toInt()}%",
            "available_memory_mb" to "${runtime.mp7AvailableMemoryMB}",
            "cache_size_mb" to "${getCacheSize()}"
        )
    } catch (e: Exception) {
        Log.e("PerformanceExtensions", "Failed to get performance stats", e)
        emptyMap()
    }
}
