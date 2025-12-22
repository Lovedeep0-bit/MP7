package com.lsj.mp7.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberMediaPermissionsState(): MultiplePermissionsState {
    val permissions = buildList {
        if (Build.VERSION.SDK_INT >= 33) {
            // Android 13+ uses granular media permissions
            add(Manifest.permission.READ_MEDIA_AUDIO)
            add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            // Android 12 and below use READ_EXTERNAL_STORAGE
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    return rememberMultiplePermissionsState(permissions)
}

/**
 * Check if the app has the necessary permissions to scan media files
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
 * Check if permissions should be shown rationale
 */
fun shouldShowPermissionRationale(context: Context): Boolean {
    // This would typically be implemented with ActivityCompat.shouldShowRequestPermissionRationale
    // For now, return false as we're handling this in the UI
    return false
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionStatusBanner(
    permissionsState: MultiplePermissionsState,
    modifier: Modifier = Modifier
) {
    if (!permissionsState.allPermissionsGranted && permissionsState.permissions.isNotEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Media permissions required",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Button(
                    onClick = { permissionsState.launchMultiplePermissionRequest() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Grant")
                }
            }
        }
    }
}


