package com.ultramusic.player

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * UltraMusic Player Application
 * 
 * Professional music player with industry-leading audio manipulation:
 * - Speed range: 0.05x to 10.0x (vs competition's 0.25x-4x)
 * - Pitch range: -36 to +36 semitones (vs competition's ±12)
 * - Cent-level precision for pitch control
 * - Audio presets (Nightcore, Slowed, Vaporwave, etc.)
 */
@HiltAndroidApp
class UltraMusicApp : Application() {
    
    companion object {
        const val APP_NAME = "UltraMusic Player"
        const val VERSION = "1.0.0"
        
        // Extended speed limits - EXCEEDS ALL COMPETITION
        const val MIN_SPEED = 0.05f
        const val MAX_SPEED = 10.0f
        const val DEFAULT_SPEED = 1.0f
        const val SPEED_STEP = 0.01f
        
        // Extended pitch limits - 3 OCTAVES!
        const val MIN_PITCH_SEMITONES = -36f
        const val MAX_PITCH_SEMITONES = 36f
        const val DEFAULT_PITCH = 0f
        const val PITCH_STEP = 0.01f // Cent-level precision
    }
}
