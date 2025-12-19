/**
 * UltraMusicPlayer - AudioBuffer.h
 * 
 * Efficient audio buffer management with:
 * - SIMD-optimized operations
 * - Ring buffer for streaming
 * - Gain/mixing operations
 * - Format conversion
 */

#ifndef ULTRA_MUSIC_AUDIO_BUFFER_H
#define ULTRA_MUSIC_AUDIO_BUFFER_H

#include <vector>
#include <cstdint>
#include <cstring>
#include <algorithm>
#include <cmath>

#if defined(__ARM_NEON) || defined(__ARM_NEON__)
#include <arm_neon.h>
#define USE_NEON 1
#endif

namespace ultramusic {

/**
 * Audio buffer with optimized operations
 */
class AudioBuffer {
public:
    AudioBuffer(int32_t numFrames, int32_t numChannels);
    ~AudioBuffer() = default;
    
    // Accessors
    float* getWritePointer(int32_t channel);
    const float* getReadPointer(int32_t channel) const;
    float* getInterleavedWritePointer();
    const float* getInterleavedReadPointer() const;
    
    int32_t getNumFrames() const { return m_numFrames; }
    int32_t getNumChannels() const { return m_numChannels; }
    int32_t getNumSamples() const { return m_numFrames * m_numChannels; }
    
    // Resize
    void setSize(int32_t numFrames, int32_t numChannels);
    
    // Clear to silence
    void clear();
    void clear(int32_t startFrame, int32_t numFrames);
    
    // Fill with value
    void fill(float value);
    
    // Copy operations
    void copyFrom(const AudioBuffer& source);
    void copyFrom(const AudioBuffer& source, int32_t sourceStart, 
                  int32_t destStart, int32_t numFrames);
    void copyFrom(const float* source, int32_t numFrames, int32_t numChannels);
    
    // Gain operations (SIMD optimized)
    void applyGain(float gain);
    void applyGain(float gain, int32_t startFrame, int32_t numFrames);
    void applyGainRamp(float startGain, float endGain);
    
    // Mixing (SIMD optimized)
    void addFrom(const AudioBuffer& source, float gain = 1.0f);
    void addFrom(const float* source, int32_t numFrames, float gain = 1.0f);
    
    // Channel operations
    void copyChannel(int32_t srcChannel, int32_t destChannel);
    void addChannel(int32_t srcChannel, int32_t destChannel, float gain = 1.0f);
    
    // Analysis
    float getPeak() const;
    float getRMS() const;
    bool isSilent(float threshold = 1e-6f) const;
    
    // Format conversion
    void interleaveTo(float* dest) const;
    void deinterleaveFrom(const float* src);
    void convertToMono();
    void convertToStereo();
    
    // Normalize
    void normalize(float targetPeak = 1.0f);
    
    // Fade
    void fadeIn(int32_t numFrames);
    void fadeOut(int32_t numFrames);
    void crossfade(const AudioBuffer& other, int32_t crossfadeFrames);
    
private:
    std::vector<float> m_data;  // Interleaved storage
    int32_t m_numFrames;
    int32_t m_numChannels;
    
    // SIMD helpers
    void applyGainSIMD(float* data, int32_t numSamples, float gain);
    void addWithGainSIMD(float* dest, const float* src, int32_t numSamples, float gain);
    float getPeakSIMD(const float* data, int32_t numSamples) const;
};

/**
 * Ring buffer for streaming audio
 */
class RingBuffer {
public:
    RingBuffer(int32_t capacity, int32_t numChannels);
    ~RingBuffer() = default;
    
    // Query available space
    int32_t getAvailableForWrite() const;
    int32_t getAvailableForRead() const;
    
    // Write/Read
    int32_t write(const float* src, int32_t numFrames);
    int32_t read(float* dest, int32_t numFrames);
    
    // Peek without consuming
    int32_t peek(float* dest, int32_t numFrames) const;
    
    // Skip frames
    void skip(int32_t numFrames);
    
    // Reset
    void reset();
    
    // Total capacity
    int32_t getCapacity() const { return m_capacity; }
    
private:
    std::vector<float> m_buffer;
    int32_t m_capacity;
    int32_t m_numChannels;
    int32_t m_writePos = 0;
    int32_t m_readPos = 0;
    int32_t m_availableFrames = 0;
};

/**
 * Double buffer for non-blocking audio
 */
class DoubleBuffer {
public:
    DoubleBuffer(int32_t bufferSize, int32_t numChannels);
    ~DoubleBuffer() = default;
    
    // Get current read buffer
    const AudioBuffer& getReadBuffer() const;
    
    // Get current write buffer
    AudioBuffer& getWriteBuffer();
    
    // Swap buffers
    void swap();
    
