/**
 * UltraMusicPlayer - JNIBridge.cpp
 * 
 * JNI interface connecting the native C++ audio engine
 * with Kotlin/Java Android code.
 */

#include <jni.h>
#include <android/log.h>
#include <string>
#include <memory>

#include "include/AudioEngine.h"
#include "include/TimeStretcher.h"
#include "include/PitchShifter.h"

#define LOG_TAG "UltraMusic-JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace ultramusic;

// Global audio engine instance
static std::unique_ptr<AudioEngine> g_audioEngine;

extern "C" {

// ============================================================================
// Audio Engine Lifecycle
// ============================================================================

JNIEXPORT jboolean JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeInit(
        JNIEnv* env, jobject thiz, jint sampleRate, jint channelCount) {
    
    LOGI("Initializing native audio engine: %dHz, %d channels", sampleRate, channelCount);
    
    try {
        g_audioEngine = std::make_unique<AudioEngine>();
        bool success = g_audioEngine->initialize(sampleRate, channelCount);
        
        if (success) {
            LOGI("Audio engine initialized successfully");
        } else {
            LOGE("Failed to initialize audio engine");
        }
        
        return success ? JNI_TRUE : JNI_FALSE;
    } catch (const std::exception& e) {
        LOGE("Exception during init: %s", e.what());
        return JNI_FALSE;
    }
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeShutdown(
        JNIEnv* env, jobject thiz) {
    
    LOGI("Shutting down native audio engine");
    
    if (g_audioEngine) {
        g_audioEngine->shutdown();
        g_audioEngine.reset();
    }
}

// ============================================================================
// Audio Loading
// ============================================================================

JNIEXPORT jboolean JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeLoadFile(
        JNIEnv* env, jobject thiz, jstring filePath) {
    
    if (!g_audioEngine) {
        LOGE("Audio engine not initialized");
        return JNI_FALSE;
    }
    
    const char* path = env->GetStringUTFChars(filePath, nullptr);
    LOGI("Loading audio file: %s", path);
    
    bool success = g_audioEngine->loadAudioFile(path);
    
    env->ReleaseStringUTFChars(filePath, path);
    
    return success ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeUnload(
        JNIEnv* env, jobject thiz) {
    
    if (g_audioEngine) {
        g_audioEngine->unloadAudio();
    }
}

// ============================================================================
// Playback Control
// ============================================================================

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativePlay(
        JNIEnv* env, jobject thiz) {
    
    if (g_audioEngine) {
        g_audioEngine->play();
    }
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativePause(
        JNIEnv* env, jobject thiz) {
    
    if (g_audioEngine) {
        g_audioEngine->pause();
    }
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeStop(
        JNIEnv* env, jobject thiz) {
    
    if (g_audioEngine) {
        g_audioEngine->stop();
    }
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeSeekTo(
        JNIEnv* env, jobject thiz, jlong framePosition) {
    
    if (g_audioEngine) {
        g_audioEngine->seekTo(framePosition);
    }
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeSeekToTime(
        JNIEnv* env, jobject thiz, jdouble seconds) {
    
    if (g_audioEngine) {
        g_audioEngine->seekToTime(seconds);
    }
}

// ============================================================================
// Speed Control - Extended Range: 0.05x to 10.0x
// ============================================================================

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeSetSpeed(
        JNIEnv* env, jobject thiz, jfloat speed) {
    
    if (g_audioEngine) {
        // Clamp to valid range
        float clampedSpeed = std::max(0.05f, std::min(10.0f, speed));
        LOGI("Setting speed: %.3f", clampedSpeed);
        g_audioEngine->setSpeed(clampedSpeed);
    }
}

JNIEXPORT jfloat JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeGetSpeed(
        JNIEnv* env, jobject thiz) {
    
    if (g_audioEngine) {
        return g_audioEngine->getCurrentSpeed();
    }
    return 1.0f;
}

// ============================================================================
// Pitch Control - Extended Range: -36 to +36 semitones
// ============================================================================

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeSetPitchSemitones(
        JNIEnv* env, jobject thiz, jfloat semitones) {
    
    if (g_audioEngine) {
        // Clamp to valid range (-36 to +36)
        float clamped = std::max(-36.0f, std::min(36.0f, semitones));
        LOGI("Setting pitch: %.2f semitones", clamped);
        g_audioEngine->setPitchSemitones(clamped);
    }
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeSetPitchCents(
        JNIEnv* env, jobject thiz, jfloat cents) {
    
    if (g_audioEngine) {
        // Clamp to valid range (-100 to +100)
        float clamped = std::max(-100.0f, std::min(100.0f, cents));
        g_audioEngine->setPitchCents(clamped);
    }
}

JNIEXPORT jfloat JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeGetPitchSemitones(
        JNIEnv* env, jobject thiz) {
    
    if (g_audioEngine) {
        return g_audioEngine->getCurrentPitchSemitones();
    }
    return 0.0f;
}

// ============================================================================
// Formant Control
// ============================================================================

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeSetFormantShift(
        JNIEnv* env, jobject thiz, jfloat shift) {
    
    if (g_audioEngine) {
        g_audioEngine->setFormantShift(shift);
    }
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeSetPreserveFormants(
        JNIEnv* env, jobject thiz, jboolean preserve) {
    
    if (g_audioEngine) {
        g_audioEngine->setPreserveFormants(preserve == JNI_TRUE);
    }
}

// ============================================================================
// Quality Settings
// ============================================================================

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeSetQualityMode(
        JNIEnv* env, jobject thiz, jint mode) {
    
    if (g_audioEngine) {
        g_audioEngine->setQualityMode(static_cast<QualityMode>(mode));
    }
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeSetAlgorithm(
        JNIEnv* env, jobject thiz, jint algorithm) {
    
    if (g_audioEngine) {
        g_audioEngine->setAlgorithm(static_cast<Algorithm>(algorithm));
    }
}

// ============================================================================
// Loop Control
// ============================================================================

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeSetLoopRegion(
        JNIEnv* env, jobject thiz, jlong startFrame, jlong endFrame) {
    
    if (g_audioEngine) {
        g_audioEngine->setLoopRegion(startFrame, endFrame);
    }
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeEnableLoop(
        JNIEnv* env, jobject thiz, jboolean enable) {
    
    if (g_audioEngine) {
        g_audioEngine->enableLoop(enable == JNI_TRUE);
    }
}

// ============================================================================
// State Queries
// ============================================================================

JNIEXPORT jboolean JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeIsPlaying(
        JNIEnv* env, jobject thiz) {
    
    if (g_audioEngine) {
        return g_audioEngine->isPlaying() ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

JNIEXPORT jlong JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeGetCurrentPosition(
        JNIEnv* env, jobject thiz) {
    
    if (g_audioEngine) {
        return g_audioEngine->getCurrentPosition();
    }
    return 0;
}

JNIEXPORT jlong JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeGetTotalFrames(
        JNIEnv* env, jobject thiz) {
    
    if (g_audioEngine) {
        return g_audioEngine->getTotalFrames();
    }
    return 0;
}

JNIEXPORT jdouble JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeGetCurrentTimeSeconds(
        JNIEnv* env, jobject thiz) {
    
    if (g_audioEngine) {
        return g_audioEngine->getCurrentTimeSeconds();
    }
    return 0.0;
}

JNIEXPORT jdouble JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeGetTotalTimeSeconds(
        JNIEnv* env, jobject thiz) {
    
    if (g_audioEngine) {
        return g_audioEngine->getTotalTimeSeconds();
    }
    return 0.0;
}

// ============================================================================
// BPM Detection
// ============================================================================

JNIEXPORT jfloat JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeDetectBPM(
        JNIEnv* env, jobject thiz) {
    
    if (g_audioEngine) {
        return g_audioEngine->detectBPM();
    }
    return 0.0f;
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeSetTargetBPM(
        JNIEnv* env, jobject thiz, jfloat bpm) {
    
    if (g_audioEngine) {
        g_audioEngine->setTargetBPM(bpm);
    }
}

// ============================================================================
// Export
// ============================================================================

JNIEXPORT jboolean JNICALL
Java_com_ultramusic_player_audio_NativeAudioEngine_nativeExportToFile(
        JNIEnv* env, jobject thiz, jstring outputPath, jstring format) {
    
    if (!g_audioEngine) {
        return JNI_FALSE;
    }
    
    const char* path = env->GetStringUTFChars(outputPath, nullptr);
    const char* fmt = env->GetStringUTFChars(format, nullptr);
    
    bool success = g_audioEngine->exportToFile(path, fmt);
    
    env->ReleaseStringUTFChars(outputPath, path);
    env->ReleaseStringUTFChars(format, fmt);
    
    return success ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
