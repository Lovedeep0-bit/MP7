package com.lsj.mp7.ui.screens

import android.content.Context

enum class DefaultTab {
    MP3,
    MP4
}

object DefaultTabState {
    private const val PREFS_NAME = "default_tab_prefs"
    private const val KEY_DEFAULT_TAB = "default_tab"

    fun getDefaultTab(context: Context): DefaultTab {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_DEFAULT_TAB, DefaultTab.MP3.name) ?: DefaultTab.MP3.name
        return runCatching { DefaultTab.valueOf(name) }.getOrDefault(DefaultTab.MP3)
    }

    fun setDefaultTab(context: Context, tab: DefaultTab) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DEFAULT_TAB, tab.name).apply()
    }
}


