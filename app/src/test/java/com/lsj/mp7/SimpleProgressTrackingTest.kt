package com.lsj.mp7

import com.lsj.mp7.data.SimpleProgressData
import org.junit.Test
import org.junit.Assert.*

class SimpleProgressTrackingTest {
    
    @Test
    fun testSimpleProgressData() {
        val progress = SimpleProgressData(
            positionMs = 60000L, // 1 minute
            durationMs = 120000L, // 2 minutes
            percentageWatched = 0.5f,
            lastWatched = System.currentTimeMillis(),
            isCompleted = false
        )
        
        assertEquals(60000L, progress.positionMs)
        assertEquals(120000L, progress.durationMs)
        assertEquals(0.5f, progress.percentageWatched, 0.01f)
        assertFalse(progress.isCompleted)
    }
    
    @Test
    fun testProgressCompletion() {
        val progress = SimpleProgressData(
            positionMs = 114000L, // 95% of 2 minutes
            durationMs = 120000L,
            percentageWatched = 0.95f,
            lastWatched = System.currentTimeMillis(),
            isCompleted = true
        )
        
        assertTrue(progress.isCompleted)
        assertEquals(0.95f, progress.percentageWatched, 0.01f)
    }
    
    @Test
    fun testProgressCalculation() {
        val positionMs = 90000L
        val durationMs = 120000L
        val expectedPercentage = 0.75f
        
        val calculatedPercentage = positionMs.toFloat() / durationMs.toFloat()
        assertEquals(expectedPercentage, calculatedPercentage, 0.01f)
    }
    
    @Test
    fun testProgressBoundaries() {
        // Test 0% progress
        val zeroProgress = SimpleProgressData(
            positionMs = 0L,
            durationMs = 120000L,
            percentageWatched = 0f,
            isCompleted = false
        )
        assertEquals(0f, zeroProgress.percentageWatched, 0.01f)
        assertFalse(zeroProgress.isCompleted)
        
        // Test 100% progress
        val fullProgress = SimpleProgressData(
            positionMs = 120000L,
            durationMs = 120000L,
            percentageWatched = 1.0f,
            isCompleted = true
        )
        assertEquals(1.0f, fullProgress.percentageWatched, 0.01f)
        assertTrue(fullProgress.isCompleted)
    }
}
