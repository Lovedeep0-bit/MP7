package com.lsj.mp7.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object SettingsState {
    var isSettingsOpen by mutableStateOf(false)
        private set
    
    fun openSettings() {
        isSettingsOpen = true
    }
    
    fun closeSettings() {
        isSettingsOpen = false
    }
}

