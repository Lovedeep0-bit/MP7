package com.lsj.mp7.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lsj.mp7.data.AudioCategory
import com.lsj.mp7.data.AudioFile
import com.lsj.mp7.data.AudioFolder
import com.lsj.mp7.data.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class AudioListViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = MediaRepository(application)
    private val _all = MutableStateFlow<List<AudioFile>>(emptyList())
    private val _query = MutableStateFlow("")

    val query: StateFlow<String> = _query

    fun setQuery(q: String) { _query.value = q }

    fun loadAllAudios() {
        viewModelScope.launch { _all.value = repo.scanAudio() }
    }

    fun loadAudiosInMusicFolder() {
        viewModelScope.launch { _all.value = repo.audiosInMusicFolder() }
    }

    fun itemsFor(category: AudioCategory): StateFlow<List<AudioFile>> =
        combine(_all, _query) { list, q ->
            // Since AudioFile doesn't have category, we'll return all items for now
            // In a real implementation, you would categorize based on path or other criteria
            val filtered = list
            if (q.isBlank()) filtered else filtered.filter { it.title.contains(q, true) || (it.path?.contains(q, true) == true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun itemsForFolder(folderName: String): StateFlow<List<AudioFile>> =
        combine(_all, _query) { list, q ->
            val filtered = list.filter {
                // Use path to determine folder
                it.path?.contains(folderName, ignoreCase = true) == true
            }
            if (q.isBlank()) filtered else filtered.filter { it.title.contains(q, true) || (it.path?.contains(q, true) == true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun items(): StateFlow<List<AudioFile>> =
        combine(_all, _query) { list, q ->
            if (q.isBlank()) list else list.filter { it.title.contains(q, true) || (it.path?.contains(q, true) == true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}


