package com.lsj.mp7.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.iconDataStore: DataStore<Preferences> by preferencesDataStore(name = "icon_settings")

object IconColorStore {
    private val ICON_KEY = stringPreferencesKey("app_icon")

    fun getIconFlow(context: Context): Flow<String> {
        return context.iconDataStore.data.map { preferences ->
            preferences[ICON_KEY] ?: "Default"
        }
    }

    suspend fun setIcon(context: Context, iconName: String) {
        context.iconDataStore.edit { preferences ->
            preferences[ICON_KEY] = iconName
        }
        applyIconChange(context, iconName)
    }

    private fun applyIconChange(context: Context, iconName: String) {
        val packageManager = context.packageManager
        val packageName = context.packageName // The applicationId (com.lsj.mp7.debug)
        val componentNamespace = "com.lsj.mp7" // The actual namespace where components are defined

        val icons = listOf("Default", "Red", "Blue", "Green", "Lavender", "Pink", "Gradient")
        
        try {
            // 1. Enable the selected icon first
            val activeComponentName = ComponentName(packageName, "$componentNamespace.MainActivity$iconName")
            packageManager.setComponentEnabledSetting(
                activeComponentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // 2. Disable all other icons
            icons.filter { it != iconName }.forEach { name ->
                val componentName = ComponentName(packageName, "$componentNamespace.MainActivity$name")
                packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
            
            // 3. To apply the change immediately and notify the launcher,
            // we must kill the current process. Using 0 instead of DONT_KILL_APP
            // on the last 'enable' call is the standard way to do this.
            packageManager.setComponentEnabledSetting(
                activeComponentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                0 // This triggers launcher refresh and restarts the app
            )
            
        } catch (e: Exception) {
            android.util.Log.e("IconColorStore", "Failed to apply icon change to $iconName", e)
        }
    }
}
