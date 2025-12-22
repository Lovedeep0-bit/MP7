package com.lsj.mp7.util

object DurationFormatter {
    fun format(durationMs: Long?): String {
        if (durationMs == null || durationMs <= 0) return "--:--"
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
        else String.format("%02d:%02d", minutes, seconds)
    }
}


