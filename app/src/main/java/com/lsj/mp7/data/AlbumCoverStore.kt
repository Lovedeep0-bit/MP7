package com.lsj.mp7.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

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
            val coverPath = getCustomCover(folderName)
            context.albumCoverStore.edit { prefs ->
                prefs.remove(keyFor(folderName))
            }
            // Delete the physical file if it exists and is in our app directory
            coverPath?.let { path ->
                if (path.startsWith("/") && path.contains("album_covers")) {
                    File(path).delete()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AlbumCoverStore", "Failed to remove custom cover for $folderName", e)
        }
    }
    
    /**
     * Save custom album cover by copying from source URI to app storage
     * Returns the internal file path on success, null on failure
     */
    suspend fun saveCustomCoverFromUri(folderName: String, sourceUri: Uri): String? {
        return try {
            // Create album_covers directory if it doesn't exist
            val coverDir = File(context.filesDir, "album_covers")
            if (!coverDir.exists()) {
                coverDir.mkdirs()
            }
            
            // Generate unique filename from folder name
            val fileName = generateFileName(folderName)
            val destFile = File(coverDir, fileName)
            
            // Copy and downsample the image
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                // First decode to get dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                
                // Reopen stream for actual decode
                context.contentResolver.openInputStream(sourceUri)?.use { secondStream ->
                    // Calculate sample size for 1024x1024 max
                    options.inSampleSize = calculateInSampleSize(options, 1024, 1024)
                    options.inJustDecodeBounds = false
                    
                    val bitmap = BitmapFactory.decodeStream(secondStream, null, options)
                    bitmap?.let {
                        // Save as JPEG with 90% quality
                        FileOutputStream(destFile).use { outputStream ->
                            it.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                        }
                        it.recycle()
                        
                        // Save the file path to DataStore
                        val filePath = destFile.absolutePath
                        saveCustomCover(folderName, filePath)
                        filePath
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AlbumCoverStore", "Failed to save custom cover for $folderName", e)
            null
        }
    }
    
    private fun generateFileName(folderName: String): String {
        val hash = MessageDigest.getInstance("MD5")
            .digest(folderName.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "${hash}.jpg"
    }
    
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
