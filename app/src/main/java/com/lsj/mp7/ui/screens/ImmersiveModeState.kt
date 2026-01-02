package com.lsj.mp7.ui.screens

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

object ImmersiveModeState {
    private const val PREFS_NAME = "immersive_mode_prefs"
    private const val KEY_IMMERSIVE = "is_immersive_mode"
    
    private var _isImmersiveMode by mutableStateOf(false)
    var isImmersiveMode: Boolean
        get() = _isImmersiveMode
        set(value) {
            _isImmersiveMode = value
        }
    
    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isImmersiveMode = prefs.getBoolean(KEY_IMMERSIVE, false)
    }
    
    fun setImmersiveMode(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IMMERSIVE, enabled).apply()
        _isImmersiveMode = enabled
    }
    
    fun getImmersiveMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IMMERSIVE, false)
    }
}
