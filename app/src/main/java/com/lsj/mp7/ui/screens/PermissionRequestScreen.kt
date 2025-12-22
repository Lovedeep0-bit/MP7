package com.lsj.mp7.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestScreen(
    onPermissionsGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val permissionsState = rememberMediaPermissionsState()
    
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            onPermissionsGranted()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon or illustration could go here
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Media Access Required",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (Build.VERSION.SDK_INT >= 33) {
                "This app needs access to your audio files to play media content. " +
                "Please grant the requested permissions to continue."
            } else {
                "This app needs access to your device storage to play media files. " +
                "Please grant the requested permission to continue."
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { permissionsState.launchMultiplePermissionRequest() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Permissions")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (!permissionsState.allPermissionsGranted && permissionsState.permissions.isNotEmpty()) {
            Text(
                text = "Some permissions were denied. The app may not function properly.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}


