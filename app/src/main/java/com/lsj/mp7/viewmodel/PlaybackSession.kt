package com.lsj.mp7.viewmodel

data class PlaybackSession(
    val uri: String = "",
    val position: Long = 0L,
    val isPlaying: Boolean = false
)
