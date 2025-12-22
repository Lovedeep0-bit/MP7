package com.lsj.mp7.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object PlayerConnection {
    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = _controller

    fun connect(context: Context) {
        if (_controller.value != null) return
        // Build controller; binding will start the service as needed when playing

        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            try {
                _controller.value = future.get()
            } catch (_: Exception) { }
        }, ContextCompat.getMainExecutor(context))
    }

    fun disconnect() {
        _controller.value?.release()
        _controller.value = null
    }
}


