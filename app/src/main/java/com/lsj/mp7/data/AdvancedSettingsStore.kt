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

private val Context.advancedSettingsStore: DataStore<Preferences> by preferencesDataStore(name = "advanced_settings")

@Serializable
data class AdvancedSettings(
    val audioTrackAutoSelect: Boolean = true,
    val subtitleLanguage: String = "en",
    val subtitlesEnabled: Boolean = false,
    val audioTrackLanguage: String = "en",
    val showAdvancedControls: Boolean = false
)

class AdvancedSettingsStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    
    private val settingsKey = stringPreferencesKey("advanced_settings")
    
    /**
     * Save advanced settings
     */
    suspend fun saveSettings(settings: AdvancedSettings) {
        try {
            context.advancedSettingsStore.edit { prefs ->
                prefs[settingsKey] = json.encodeToString(settings)
            }
        } catch (e: Exception) {
            // Silently fail - settings are not critical
        }
    }
    
    /**
     * Get settings flow
     */
    fun getSettingsFlow(): Flow<AdvancedSettings> {
        return context.advancedSettingsStore.data.map { prefs ->
            getSettings(prefs)
        }
    }
    
    /**
     * Update specific setting
     */
    suspend fun updateAudioTrackAutoSelect(autoSelect: Boolean) {
        try {
            context.advancedSettingsStore.edit { prefs ->
                val currentSettings = getSettings(prefs)
                val newSettings = currentSettings.copy(audioTrackAutoSelect = autoSelect)
                prefs[settingsKey] = json.encodeToString(newSettings)
            }
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
    suspend fun updateSubtitleLanguage(language: String) {
        try {
            context.advancedSettingsStore.edit { prefs ->
                val currentSettings = getSettings(prefs)
                val newSettings = currentSettings.copy(subtitleLanguage = language)
                prefs[settingsKey] = json.encodeToString(newSettings)
            }
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
    suspend fun updateSubtitlesEnabled(enabled: Boolean) {
        try {
            context.advancedSettingsStore.edit { prefs ->
                val currentSettings = getSettings(prefs)
                val newSettings = currentSettings.copy(subtitlesEnabled = enabled)
                prefs[settingsKey] = json.encodeToString(newSettings)
            }
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
    suspend fun updateAudioTrackLanguage(language: String) {
        try {
            context.advancedSettingsStore.edit { prefs ->
                val currentSettings = getSettings(prefs)
                val newSettings = currentSettings.copy(audioTrackLanguage = language)
                prefs[settingsKey] = json.encodeToString(newSettings)
            }
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
    suspend fun updateShowAdvancedControls(show: Boolean) {
        try {
            context.advancedSettingsStore.edit { prefs ->
                val currentSettings = getSettings(prefs)
                val newSettings = currentSettings.copy(showAdvancedControls = show)
                prefs[settingsKey] = json.encodeToString(newSettings)
            }
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
    private fun getSettings(prefs: Preferences): AdvancedSettings {
        return try {
            val jsonString = prefs[settingsKey] ?: return AdvancedSettings()
            json.decodeFromString<AdvancedSettings>(jsonString)
        } catch (e: Exception) {
            AdvancedSettings()
        }
    }
}
