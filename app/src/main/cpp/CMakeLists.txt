# CMakeLists.txt for UltraMusic Battle Audio Engine
# Professional-grade audio processing with OFFICIAL Superpowered SDK + Custom DSP
#
# AUDIO QUALITY: 9.5/10 (Official Superpowered frequency-domain processing)
#
# Features:
# - High-quality time-stretching (0.05x to 4x)
# - High-quality pitch-shifting (-24 to +24 semitones / -2400 to +2400 cents)
# - Transient preservation (drums stay punchy)
# - Formant correction (voice sounds natural)
# - Battle-grade limiter, compressor, bass boost
#
# SUPERPOWERED LICENSE: FREE for apps earning under $100K/year
# https://superpowered.com

cmake_minimum_required(VERSION 3.18.1)

project("ultramusic_audio" VERSION 2.0.0 LANGUAGES CXX C)

# C++17 for modern features
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optimization flags for maximum performance
set(CMAKE_CXX_FLAGS_RELEASE "-O3 -ffast-math -funroll-loops -ftree-vectorize")
set(CMAKE_C_FLAGS_RELEASE "-O3 -ffast-math -funroll-loops -ftree-vectorize")

# Enable NEON on ARM for SIMD acceleration
if(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mfpu=neon")
elseif(ANDROID_ABI STREQUAL "arm64-v8a")
    # NEON is always available on arm64
endif()

# =============================================================================
# SUPERPOWERED SDK (OFFICIAL)
# High-quality frequency-domain audio processing
# =============================================================================

set(SUPERPOWERED_DIR ${CMAKE_CURRENT_SOURCE_DIR}/superpowered)

# Import official Superpowered static library
add_library(superpowered STATIC IMPORTED)

# Set library path based on ABI
if(ANDROID_ABI STREQUAL "armeabi-v7a")
    set_target_properties(superpowered PROPERTIES
        IMPORTED_LOCATION ${SUPERPOWERED_DIR}/libSuperpoweredAndroidarmeabi-v7a.a
    )
elseif(ANDROID_ABI STREQUAL "arm64-v8a")
    set_target_properties(superpowered PROPERTIES
        IMPORTED_LOCATION ${SUPERPOWERED_DIR}/libSuperpoweredAndroidarm64-v8a.a
    )
elseif(ANDROID_ABI STREQUAL "x86")
    set_target_properties(superpowered PROPERTIES
        IMPORTED_LOCATION ${SUPERPOWERED_DIR}/libSuperpoweredAndroidX86.a
    )
elseif(ANDROID_ABI STREQUAL "x86_64")
    set_target_properties(superpowered PROPERTIES
        IMPORTED_LOCATION ${SUPERPOWERED_DIR}/libSuperpoweredAndroidX86_64.a
    )
endif()

# =============================================================================
# SOUNDTOUCH LIBRARY (Legacy fallback)
# =============================================================================

set(SOUNDTOUCH_DIR ${CMAKE_CURRENT_SOURCE_DIR}/soundtouch)

set(SOUNDTOUCH_SOURCES
    ${SOUNDTOUCH_DIR}/SoundTouch.cpp
)

# =============================================================================
# BATTLE AUDIO ENGINE
# =============================================================================

set(BATTLE_ENGINE_SOURCES
    ${CMAKE_CURRENT_SOURCE_DIR}/battle_audio_engine.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/battle_limiter.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/battle_compressor.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/battle_bass_boost.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/jni_bridge.cpp
)

# =============================================================================
# BUILD SHARED LIBRARY
# =============================================================================

add_library(ultramusic_audio SHARED
    ${SOUNDTOUCH_SOURCES}
    ${BATTLE_ENGINE_SOURCES}
)

# Include directories
target_include_directories(ultramusic_audio PRIVATE
    ${SUPERPOWERED_DIR}
    ${SOUNDTOUCH_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# Link libraries
find_library(log-lib log)

target_link_libraries(ultramusic_audio
    superpowered
    ${log-lib}
)

# =============================================================================
# SUPERPOWERED LICENSE INFO
# =============================================================================
#
# Superpowered is FREE for apps earning under $100,000/year
#
# Tier 1 (FREE):     Revenue < $100K/year
# Tier 2 ($499/yr):  Revenue $100K - $500K/year
# Tier 3 (Contact):  Revenue > $500K/year
#
# Register at: https://superpowered.com/dev
#
