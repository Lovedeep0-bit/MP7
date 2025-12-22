package com.lsj.mp7.util

object SimpleConfig {
    // Basic settings to prevent crashes
    const val MAX_VIDEOS_TO_LOAD = 100
    const val TIMEOUT_MS = 5000L
    const val MAX_RETRY_ATTEMPTS = 2
    
    // Debug settings
    object Debug {
        const val ENABLE_LOGGING = true
        const val LOG_TAG = "MP7Simple"
    }
}
