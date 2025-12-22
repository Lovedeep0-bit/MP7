package com.lsj.mp7.util

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Utility class for handling media permissions across different Android versions
 */
object PermissionUtils {
    
    /**
     * Check if the app has the necessary permissions to scan media files (audio and video)
     */
    fun hasMediaPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            // Android 13+ check granular permissions
            val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasVideo = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == android.content.pm.PackageManager.PERMISSION_GRANTED
            hasAudio && hasVideo
        } else {
            // Android 12 and below check storage permission
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get the list of required permissions based on Android version
     */
    fun getRequiredMediaPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= 33) {
            listOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    
    /**
     * Check if the app is running on Android 13+ (API 33+)
     */
    fun isAndroid13Plus(): Boolean {
        return Build.VERSION.SDK_INT >= 33
    }
    
    /**
     * Get a human-readable description of what permissions are needed
     */
    fun getPermissionDescription(): String {
        return if (isAndroid13Plus()) {
            "Audio and video access"
        } else {
            "Storage access"
        }
    }
}
