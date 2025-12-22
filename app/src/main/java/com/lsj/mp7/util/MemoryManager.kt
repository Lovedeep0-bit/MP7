package com.lsj.mp7.util

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.Runtime

// Enhanced Memory Manager with Proactive Management
class MemoryManager private constructor() {
    companion object {
        @Volatile
        private var INSTANCE: MemoryManager? = null
        private const val MEMORY_WARNING_THRESHOLD = 0.7f
        private const val MEMORY_CRITICAL_THRESHOLD = 0.8f
        private const val MEMORY_EMERGENCY_THRESHOLD = 0.9f
        private const val GC_COOLDOWN_MS = 5000L
        private var lastGcTime = 0L
        
        fun getInstance(): MemoryManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MemoryManager().also { INSTANCE = it }
            }
        }
    }
    
    private val memoryMonitorJob = SupervisorJob()
    private val monitoringScope = CoroutineScope(Dispatchers.IO + memoryMonitorJob)
    private var isMonitoring = false
    
    fun startMemoryMonitoring(context: Context) {
        if (isMonitoring) return
        isMonitoring = true
        
        monitoringScope.launch {
            while (isActive) {
                try {
                    val memoryInfo = getMemoryInfo()
                    val usageRatio = memoryInfo.used.toFloat() / memoryInfo.total
                    
                    when {
                        usageRatio >= MEMORY_EMERGENCY_THRESHOLD -> {
                            emergencyMemoryCleanup(context)
                            delay(2000) // Longer delay after emergency cleanup
                        }
                        usageRatio >= MEMORY_CRITICAL_THRESHOLD -> {
                            criticalMemoryCleanup(context)
                            delay(1500)
                        }
                        usageRatio >= MEMORY_WARNING_THRESHOLD -> {
                            warningMemoryCleanup(context)
                            delay(3000)
                        }
                        else -> delay(5000) // Normal monitoring interval
                    }
                } catch (e: Exception) {
                    Log.e("MemoryManager", "Memory monitoring error", e)
                    delay(5000)
                }
            }
        }
    }
    
    suspend fun emergencyMemoryCleanup(context: Context) = withContext(Dispatchers.IO) {
        Log.w("MemoryManager", "Emergency memory cleanup triggered")
        try {
            // Clear all caches aggressively
            
            // Force GC if cooldown period has passed
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastGcTime > GC_COOLDOWN_MS) {
                System.gc()
                System.runFinalization()
                lastGcTime = currentTime
            }
            
            // Clear app cache directory safely
            try {
                val cacheDir = context.cacheDir
                if (cacheDir.exists()) {
                    cacheDir.listFiles()?.forEach { file ->
                        try {
                            if (file.isDirectory) {
                                file.deleteRecursively()
                            } else {
                                file.delete()
                            }
                        } catch (e: Exception) {
                            Log.w("MemoryManager", "Could not delete cache file: ${file.name}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("MemoryManager", "Could not clear cache directory", e)
            }
        } catch (e: Exception) {
            Log.e("MemoryManager", "Emergency cleanup failed", e)
        }
        Unit // Explicit return to satisfy expression requirement
    }
    
    private suspend fun criticalMemoryCleanup(context: Context) = withContext(Dispatchers.IO) {
        
        if (System.currentTimeMillis() - lastGcTime > GC_COOLDOWN_MS / 2) {
            System.gc()
            lastGcTime = System.currentTimeMillis()
        }
    }
    
    private suspend fun warningMemoryCleanup(context: Context) = withContext(Dispatchers.IO) {
        
    }
    
    data class MemoryInfo(val used: Long, val total: Long, val available: Long)
    
    private fun getMemoryInfo(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        val used = runtime.totalMemory() - runtime.freeMemory()
        val total = runtime.totalMemory()
        val available = runtime.maxMemory() - used
        
        return MemoryInfo(used, total, available)
    }
    
    fun stopMemoryMonitoring() {
        isMonitoring = false
        memoryMonitorJob.cancel()
    }
    
    fun getMemoryWarningThreshold(): Float = MEMORY_WARNING_THRESHOLD
    fun getMemoryCriticalThreshold(): Float = MEMORY_CRITICAL_THRESHOLD
    fun getMemoryEmergencyThreshold(): Float = MEMORY_EMERGENCY_THRESHOLD
    
    fun logMemoryStatus(context: Context) {
        val memoryInfo = getMemoryInfo()
        val usageRatio = memoryInfo.used.toFloat() / memoryInfo.total
        Log.d("MemoryManager", "Memory usage: ${(usageRatio * 100).toInt()}% (${memoryInfo.used / 1024 / 1024}MB / ${memoryInfo.total / 1024 / 1024}MB)")
    }
}