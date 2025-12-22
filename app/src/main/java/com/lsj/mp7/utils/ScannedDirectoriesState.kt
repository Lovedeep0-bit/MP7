package com.lsj.mp7.utils

import android.content.Context
import android.os.Environment

object ScannedDirectoriesState {
    private const val PREFS_NAME = "scanned_dirs_prefs"
    private const val KEY_DIRS = "scanned_dirs"

    private fun uriToDisplayPath(raw: String): String? = runCatching {
        if (!raw.startsWith("content://")) return raw
        val uri = android.net.Uri.parse(raw)
        val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
        // e.g. primary:Download/Foo -> /storage/emulated/0/Download/Foo
        val parts = docId.split(":")
        val volume = parts.getOrNull(0) ?: return raw
        val relPath = parts.getOrNull(1) ?: ""
        val base = if (volume.equals("primary", ignoreCase = true)) {
            "/storage/emulated/0"
        } else {
            "/storage/$volume"
        }
        (base + "/" + relPath).replace("//", "/")
    }.getOrNull()

    private fun defaultAudioDirs(): Set<String> {
        return listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)?.absolutePath
        ).toSet()
    }

    private fun defaultVideoDirs(): Set<String> {
        return listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)?.absolutePath
        ).toSet()
    }

    private fun defaultDirs(): Set<String> {
        return defaultAudioDirs() + defaultVideoDirs()
    }

    fun getDirectories(context: Context): Set<String> {
        val saved = getAddedDirectories(context)
        // Always include defaults
        return (saved + defaultDirs()).toSet()
    }

    private fun getAddedDirectories(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedRaw = prefs.getStringSet(KEY_DIRS, null)?.toSet()?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
        return savedRaw.mapNotNull { uriToDisplayPath(it) }.toSet()
    }

    fun getAllowedAudioDirectories(context: Context): Set<String> {
        return getAddedDirectories(context) + defaultAudioDirs()
    }

    fun getAllowedVideoDirectories(context: Context): Set<String> {
        return getAddedDirectories(context) + defaultVideoDirs()
    }

    fun isDefaultDirectory(path: String): Boolean {
        return defaultDirs().any { path.startsWith(it, ignoreCase = true) }
    }

    fun addDirectory(context: Context, path: String) {
        if (path.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_DIRS, null)?.toMutableSet() ?: mutableSetOf()
        // We store RAW uri strings in prefs if possible, but the addDirectory takes a raw path/uri
        // If it's a content URI, store as is. If it's a path, maybe we should store normalized?
        // existing implementation stored the raw input 'path' (which was passed as uri.toString() from UI)
        // logic: uriToDisplayPath is used on retrieval.
        // so we save the 'path' (URI string) directly.
        
        // Check duplicates? 'resolved' check below suggests we want to avoid duplicates by display path
        // but we store raw. This is slightly tricky if we don't have the original URI for the resolved path.
        // Simplified: Just add the string passed in.
        current.add(path.trim())
        prefs.edit().putStringSet(KEY_DIRS, current).apply()
    }

    fun removeDirectory(context: Context, path: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_DIRS, null)?.toMutableSet() ?: return
        
        // 'path' argument here is the DISPLAY path (e.g. /storage/emulated/0/Foo)
        // We need to remove the entry that resolves to this path.
        val toRemove = current.find { uriToDisplayPath(it) == path }
        
        if (toRemove != null) {
            current.remove(toRemove)
            prefs.edit().putStringSet(KEY_DIRS, current).apply()
        }
    }
}


