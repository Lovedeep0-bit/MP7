package com.lsj.mp7.util

import android.util.Log

object SimpleErrorHandler {
    private const val TAG = "SimpleErrorHandler"
    
    /**
     * Safely execute a function and return null if it fails
     */
    fun <T> safeExecute(operation: String, block: () -> T): T? {
        return try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, "Error in $operation: ${e.message}")
            null
        }
    }
    
    /**
     * Safely execute a function with a default value if it fails
     */
    fun <T> safeExecuteWithDefault(default: T, operation: String, block: () -> T): T {
        return try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, "Error in $operation: ${e.message}")
            default
        }
    }
    
    /**
     * Log error without crashing
     */
    fun logError(operation: String, error: Throwable) {
        try {
            Log.e(TAG, "Error in $operation: ${error.message}")
        } catch (e: Exception) {
            // Even logging failed, don't crash
        }
    }
    
    /**
     * Check if an operation should be retried
     */
    fun shouldRetry(attempt: Int, maxAttempts: Int = SimpleConfig.MAX_RETRY_ATTEMPTS): Boolean {
        return attempt < maxAttempts
    }
}
