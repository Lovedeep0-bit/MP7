package com.lsj.mp7.player

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

private object HeadsetClickManager {
    private const val DOUBLE_TAP_WINDOW_MS = 300L
    private val handler = Handler(Looper.getMainLooper())
    private var lastUpTime = 0L
    private var pendingSingle: Runnable? = null

    fun onClick(onSingle: () -> Unit, onDouble: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastUpTime <= DOUBLE_TAP_WINDOW_MS) {
            pendingSingle?.let { handler.removeCallbacks(it) }
            pendingSingle = null
            lastUpTime = 0L
            onDouble()
        } else {
            lastUpTime = now
            val r = Runnable {
                onSingle()
                pendingSingle = null
                lastUpTime = 0L
            }
            pendingSingle = r
            handler.postDelayed(r, DOUBLE_TAP_WINDOW_MS)
        }
    }
}

class HeadsetButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_MEDIA_BUTTON != intent.action) return
        val event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java) ?: return
        if (event.action != KeyEvent.ACTION_UP) return
        when (event.keyCode) {
            KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                HeadsetClickManager.onClick(
                    onSingle = { withController(context) { c -> if (c.isPlaying) c.pause() else c.play() } },
                    onDouble = { withController(context) { c -> c.seekToNext() } }
                )
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> withController(context) { it.play() }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> withController(context) { it.pause() }
            KeyEvent.KEYCODE_MEDIA_NEXT -> withController(context) { it.seekToNext() }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> withController(context) { it.seekToPrevious() }
        }
    }

    private fun withController(context: Context, action: (MediaController) -> Unit) {
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            try {
                val controller = future.get()
                action(controller)
                controller.release()
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))
    }
}


