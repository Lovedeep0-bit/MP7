package com.lsj.mp7.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.lsj.mp7.data.MediaRepository
import com.lsj.mp7.data.AudioFolder

data class HomeUiState(
    val folders: List<AudioFolder> = emptyList(),
    val musicSubfolders: List<AudioFolder> = emptyList(),
    val allAudios: List<com.lsj.mp7.data.AudioFile> = emptyList(),
    val isLoading: Boolean = true,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState
    private val repo = MediaRepository(application)

    init {
        refreshCounts()
    }

    fun getApplicationContext() = getApplication<Application>().applicationContext

    fun refreshCounts() = viewModelScope.launch {
        // Only mark loading if we don't have data yet
        if (_uiState.value.musicSubfolders.isEmpty()) {
            _uiState.value = _uiState.value.copy(isLoading = true)
        }
        val result = repo.getFullAudioData()
        
        // For MP3-only UI, show folders as music subfolders
        _uiState.value = HomeUiState(
            folders = result.folders,
            musicSubfolders = result.folders,
            allAudios = result.all,
            isLoading = false,
        )
    }
}


