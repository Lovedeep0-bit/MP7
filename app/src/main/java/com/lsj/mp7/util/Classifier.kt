package com.lsj.mp7.util

import com.lsj.mp7.data.AudioCategory

object Classifier {
    private val audiobookKeywords = listOf("audiobook", "audio book", "books")
    private val podcastKeywords = listOf("podcast", "podcasts")
    private val lectureKeywords = listOf("lecture", "lectures", "course", "class", "lesson")

    fun classifyFromPath(pathLower: String): AudioCategory {
        return when {
            audiobookKeywords.any { pathLower.contains(it) } -> AudioCategory.SONGS
            podcastKeywords.any { pathLower.contains(it) } -> AudioCategory.SONGS
            lectureKeywords.any { pathLower.contains(it) } -> AudioCategory.SONGS
            else -> AudioCategory.SONGS
        }
    }
}


