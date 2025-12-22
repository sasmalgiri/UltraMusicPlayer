# CMakeLists.txt for UltraMusic Battle Audio Engine
# Professional-grade audio processing with SoundTouch + Custom DSP

cmake_minimum_required(VERSION 3.18.1)

project("ultramusic_audio" VERSION 1.0.0 LANGUAGES CXX C)

# C++17 for modern features
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optimization flags for maximum performance
set(CMAKE_CXX_FLAGS_RELEASE "-O3 -ffast-math -funroll-loops")
set(CMAKE_C_FLAGS_RELEASE "-O3 -ffast-math -funroll-loops")

# =============================================================================
# SOUNDTOUCH LIBRARY
# Uses stub implementation - for full quality, replace with official source
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
    ${SOUNDTOUCH_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# Link Android libraries
find_library(log-lib log)

target_link_libraries(ultramusic_audio
    ${log-lib}
)
