package com.lsj.mp7.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.simpleSettingsStore: DataStore<Preferences> by preferencesDataStore(name = "simple_settings")

@Serializable
data class SimplePlaybackSettings(
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val autoPlay: Boolean = true,
    val rememberPosition: Boolean = true,
    val aspectRatio: AspectRatio = AspectRatio.FIT,
    val isFullscreen: Boolean = false,
    val isPictureInPicture: Boolean = false
)

class SimplePlaybackSettingsStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    
    private val settingsKey = stringPreferencesKey("playback_settings")
    
    /**
     * Save playback settings
     */
    suspend fun saveSettings(settings: SimplePlaybackSettings) {
        try {
            context.simpleSettingsStore.edit { prefs ->
                prefs[settingsKey] = json.encodeToString(settings)
            }
        } catch (e: Exception) {
            // Silently fail - settings are not critical
        }
    }
    
    /**
     * Get settings flow
     */
    fun getSettingsFlow(): Flow<SimplePlaybackSettings> {
        return context.simpleSettingsStore.data.map { prefs ->
            getSettings(prefs)
        }
    }
    
    /**
     * Update specific setting
     */
    suspend fun updatePlaybackSpeed(speed: Float) {
        try {
            context.simpleSettingsStore.edit { prefs ->
                val currentSettings = getSettings(prefs)
                val newSettings = currentSettings.copy(playbackSpeed = speed.coerceIn(0.25f, 4.0f))
                prefs[settingsKey] = json.encodeToString(newSettings)
            }
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
    suspend fun updateVolume(volume: Float) {
        try {
            context.simpleSettingsStore.edit { prefs ->
                val currentSettings = getSettings(prefs)
                val newSettings = currentSettings.copy(
                    volume = volume.coerceIn(0f, 1f),
                    isMuted = volume <= 0f
                )
                prefs[settingsKey] = json.encodeToString(newSettings)
            }
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
    suspend fun toggleMute() {
        try {
            context.simpleSettingsStore.edit { prefs ->
                val currentSettings = getSettings(prefs)
                val newSettings = currentSettings.copy(isMuted = !currentSettings.isMuted)
                prefs[settingsKey] = json.encodeToString(newSettings)
            }
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
    suspend fun updateAutoPlay(autoPlay: Boolean) {
        try {
            context.simpleSettingsStore.edit { prefs ->
                val currentSettings = getSettings(prefs)
                val newSettings = currentSettings.copy(autoPlay = autoPlay)
                prefs[settingsKey] = json.encodeToString(newSettings)
            }
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
    private fun getSettings(prefs: Preferences): SimplePlaybackSettings {
        return try {
            val jsonString = prefs[settingsKey] ?: return SimplePlaybackSettings()
            json.decodeFromString<SimplePlaybackSettings>(jsonString)
        } catch (e: Exception) {
            SimplePlaybackSettings()
        }
    }
}
