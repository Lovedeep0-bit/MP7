package com.lsj.mp7.viewmodel

import com.lsj.mp7.data.AdvancedSettings
import com.lsj.mp7.data.SimplePlaybackSettings
import com.lsj.mp7.data.SimpleProgressData

data class PlayerState(
    val errorMessage: String? = null,
    val showControls: Boolean = true,
    val isPlayerReady: Boolean = false,
    val showResumeDialog: Boolean = false,
    val currentProgress: SimpleProgressData = SimpleProgressData(),
    val currentSettings: SimplePlaybackSettings = SimplePlaybackSettings(),
    val currentAdvancedSettings: AdvancedSettings = AdvancedSettings(),
    val showSettings: Boolean = false,
    val showAdvancedSettings: Boolean = false
)
