package com.lsj.mp7.ui.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object AppNavigator {
    private val _openNowPlaying = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openNowPlaying: SharedFlow<Unit> = _openNowPlaying

    fun triggerOpenNowPlaying() {
        _openNowPlaying.tryEmit(Unit)
    }
}


