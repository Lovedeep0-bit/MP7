package com.lsj.mp7.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.lsj.mp7.player.PlayerConnection

@Composable
fun AudioPlayerControls() {
    val controller = PlayerConnection.controller.collectAsState(initial = null).value
    if (controller == null) {
        Text("Idle", color = Color.White)
        return
    }
    Column(Modifier.fillMaxWidth().padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = { controller.seekToPreviousMediaItem() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = Color.White)
            }
            IconButton(onClick = {
                if (controller.isPlaying) controller.pause() else controller.play()
            }) {
                Icon(
                    if (controller.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            IconButton(onClick = { controller.seekToNextMediaItem() }) {
                Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White)
            }
        }
        // Simple seek bar placeholder (no time labels yet)
        Slider(value = 0f, onValueChange = {}, modifier = Modifier.fillMaxWidth())
    }
}


