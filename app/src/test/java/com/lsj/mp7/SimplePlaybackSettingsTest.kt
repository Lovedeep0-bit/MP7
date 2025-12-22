package com.lsj.mp7

import com.lsj.mp7.data.SimplePlaybackSettings
import org.junit.Test
import org.junit.Assert.*

class SimplePlaybackSettingsTest {
    
    @Test
    fun testDefaultSettings() {
        val settings = SimplePlaybackSettings()
        
        assertEquals(1.0f, settings.playbackSpeed, 0.01f)
        assertEquals(1.0f, settings.volume, 0.01f)
        assertFalse(settings.isMuted)
        assertTrue(settings.autoPlay)
        assertTrue(settings.rememberPosition)
    }
    
    @Test
    fun testCustomSettings() {
        val settings = SimplePlaybackSettings(
            playbackSpeed = 1.5f,
            volume = 0.7f,
            isMuted = true,
            autoPlay = false,
            rememberPosition = false
        )
        
        assertEquals(1.5f, settings.playbackSpeed, 0.01f)
        assertEquals(0.7f, settings.volume, 0.01f)
        assertTrue(settings.isMuted)
        assertFalse(settings.autoPlay)
        assertFalse(settings.rememberPosition)
    }
    
    @Test
    fun testSettingsCopy() {
        val original = SimplePlaybackSettings()
        val modified = original.copy(
            playbackSpeed = 2.0f,
            volume = 0.5f
        )
        
        assertEquals(2.0f, modified.playbackSpeed, 0.01f)
        assertEquals(0.5f, modified.volume, 0.01f)
        assertEquals(original.isMuted, modified.isMuted)
        assertEquals(original.autoPlay, modified.autoPlay)
        assertEquals(original.rememberPosition, modified.rememberPosition)
    }
    
    @Test
    fun testSpeedBoundaries() {
        // Test valid speeds
        val validSpeeds = listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f, 4.0f)
        validSpeeds.forEach { speed ->
            val settings = SimplePlaybackSettings(playbackSpeed = speed)
            assertEquals(speed, settings.playbackSpeed, 0.01f)
        }
    }
    
    @Test
    fun testVolumeBoundaries() {
        // Test valid volumes
        val validVolumes = listOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f)
        validVolumes.forEach { volume ->
            val settings = SimplePlaybackSettings(volume = volume)
            assertEquals(volume, settings.volume, 0.01f)
        }
    }
    
    @Test
    fun testMuteLogic() {
        val mutedSettings = SimplePlaybackSettings(isMuted = true)
        val unmutedSettings = SimplePlaybackSettings(isMuted = false)
        
        assertTrue(mutedSettings.isMuted)
        assertFalse(unmutedSettings.isMuted)
    }
}
