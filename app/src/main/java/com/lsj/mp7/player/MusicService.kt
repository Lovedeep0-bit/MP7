package com.lsj.mp7.player

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.AudioFocusRequest
import androidx.media3.common.AudioAttributes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.DefaultMediaNotificationProvider
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
 
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

@OptIn(UnstableApi::class)
class MusicService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private lateinit var noisyReceiver: NoisyReceiver
    private lateinit var btReceiver: BluetoothReceiver
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var resumeOnFocusGain: Boolean = false
    

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        val exo = ExoPlayer.Builder(this).build().also { exoPlayer ->
            exoPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .build(),
                true
            )
            exoPlayer.setHandleAudioBecomingNoisy(true)
        }
        player = exo
        // Provide a sessionActivity so tapping the media notification opens the app
        val launchIntent = Intent(this, com.lsj.mp7.MainActivity::class.java).apply {
            action = "com.lsj.mp7.OPEN_NOW_PLAYING"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivity = PendingIntent.getActivity(
            this,
            1001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        mediaSession = MediaSession.Builder(this, exo)
            .setSessionActivity(sessionActivity)
            .setCallback(object : MediaSession.Callback {
                override fun onMediaButtonEvent(
                    mediaSession: MediaSession,
                    controllerInfo: MediaSession.ControllerInfo,
                    intent: Intent
                ): Boolean {
                    if (Intent.ACTION_MEDIA_BUTTON != intent.action) return false
                    val event: KeyEvent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                    }
                    if (event == null) return false
                    if (event.action != KeyEvent.ACTION_UP) return false
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            HeadsetClickManager.onClick(
                                onSingle = { togglePlayPause() },
                                onDouble = { player?.seekToNext() }
                            )
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> { startPlaybackWithFocus(); return true }
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> { player?.pause(); return true }
                        KeyEvent.KEYCODE_MEDIA_STOP -> { player?.pause(); return true }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> { player?.seekToNext(); return true }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> { player?.seekToPrevious(); return true }
                    }
                    return false
                }
            })
            .build()

        // Provide default notification for background playback; the service will manage foreground state
        setMediaNotificationProvider(DefaultMediaNotificationProvider(this))

        // Headset/noisy + Bluetooth actions
        noisyReceiver = NoisyReceiver { player?.pause() }
        registerReceiver(noisyReceiver, IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY))

        btReceiver = BluetoothReceiver(
            onDisconnected = { player?.pause() },
            onConnected = { /* auto-resume if it was playing before handled by controller */ }
        )
        if (canUseBluetoothReceivers()) {
            registerReceiver(btReceiver, btReceiver.intentFilter())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Keep running so background controls remain responsive
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // When the app is cleared from recents, stop playback and shut down the service
        player?.run {
            runCatching { stop() }
            runCatching { clearMediaItems() }
        }
        stopForeground(true)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        unregisterReceiver(noisyReceiver)
        runCatching { unregisterReceiver(btReceiver) }
        mediaSession?.run { release() }
        player?.run { release() }
        mediaSession = null
        player = null
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    private fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else startPlaybackWithFocus()
    }

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

    companion object {
        private const val NOTIFICATION_ID = 1
    }

    private fun canUseBluetoothReceivers(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            return granted
        }
        return true
    }

    private fun startPlaybackWithFocus() {
        val p = player ?: return
        val focusGranted = requestAudioFocus()
        if (focusGranted) {
            p.play()
        }
    }

    

    private fun requestAudioFocus(): Boolean {
        val listener = AudioManager.OnAudioFocusChangeListener { change ->
            when (change) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if (resumeOnFocusGain) {
                        player?.play()
                        resumeOnFocusGain = false
                    }
                    player?.volume = 1.0f
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    player?.volume = 0.2f
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    resumeOnFocusGain = player?.isPlaying == true
                    player?.pause()
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    resumeOnFocusGain = false
                    player?.pause()
                }
            }
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(listener)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setWillPauseWhenDucked(false)
                .build()
            audioFocusRequest = req
            audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                listener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    
}


