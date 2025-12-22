package com.lsj.mp7.util

import android.util.Log
import java.lang.Runtime
import kotlinx.coroutines.*

// 13. Utility Classes for Enhanced Performance

class PerformanceMonitor {
    private var frameCount = 0
    private var lastFpsUpdate = System.currentTimeMillis()
    private var currentFps = 0
    
    fun onFrameRendered() {
        frameCount++
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFpsUpdate >= 1000) {
            currentFps = frameCount
            frameCount = 0
            lastFpsUpdate = currentTime
            Log.d("PerformanceMonitor", "FPS: $currentFps")
        }
    }
    fun getCurrentFps(): Int = currentFps
    companion object {
        @Volatile
        private var INSTANCE: PerformanceMonitor? = null
        fun getInstance(): PerformanceMonitor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PerformanceMonitor().also { INSTANCE = it }
            }
        }
    }
}

// Memory management extensions
val Runtime.memoryUsageRatio: Float
    get() {
        val used = totalMemory() - freeMemory()
        return used.toFloat() / maxMemory()
    }

val Runtime.availableMemoryMB: Long
    get() = (maxMemory() - (totalMemory() - freeMemory())) / (1024 * 1024)

// Coroutine extensions for safer execution
suspend fun <T> withTimeoutAndRetry(
    timeoutMs: Long,
    maxRetries: Int = 3,
    delayMs: Long = 1000,
    block: suspend () -> T
): T {
    repeat(maxRetries) { attempt ->
        try {
            return withTimeout(timeoutMs) {
                block()
            }
        } catch (e: TimeoutCancellationException) {
            if (attempt == maxRetries - 1) throw e
            delay(delayMs * (attempt + 1))
        }
    }
    error("Should not reach here")
}

// Performance measurement utilities
class PerformanceProfiler {
    val measurements = mutableMapOf<String, MutableList<Long>>()
    inline fun <T> measure(operation: String, block: () -> T): T {
        val startTime = System.nanoTime()
        val result = block()
        val duration = System.nanoTime() - startTime
        measurements.getOrPut(operation) { mutableListOf() }.add(duration)
        Log.d("PerformanceProfiler", "$operation took ${duration / 1_000_000}ms")
        return result
    }
    suspend inline fun <T> measureAsync(operation: String, block: suspend () -> T): T {
        val startTime = System.nanoTime()
        val result = block()
        val duration = System.nanoTime() - startTime
        measurements.getOrPut(operation) { mutableListOf() }.add(duration)
        Log.d("PerformanceProfiler", "$operation took ${duration / 1_000_000}ms")
        return result
    }
    fun getAverageTime(operation: String): Long {
        val times = measurements[operation] ?: return 0
        return if (times.isNotEmpty()) times.average().toLong() else 0
    }
    fun getStats(): Map<String, String> {
        return measurements.mapValues { (_, times) ->
            val avg = times.average()
            val min = times.minOrNull() ?: 0
            val max = times.maxOrNull() ?: 0
            "avg: ${(avg / 1_000_000).toInt()}ms, min: ${(min / 1_000_000).toInt()}ms, max: ${(max / 1_000_000).toInt()}ms, count: ${times.size}"
        }
    }
    fun clear() {
        measurements.clear()
    }
    companion object {
        @Volatile
        private var INSTANCE: PerformanceProfiler? = null
        fun getInstance(): PerformanceProfiler {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PerformanceProfiler().also { INSTANCE = it }
            }
        }
    }
}

// Memory optimization utilities
object MemoryOptimizer {
    fun getOptimalThumbnailSize(availableMemoryMB: Long): Pair<Int, Int> {
        return when {
            availableMemoryMB < 50 -> Pair(160, 90)
            availableMemoryMB < 100 -> Pair(200, 112)
            availableMemoryMB < 200 -> Pair(240, 135)
            else -> Pair(320, 180)
        }
    }
    fun getOptimalCacheSize(availableMemoryMB: Long): Int {
        return when {
            availableMemoryMB < 50 -> 10
            availableMemoryMB < 100 -> 20
            availableMemoryMB < 200 -> 30
            else -> 50
        }
    }
    fun shouldPreloadThumbnails(availableMemoryMB: Long): Boolean {
        return availableMemoryMB > 100
    }
    fun getOptimalConcurrentOperations(availableMemoryMB: Long): Int {
        return when {
            availableMemoryMB < 50 -> 1
            availableMemoryMB < 100 -> 2
            else -> 3
        }
    }
}

// Threading utilities
object ThreadingUtils {
    private val thumbnailDispatcher = Dispatchers.IO.limitedParallelism(3)
    private val networkDispatcher = Dispatchers.IO.limitedParallelism(2)
    fun getThumbnailDispatcher() = thumbnailDispatcher
    fun getNetworkDispatcher() = networkDispatcher
    suspend fun <T> executeOnThumbnailDispatcher(block: suspend () -> T): T {
        return withContext(thumbnailDispatcher) { block() }
    }
    suspend fun <T> executeOnNetworkDispatcher(block: suspend () -> T): T {
        return withContext(networkDispatcher) { block() }
    }
}
