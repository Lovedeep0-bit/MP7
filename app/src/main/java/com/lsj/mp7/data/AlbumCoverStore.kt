package com.lsj.mp7.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.albumCoverStore: DataStore<Preferences> by preferencesDataStore(name = "album_covers")

class AlbumCoverStore(private val context: Context) {
    
    private fun keyFor(folderName: String): Preferences.Key<String> = 
        stringPreferencesKey("cover_${folderName.lowercase()}")
    
    /**
     * Save custom album cover URI for a folder
     */
    suspend fun saveCustomCover(folderName: String, coverUri: String) {
        try {
            context.albumCoverStore.edit { prefs ->
                prefs[keyFor(folderName)] = coverUri
            }
        } catch (e: Exception) {
            android.util.Log.e("AlbumCoverStore", "Failed to save custom cover for $folderName", e)
        }
    }
    
    /**
     * Get custom album cover URI for a folder
     */
    suspend fun getCustomCover(folderName: String): String? {
        return try {
            val prefs = context.albumCoverStore.data.first()
            prefs[keyFor(folderName)]
        } catch (e: Exception) {
            android.util.Log.e("AlbumCoverStore", "Failed to get custom cover for $folderName", e)
            null
        }
    }
    
    /**
     * Get custom album cover URI flow for a folder
     */
    fun getCustomCoverFlow(folderName: String): Flow<String?> {
        return context.albumCoverStore.data.map { prefs ->
            prefs[keyFor(folderName)]
        }
    }
    
    /**
     * Remove custom cover for a folder
     */
    suspend fun removeCustomCover(folderName: String) {
        try {
            context.albumCoverStore.edit { prefs ->
                prefs.remove(keyFor(folderName))
            }
        } catch (e: Exception) {
            android.util.Log.e("AlbumCoverStore", "Failed to remove custom cover for $folderName", e)
        }
    }
}