    // Check if write buffer is ready
    bool isWriteReady() const { return m_writeReady; }
    void setWriteReady(bool ready) { m_writeReady = ready; }
    
private:
    AudioBuffer m_buffers[2];
    int32_t m_readIndex = 0;
    bool m_writeReady = false;
};

// ============================================================================
// Inline Implementations
// ============================================================================

inline AudioBuffer::AudioBuffer(int32_t numFrames, int32_t numChannels)
    : m_numFrames(numFrames)
    , m_numChannels(numChannels)
{
    m_data.resize(numFrames * numChannels, 0.0f);
}

inline float* AudioBuffer::getWritePointer(int32_t channel) {
    // Note: Data is interleaved, so we return pointer to first sample of channel
    // Caller should use stride of m_numChannels
    return m_data.data() + channel;
}

inline const float* AudioBuffer::getReadPointer(int32_t channel) const {
    return m_data.data() + channel;
}

inline float* AudioBuffer::getInterleavedWritePointer() {
    return m_data.data();
}

inline const float* AudioBuffer::getInterleavedReadPointer() const {
    return m_data.data();
}

inline void AudioBuffer::setSize(int32_t numFrames, int32_t numChannels) {
    m_numFrames = numFrames;
    m_numChannels = numChannels;
    m_data.resize(numFrames * numChannels);
}

inline void AudioBuffer::clear() {
    std::fill(m_data.begin(), m_data.end(), 0.0f);
}

inline void AudioBuffer::clear(int32_t startFrame, int32_t numFrames) {
    int32_t startIdx = startFrame * m_numChannels;
    int32_t count = numFrames * m_numChannels;
    std::fill(m_data.begin() + startIdx, m_data.begin() + startIdx + count, 0.0f);
}

inline void AudioBuffer::fill(float value) {
    std::fill(m_data.begin(), m_data.end(), value);
}

inline void AudioBuffer::applyGain(float gain) {
    applyGainSIMD(m_data.data(), static_cast<int32_t>(m_data.size()), gain);
}

inline void AudioBuffer::applyGainSIMD(float* data, int32_t numSamples, float gain) {
#if USE_NEON
    int32_t i = 0;
    float32x4_t gainVec = vdupq_n_f32(gain);
    
    for (; i + 4 <= numSamples; i += 4) {
        float32x4_t samples = vld1q_f32(&data[i]);
        samples = vmulq_f32(samples, gainVec);
        vst1q_f32(&data[i], samples);
    }
    
    // Handle remaining samples
    for (; i < numSamples; ++i) {
        data[i] *= gain;
    }
#else
    for (int32_t i = 0; i < numSamples; ++i) {
        data[i] *= gain;
    }
#endif
}

inline void AudioBuffer::addWithGainSIMD(float* dest, const float* src, 
                                         int32_t numSamples, float gain) {
#if USE_NEON
    int32_t i = 0;
    float32x4_t gainVec = vdupq_n_f32(gain);
    
    for (; i + 4 <= numSamples; i += 4) {
        float32x4_t d = vld1q_f32(&dest[i]);
        float32x4_t s = vld1q_f32(&src[i]);
        d = vmlaq_f32(d, s, gainVec);  // d = d + s * gain
        vst1q_f32(&dest[i], d);
    }
    
    for (; i < numSamples; ++i) {
        dest[i] += src[i] * gain;
    }
#else
    for (int32_t i = 0; i < numSamples; ++i) {
        dest[i] += src[i] * gain;
    }
#endif
}

inline float AudioBuffer::getPeakSIMD(const float* data, int32_t numSamples) const {
#if USE_NEON
    float32x4_t maxVec = vdupq_n_f32(0.0f);
    int32_t i = 0;
    
    for (; i + 4 <= numSamples; i += 4) {
        float32x4_t samples = vld1q_f32(&data[i]);
        samples = vabsq_f32(samples);
        maxVec = vmaxq_f32(maxVec, samples);
    }
    
    // Reduce to single value
    float32x2_t max2 = vpmax_f32(vget_low_f32(maxVec), vget_high_f32(maxVec));
    max2 = vpmax_f32(max2, max2);
    float peak = vget_lane_f32(max2, 0);
    
    // Handle remaining samples
    for (; i < numSamples; ++i) {
        peak = std::max(peak, std::abs(data[i]));
    }
    
    return peak;
#else
    float peak = 0.0f;
    for (int32_t i = 0; i < numSamples; ++i) {
        peak = std::max(peak, std::abs(data[i]));
    }
    return peak;
#endif
}

inline float AudioBuffer::getPeak() const {
    return getPeakSIMD(m_data.data(), static_cast<int32_t>(m_data.size()));
}

inline float AudioBuffer::getRMS() const {
    float sum = 0.0f;
    for (float sample : m_data) {
        sum += sample * sample;
    }
    return std::sqrt(sum / m_data.size());
}

inline bool AudioBuffer::isSilent(float threshold) const {
    return getPeak() < threshold;
}

inline void AudioBuffer::normalize(float targetPeak) {
    float currentPeak = getPeak();
    if (currentPeak > 1e-6f) {
        applyGain(targetPeak / currentPeak);
    }
}

inline void AudioBuffer::fadeIn(int32_t numFrames) {
    numFrames = std::min(numFrames, m_numFrames);
    for (int32_t i = 0; i < numFrames; ++i) {
        float gain = static_cast<float>(i) / numFrames;
        for (int32_t ch = 0; ch < m_numChannels; ++ch) {
            m_data[i * m_numChannels + ch] *= gain;
        }
    }
}

inline void AudioBuffer::fadeOut(int32_t numFrames) {
    numFrames = std::min(numFrames, m_numFrames);
    int32_t startFrame = m_numFrames - numFrames;
    for (int32_t i = 0; i < numFrames; ++i) {
        float gain = 1.0f - static_cast<float>(i) / numFrames;
        int32_t frameIdx = startFrame + i;
        for (int32_t ch = 0; ch < m_numChannels; ++ch) {
            m_data[frameIdx * m_numChannels + ch] *= gain;
        }
    }
}

} // namespace ultramusic

#endif // ULTRA_MUSIC_AUDIO_BUFFER_H
