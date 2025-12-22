package com.lsj.mp7.data

import kotlinx.serialization.Serializable

// Basic data classes for media playback
@Serializable
data class AudioTrack(
    val id: String,
    val language: String? = null,
    val label: String? = null,
    val channelCount: Int = 0,
    val sampleRate: Int = 0,
    val bitrate: Long = 0L,
    val isSelected: Boolean = false,
    val isDefault: Boolean = false
)

@Serializable
data class SubtitleTrack(
    val id: String,
    val language: String? = null,
    val label: String? = null,
    val isEmbedded: Boolean = false,
    val isSelected: Boolean = false,
    val isDefault: Boolean = false
)

@Serializable
enum class AspectRatio {
    FIT,
    FILL,
    STRETCH,
    ORIGINAL,
    CUSTOM_16_9,
    CUSTOM_4_3
}

@Serializable
data class AudioFile(
    val id: Long,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val duration: Long = 0L,
    val uri: String,
    val size: Long = 0L,
    val path: String? = null
)

@Serializable
data class AudioFolder(
    val id: Long,
    val name: String,
    val path: String,
    val audioCount: Int = 0,
    val totalDuration: Long = 0L
)

@Serializable
enum class AudioCategory {
    SONGS,
    ALBUMS,
    ARTISTS,
    FOLDERS,
    PLAYLISTS
}

@Serializable
data class AdvancedPlaybackSettings(
    val selectedAudioTrackId: String? = null,
    val selectedSubtitleTrackId: String? = null,
    val subtitlesEnabled: Boolean = false,
    val audioTrackAutoSelect: Boolean = true,
    val subtitleLanguage: String? = null
)

@Serializable
data class VideoMetadata(
    val audioTracks: List<AudioTrack> = emptyList(),
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val hasMultipleAudioTracks: Boolean = false,
    val hasSubtitles: Boolean = false,
    val duration: Long = 0L,
    val resolution: String? = null,
    val codec: String? = null
)

@Serializable
data class VideoFile(
    val id: Long,
    val title: String,
    val displayName: String,
    val duration: Long = 0L,
    val uri: String,
    val path: String? = null,
    val size: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val dateAdded: Long = 0L,
    val mimeType: String? = null,
    // Progress tracking fields
    val watchProgress: Float = 0f,
    val isCompleted: Boolean = false,
    val lastWatched: Long = 0L,
    val lastPlayPosition: Long = 0L,
    val parentFolder: String = "Movies",
    val isWatched: Boolean = false
)

@Serializable
data class VideoFolder(
    val id: Long,
    val name: String,
    val path: String,
    val videoCount: Int = 0,
    val totalDuration: Long = 0L
)

@Serializable
data class FolderItem(
    val id: Long,
    val name: String,
    val path: String,
    val videoCount: Int = 0,
    val videos: List<VideoFile> = emptyList()
)

