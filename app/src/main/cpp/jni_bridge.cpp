/**
 * JNI BRIDGE
 * 
 * Connects Kotlin code to native C++ Battle Audio Engine.
 * Provides the interface for all audio processing operations.
 */

#include <jni.h>
#include <android/log.h>
#include "battle_audio_engine.h"
#include "SoundTouch.h"

#define LOG_TAG "JNI_Bridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Forward declarations
extern "C" {
    void* battle_engine_create();
    void battle_engine_destroy(void* handle);
    void battle_engine_configure(void* handle, int sampleRate, int channels);
    void battle_engine_set_speed(void* handle, float speed);
    void battle_engine_set_pitch(void* handle, float semitones);
    void battle_engine_set_rate(void* handle, float rate);
    void battle_engine_set_battle_mode(void* handle, bool enabled);
    void battle_engine_set_bass_boost(void* handle, float amount);
    void battle_engine_process(void* handle, const short* input, int numSamples, 
                               short* output, int* outputSamples);
    void battle_engine_flush(void* handle);
    void battle_engine_clear(void* handle);
}

// =============================================================================
// JNI METHOD IMPLEMENTATIONS
// =============================================================================

extern "C" {

// Package: com.ultramusic.player.audio
// Class: NativeBattleEngine

JNIEXPORT jlong JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_nativeCreate(
        JNIEnv* env, jobject thiz) {
    LOGI("Creating native Battle Audio Engine");
    void* engine = battle_engine_create();
    return reinterpret_cast<jlong>(engine);
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_nativeDestroy(
        JNIEnv* env, jobject thiz, jlong handle) {
    LOGI("Destroying native Battle Audio Engine");
    battle_engine_destroy(reinterpret_cast<void*>(handle));
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_nativeConfigure(
        JNIEnv* env, jobject thiz, jlong handle, jint sampleRate, jint channels) {
    LOGI("Configuring: %d Hz, %d channels", sampleRate, channels);
    battle_engine_configure(reinterpret_cast<void*>(handle), sampleRate, channels);
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_nativeSetSpeed(
        JNIEnv* env, jobject thiz, jlong handle, jfloat speed) {
    battle_engine_set_speed(reinterpret_cast<void*>(handle), speed);
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_nativeSetPitch(
        JNIEnv* env, jobject thiz, jlong handle, jfloat semitones) {
    battle_engine_set_pitch(reinterpret_cast<void*>(handle), semitones);
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_nativeSetRate(
        JNIEnv* env, jobject thiz, jlong handle, jfloat rate) {
    battle_engine_set_rate(reinterpret_cast<void*>(handle), rate);
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_nativeSetBattleMode(
        JNIEnv* env, jobject thiz, jlong handle, jboolean enabled) {
    battle_engine_set_battle_mode(reinterpret_cast<void*>(handle), enabled);
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_nativeSetBassBoost(
        JNIEnv* env, jobject thiz, jlong handle, jfloat amount) {
    battle_engine_set_bass_boost(reinterpret_cast<void*>(handle), amount);
}

JNIEXPORT jint JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_nativeProcess(
        JNIEnv* env, jobject thiz, jlong handle, 
        jshortArray inputArray, jint numSamples, jshortArray outputArray) {
    
    // Get input samples
    jshort* input = env->GetShortArrayElements(inputArray, nullptr);
    if (!input) {
        LOGE("Failed to get input array");
        return 0;
    }
    
    // Prepare output buffer
    jshort* output = env->GetShortArrayElements(outputArray, nullptr);
    if (!output) {
        env->ReleaseShortArrayElements(inputArray, input, 0);
        LOGE("Failed to get output array");
        return 0;
    }
    
    // Process
    int outputSamples = 0;
    battle_engine_process(reinterpret_cast<void*>(handle), 
                          input, numSamples, output, &outputSamples);
    
    // Release arrays
    env->ReleaseShortArrayElements(inputArray, input, 0);
    env->ReleaseShortArrayElements(outputArray, output, 0);
    
    return outputSamples;
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_nativeFlush(
        JNIEnv* env, jobject thiz, jlong handle) {
    battle_engine_flush(reinterpret_cast<void*>(handle));
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_nativeClear(
        JNIEnv* env, jobject thiz, jlong handle) {
    battle_engine_clear(reinterpret_cast<void*>(handle));
}

// =============================================================================
// DIRECT SOUNDTOUCH ACCESS (for simpler use cases)
// =============================================================================

JNIEXPORT jlong JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_soundTouchCreate(
        JNIEnv* env, jobject thiz) {
    soundtouch::SoundTouch* st = new soundtouch::SoundTouch();
    
    // Configure for high quality
    st->setSetting(SETTING_USE_AA_FILTER, 1);
    st->setSetting(SETTING_AA_FILTER_LENGTH, 64);
    st->setSetting(SETTING_SEQUENCE_MS, 40);
    st->setSetting(SETTING_SEEKWINDOW_MS, 15);
    st->setSetting(SETTING_OVERLAP_MS, 8);
    
    LOGI("SoundTouch created");
    return reinterpret_cast<jlong>(st);
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_soundTouchDestroy(
        JNIEnv* env, jobject thiz, jlong handle) {
    delete reinterpret_cast<soundtouch::SoundTouch*>(handle);
    LOGI("SoundTouch destroyed");
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_soundTouchSetSampleRate(
        JNIEnv* env, jobject thiz, jlong handle, jint sampleRate) {
    reinterpret_cast<soundtouch::SoundTouch*>(handle)->setSampleRate(sampleRate);
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_soundTouchSetChannels(
        JNIEnv* env, jobject thiz, jlong handle, jint channels) {
    reinterpret_cast<soundtouch::SoundTouch*>(handle)->setChannels(channels);
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_soundTouchSetTempo(
        JNIEnv* env, jobject thiz, jlong handle, jfloat tempo) {
    reinterpret_cast<soundtouch::SoundTouch*>(handle)->setTempo(tempo);
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_soundTouchSetPitch(
        JNIEnv* env, jobject thiz, jlong handle, jfloat pitch) {
    reinterpret_cast<soundtouch::SoundTouch*>(handle)->setPitch(pitch);
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_soundTouchSetPitchSemitones(
        JNIEnv* env, jobject thiz, jlong handle, jfloat semitones) {
    reinterpret_cast<soundtouch::SoundTouch*>(handle)->setPitchSemiTones(semitones);
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_soundTouchSetRate(
        JNIEnv* env, jobject thiz, jlong handle, jfloat rate) {
    reinterpret_cast<soundtouch::SoundTouch*>(handle)->setRate(rate);
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_soundTouchPutSamples(
        JNIEnv* env, jobject thiz, jlong handle, jshortArray samples, jint numSamples) {
    jshort* data = env->GetShortArrayElements(samples, nullptr);
    if (data) {
        soundtouch::SoundTouch* st = reinterpret_cast<soundtouch::SoundTouch*>(handle);
        st->putSamples(data, numSamples);
        env->ReleaseShortArrayElements(samples, data, 0);
    }
}

JNIEXPORT jint JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_soundTouchReceiveSamples(
        JNIEnv* env, jobject thiz, jlong handle, jshortArray output, jint maxSamples) {
    jshort* data = env->GetShortArrayElements(output, nullptr);
    if (!data) return 0;
    
    soundtouch::SoundTouch* st = reinterpret_cast<soundtouch::SoundTouch*>(handle);
    int received = st->receiveSamples(data, maxSamples);
    
    env->ReleaseShortArrayElements(output, data, 0);
    return received;
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_soundTouchFlush(
        JNIEnv* env, jobject thiz, jlong handle) {
    reinterpret_cast<soundtouch::SoundTouch*>(handle)->flush();
}

JNIEXPORT void JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_soundTouchClear(
        JNIEnv* env, jobject thiz, jlong handle) {
    reinterpret_cast<soundtouch::SoundTouch*>(handle)->clear();
}

JNIEXPORT jstring JNICALL
Java_com_ultramusic_player_audio_NativeBattleEngine_nativeGetVersion(
        JNIEnv* env, jobject thiz) {
    return env->NewStringUTF("UltraMusic Battle Engine v1.0 + SoundTouch");
}

} // extern "C"
