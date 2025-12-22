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

private val Context.simpleProgressStore: DataStore<Preferences> by preferencesDataStore(name = "simple_progress")

@Serializable
data class SimpleProgressData(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val percentageWatched: Float = 0f,
    val lastWatched: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)

class SimpleProgressStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    
    private fun keyFor(uri: String): Preferences.Key<String> = stringPreferencesKey("progress_$uri")
    
    /**
     * Save video progress
     */
    suspend fun saveProgress(uri: String, currentPosition: Long, duration: Long = 0L) {
        try {
            context.simpleProgressStore.edit { prefs ->
                val currentData = getProgressData(prefs, uri)
                val resolvedDuration = if (duration > 0) duration else currentData.durationMs
                val percentageWatched = if (resolvedDuration > 0) {
                    (currentPosition.toFloat() / resolvedDuration.toFloat()).coerceIn(0f, 1f)
                } else currentData.percentageWatched
                
                val newData = currentData.copy(
                    positionMs = currentPosition,
                    durationMs = resolvedDuration,
                    percentageWatched = percentageWatched,
                    lastWatched = System.currentTimeMillis(),
                    isCompleted = percentageWatched >= 0.999f
                )
                
                prefs[keyFor(uri)] = json.encodeToString(newData)
            }
        } catch (e: Exception) {
            // Silently fail - progress tracking is not critical
        }
    }
    
    /**
     * Get progress flow for a video
     */
    fun getProgressFlow(uri: String): Flow<SimpleProgressData> {
        return context.simpleProgressStore.data.map { prefs ->
            getProgressData(prefs, uri)
        }
    }
    
    /**
     * Mark item as completed
     */
    suspend fun markAsCompleted(uri: String) {
        try {
            context.simpleProgressStore.edit { prefs ->
                val currentData = getProgressData(prefs, uri)
                val completedData = currentData.copy(
                    percentageWatched = 1.0f,
                    lastWatched = System.currentTimeMillis(),
                    isCompleted = true
                )
                prefs[keyFor(uri)] = json.encodeToString(completedData)
            }
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
    /**
     * Reset progress for a video
     */
    suspend fun resetProgress(uri: String) {
        try {
            context.simpleProgressStore.edit { prefs ->
                prefs.remove(keyFor(uri))
            }
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
    /**
     * Clear all progress
     */
    suspend fun clearAllProgress() {
        try {
            context.simpleProgressStore.edit { prefs ->
                prefs.clear()
            }
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
    private fun getProgressData(prefs: Preferences, uri: String): SimpleProgressData {
        return try {
            val jsonString = prefs[keyFor(uri)] ?: return SimpleProgressData()
            json.decodeFromString<SimpleProgressData>(jsonString)
        } catch (e: Exception) {
            SimpleProgressData()
        }
    }
}
