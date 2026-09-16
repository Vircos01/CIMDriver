package com.cimdriver.app.service

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class TrackingRecoveryManagerTest {

    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockPrefs: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var recoveryManager: TrackingRecoveryManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        `when`(mockContext.getSharedPreferences("tracking_recovery_prefs", Context.MODE_PRIVATE))
            .thenReturn(mockPrefs)
            
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        `when`(mockEditor.remove(anyString())).thenReturn(mockEditor)
        
        recoveryManager = TrackingRecoveryManager(mockContext)
    }

    @Test
    fun `shouldAttemptRecovery returns true when attempts under max`() = runBlocking {
        val tripId = 123L
        `when`(mockPrefs.getInt("recovery_attempts_$tripId", 0)).thenReturn(1) // 1 attempt so far
        
        val shouldRecover = recoveryManager.shouldAttemptRecovery(tripId)
        
        assertTrue("Should allow recovery if attempts < MAX", shouldRecover)
        // failTrip should not be called, but we can't easily verify since it hits AppDatabase directly
    }

    @Test
    fun `incrementRecoveryAttempt increments attempt counter`() {
        val tripId = 456L
        `when`(mockPrefs.getInt("recovery_attempts_$tripId", 0)).thenReturn(2)
        
        recoveryManager.incrementRecoveryAttempt(tripId)
        
        verify(mockEditor).putInt("recovery_attempts_$tripId", 3)
        verify(mockEditor).apply()
    }
    
    @Test
    fun `clearRecoveryAttempts removes preference`() {
        val tripId = 789L
        
        recoveryManager.clearRecoveryAttempts(tripId)
        
        verify(mockEditor).remove("recovery_attempts_$tripId")
        verify(mockEditor).apply()
    }
}
