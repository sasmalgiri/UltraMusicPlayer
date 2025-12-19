package com.ultramusic.player

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * UltraMusicPlayer Application
 * 
 * Professional music player with advanced speed and pitch control.
 * Features:
 * - Speed range: 0.05x to 10.0x
 * - Pitch range: -36 to +36 semitones
 * - Cent-level precision
 * - Formant preservation
 * - Multi-storage browsing (Internal + SD Card)
 * - Voice search
 */
@HiltAndroidApp
class UltraMusicApp : Application() {
    
    companion object {
        lateinit var instance: UltraMusicApp
            private set
            
        // App constants
        const val APP_NAME = "UltraMusic Player"
        const val VERSION = "1.0.0"
        
        // Extended speed limits (exceeding competition)
        const val MIN_SPEED = 0.05f
        const val MAX_SPEED = 10.0f
        const val SPEED_PRECISION = 0.01f
        
        // Extended pitch limits (3 octaves!)
        const val MIN_PITCH_SEMITONES = -36f
        const val MAX_PITCH_SEMITONES = 36f
        const val PITCH_PRECISION = 0.01f  // Cent-level
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
