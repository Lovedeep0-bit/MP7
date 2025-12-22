package com.lsj.mp7.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lsj.mp7.data.MediaRepository
import com.lsj.mp7.data.VideoFile
import com.lsj.mp7.data.VideoFolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class VideoListViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = MediaRepository(application)
    private val _all = MutableStateFlow<List<VideoFile>>(emptyList())
    private val _query = MutableStateFlow("")

    val query: StateFlow<String> = _query

    fun setQuery(q: String) { _query.value = q }

    fun loadAllVideos() {
        viewModelScope.launch { _all.value = repo.scanVideos() }
    }

    fun loadVideosInFolder(folderName: String) {
        viewModelScope.launch { _all.value = repo.videosInFolder(folderName) }
    }

    fun itemsForFolder(folderName: String): StateFlow<List<VideoFile>> =
        combine(_all, _query) { list, q ->
            val filtered = list.filter {
                val p = it.path ?: return@filter false
                val parts = p.replace('\\', '/').split('/')
                parts.getOrNull(parts.size - 2) == folderName
            }
            if (q.isBlank()) filtered else filtered.filter { 
                it.title.contains(q, ignoreCase = true) || (it.path?.contains(q, ignoreCase = true) == true) 
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun items(): StateFlow<List<VideoFile>> =
        combine(_all, _query) { list, q ->
            if (q.isBlank()) list else list.filter { 
                it.title.contains(q, ignoreCase = true) || (it.path?.contains(q, ignoreCase = true) == true) 
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

