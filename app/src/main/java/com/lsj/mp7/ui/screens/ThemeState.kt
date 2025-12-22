package com.lsj.mp7.ui.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

enum class AppTheme {
    Light,
    Dark,
    OLED
}

object ThemeState {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "app_theme"
    
    private var _currentTheme by mutableStateOf(AppTheme.Dark)
    var currentTheme: AppTheme
        get() = _currentTheme
        set(value) {
            _currentTheme = value
        }
    
    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeName = prefs.getString(KEY_THEME, AppTheme.Dark.name) ?: AppTheme.Dark.name
        _currentTheme = try {
            AppTheme.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            AppTheme.Dark
        }
    }
    
    fun setTheme(context: Context, theme: AppTheme) {
        _currentTheme = theme
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }
    
    fun getTheme(context: Context): AppTheme {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeName = prefs.getString(KEY_THEME, AppTheme.Dark.name) ?: AppTheme.Dark.name
        return try {
            AppTheme.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            AppTheme.Dark
        }
    }
    
    fun isDarkTheme(): Boolean {
        return currentTheme == AppTheme.Dark || currentTheme == AppTheme.OLED
    }
    
    fun isOLEDTheme(): Boolean {
        return currentTheme == AppTheme.OLED
    }
}

@Composable
fun rememberThemeState(): AppTheme {
    val context = LocalContext.current
    return remember {
        mutableStateOf(ThemeState.getTheme(context))
    }.value
}

