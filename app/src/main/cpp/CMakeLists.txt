# CMakeLists.txt for UltraMusicPlayer Native Audio Engine
# High-quality time-stretching and pitch-shifting with maximum sound quality preservation

cmake_minimum_required(VERSION 3.22.1)
project("ultramusic_audio" LANGUAGES CXX)

# C++17 for modern features
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optimization flags for best audio quality
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -ffast-math -funroll-loops")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -DNDEBUG")

# Find Oboe package (via prefab)
find_package(oboe REQUIRED CONFIG)

# Source files for our audio engine
set(AUDIO_ENGINE_SOURCES
    src/AudioEngine.cpp
    src/PhaseVocoder.cpp
    src/TimeStretcher.cpp
    src/PitchShifter.cpp
    src/FormantPreserver.cpp
    src/Resampler.cpp
    src/FFTProcessor.cpp
    src/AudioBuffer.cpp
    src/JNIBridge.cpp
)

# Create the shared library
add_library(ultramusic_audio SHARED ${AUDIO_ENGINE_SOURCES})

# Include directories
target_include_directories(ultramusic_audio PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

# Link libraries
target_link_libraries(ultramusic_audio
    oboe::oboe
    android
    log
    OpenSLES
)

# Enable SIMD optimizations for ARM
if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    target_compile_options(ultramusic_audio PRIVATE -march=armv8-a+simd)
elseif(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    target_compile_options(ultramusic_audio PRIVATE -mfpu=neon)
endif()

# Additional compile definitions
target_compile_definitions(ultramusic_audio PRIVATE
    ULTRA_MUSIC_ENGINE
    USE_HIGH_QUALITY_DSP
)
