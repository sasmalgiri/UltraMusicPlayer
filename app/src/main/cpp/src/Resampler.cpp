/**
 * UltraMusicPlayer - Resampler.cpp
 * 
 * Studio-quality sample rate conversion implementation.
 * Uses windowed sinc interpolation with Kaiser window.
 * 
 * This is essential for:
 * - Pitch shifting (resampling is part of the process)
 * - Sample rate conversion for different output devices
 * - Maintaining audio quality at all processing stages
 */

#include "../include/Resampler.h"
#include <cmath>
#include <algorithm>
#include <numeric>

namespace ultramusic {

static constexpr float PI = 3.14159265358979323846f;

// ============================================================================
// Resampler Implementation
// ============================================================================

Resampler::Resampler(int32_t inputSampleRate, int32_t outputSampleRate, int32_t channels)
    : m_inputSampleRate(inputSampleRate)
    , m_outputSampleRate(outputSampleRate)
    , m_channelCount(channels)
    , m_ratio(static_cast<double>(outputSampleRate) / inputSampleRate)
{
    setQuality(ResamplerQuality::HIGH);
}

Resampler::~Resampler() = default;

void Resampler::setQuality(ResamplerQuality quality) {
    m_quality = quality;
    
    // Set filter parameters based on quality
    switch (quality) {
        case ResamplerQuality::FAST:
            m_filterLength = 4;
            m_tableSize = 128;
            m_kaiserBeta = 4.0f;
            break;
            
        case ResamplerQuality::MEDIUM:
            m_filterLength = 16;
            m_tableSize = 256;
            m_kaiserBeta = 6.0f;
            break;
            
        case ResamplerQuality::HIGH:
            m_filterLength = 32;
            m_tableSize = 512;
            m_kaiserBeta = 8.0f;
            break;
            
        case ResamplerQuality::ULTRA:
            m_filterLength = 64;
            m_tableSize = 1024;
            m_kaiserBeta = 10.0f;
            break;
            
        case ResamplerQuality::MASTERING:
            m_filterLength = 128;
            m_tableSize = 2048;
            m_kaiserBeta = 12.0f;
            break;
    }
    
    // Reinitialize filter tables
    initSincTable();
    
    // Resize input buffer for filter overlap
    m_inputBuffer.resize(m_filterLength * 2 * m_channelCount, 0.0f);
    m_inputBufferSize = 0;
}

void Resampler::setRatio(double ratio) {
    m_ratio = ratio;
}

void Resampler::initSincTable() {
    // Create windowed sinc filter table
    // Table stores filter values for fractional delays from 0 to 1
    
    int32_t fullTableSize = m_tableSize * (m_filterLength * 2 + 1);
    m_sincTable.resize(fullTableSize);
    m_diffTable.resize(fullTableSize);
    
    float invTableSize = 1.0f / m_tableSize;
    
    for (int32_t t = 0; t < m_tableSize; ++t) {
        float frac = t * invTableSize;  // Fractional delay 0.0 to 1.0
        
        // Calculate filter coefficients for this fractional delay
        for (int32_t i = -m_filterLength; i <= m_filterLength; ++i) {
            float x = i - frac;
            
            // Windowed sinc
            float sincVal = sinc(x);
            float kaiserVal = kaiser(x / m_filterLength, m_kaiserBeta);
            float filterVal = sincVal * kaiserVal;
            
            int32_t idx = t * (m_filterLength * 2 + 1) + (i + m_filterLength);
            m_sincTable[idx] = filterVal;
        }
    }
    
    // Compute difference table for linear interpolation between table entries
    for (int32_t t = 0; t < m_tableSize - 1; ++t) {
        for (int32_t i = 0; i < m_filterLength * 2 + 1; ++i) {
            int32_t idx = t * (m_filterLength * 2 + 1) + i;
            m_diffTable[idx] = m_sincTable[idx + (m_filterLength * 2 + 1)] - m_sincTable[idx];
        }
    }
    // Last entry diff is zero
    for (int32_t i = 0; i < m_filterLength * 2 + 1; ++i) {
        int32_t idx = (m_tableSize - 1) * (m_filterLength * 2 + 1) + i;
        m_diffTable[idx] = 0.0f;
    }
}

float Resampler::kaiser(float x, float beta) const {
    if (std::abs(x) > 1.0f) return 0.0f;
    
    float arg = beta * std::sqrt(1.0f - x * x);
    return bessel_i0(arg) / bessel_i0(beta);
}

float Resampler::bessel_i0(float x) const {
    // Modified Bessel function I0 using polynomial approximation
    float ax = std::abs(x);
    
    if (ax < 3.75f) {
        float t = x / 3.75f;
        t = t * t;
        return 1.0f + t * (3.5156229f + t * (3.0899424f + t * (1.2067492f
            + t * (0.2659732f + t * (0.0360768f + t * 0.0045813f)))));
    } else {
        float t = 3.75f / ax;
        return (std::exp(ax) / std::sqrt(ax)) * (0.39894228f + t * (0.01328592f
            + t * (0.00225319f + t * (-0.00157565f + t * (0.00916281f
            + t * (-0.02057706f + t * (0.02635537f + t * (-0.01647633f
            + t * 0.00392377f))))))));
    }
}

float Resampler::sinc(float x) const {
    if (std::abs(x) < 1e-6f) return 1.0f;
    float pix = PI * x;
    return std::sin(pix) / pix;
}

float Resampler::getFilterValue(float fractionalDelay) const {
    // Get interpolated filter value from table
    float tablePos = fractionalDelay * m_tableSize;
    int32_t tableIdx = static_cast<int32_t>(tablePos);
    float tableFrac = tablePos - tableIdx;
    
    tableIdx = std::clamp(tableIdx, 0, m_tableSize - 1);
    
    // Interpolate between table entries
    // (This gives sub-sample accuracy for the filter)
    return m_sincTable[tableIdx] + tableFrac * m_diffTable[tableIdx];
}

int32_t Resampler::process(const float* input, int32_t inputFrames,
                          float* output, int32_t maxOutputFrames) {
    
    // Fast path for unity ratio
    if (std::abs(m_ratio - 1.0) < 1e-6) {
        int32_t framesToCopy = std::min(inputFrames, maxOutputFrames);
        std::copy(input, input + framesToCopy * m_channelCount, output);
        return framesToCopy;
    }
    
    // Fast path for simple linear interpolation (FAST quality)
    if (m_quality == ResamplerQuality::FAST) {
        int32_t outputFrames = 0;
        
        while (m_position < inputFrames - 1 && outputFrames < maxOutputFrames) {
            int32_t pos0 = static_cast<int32_t>(m_position);
            float frac = static_cast<float>(m_position - pos0);
            
            for (int32_t ch = 0; ch < m_channelCount; ++ch) {
                float s0 = input[pos0 * m_channelCount + ch];
                float s1 = input[(pos0 + 1) * m_channelCount + ch];
                output[outputFrames * m_channelCount + ch] = s0 + frac * (s1 - s0);
            }
            
            m_position += 1.0 / m_ratio;
            outputFrames++;
        }
        
        m_position -= inputFrames;
        return outputFrames;
    }
    
    // High-quality windowed sinc interpolation
    int32_t outputFrames = 0;
    int32_t filterWidth = m_filterLength * 2 + 1;
    
    while (outputFrames < maxOutputFrames) {
        double inputPos = m_position;
        
        // Check if we have enough input samples
        int32_t intPos = static_cast<int32_t>(inputPos);
        if (intPos - m_filterLength < 0 || intPos + m_filterLength >= inputFrames) {
            break;  // Need more input
        }
        
        float frac = static_cast<float>(inputPos - intPos);
        
        // Get table index for fractional delay
        float tablePos = frac * m_tableSize;
        int32_t tableIdx = static_cast<int32_t>(tablePos);
        float tableFrac = tablePos - tableIdx;
        tableIdx = std::clamp(tableIdx, 0, m_tableSize - 1);
        
        // Apply filter for each channel
        for (int32_t ch = 0; ch < m_channelCount; ++ch) {
            float sum = 0.0f;
            
            const float* sincPtr = &m_sincTable[tableIdx * filterWidth];
            const float* diffPtr = &m_diffTable[tableIdx * filterWidth];
            
            for (int32_t i = -m_filterLength; i <= m_filterLength; ++i) {
                int32_t srcIdx = (intPos + i) * m_channelCount + ch;
                int32_t filterIdx = i + m_filterLength;
                
                // Linear interpolation between table entries
                float filterVal = sincPtr[filterIdx] + tableFrac * diffPtr[filterIdx];
                sum += input[srcIdx] * filterVal;
            }
            
            output[outputFrames * m_channelCount + ch] = sum;
        }
        
        m_position += 1.0 / m_ratio;
        outputFrames++;
    }
    
    // Adjust position for consumed input
    m_position -= inputFrames;
    
    return outputFrames;
}

int32_t Resampler::getExpectedOutputFrames(int32_t inputFrames) const {
    return static_cast<int32_t>(std::ceil(inputFrames * m_ratio));
}

int32_t Resampler::getRequiredInputFrames(int32_t outputFrames) const {
    return static_cast<int32_t>(std::ceil(outputFrames / m_ratio)) + m_filterLength * 2;
}

void Resampler::reset() {
    m_position = 0.0;
    std::fill(m_inputBuffer.begin(), m_inputBuffer.end(), 0.0f);
    m_inputBufferSize = 0;
}

int32_t Resampler::getLatency() const {
    return m_filterLength;
}

// ============================================================================
// Oversampler Implementation
// ============================================================================

Oversampler::Oversampler(int32_t sampleRate, int32_t channels, int32_t factor)
    : m_sampleRate(sampleRate)
    , m_channelCount(channels)
    , m_factor(factor)
{
    initFilters();
}

Oversampler::~Oversampler() = default;

void Oversampler::initFilters() {
    // Design half-band filter for upsampling/downsampling
    // Using 31-tap FIR filter for good quality
    
    const int32_t filterLength = 31;
    m_upsampleFilter.resize(filterLength);
    m_downsampleFilter.resize(filterLength);
    
    // Generate windowed sinc filter coefficients
    float cutoff = 0.45f;  // Slightly less than 0.5 for good attenuation
    float sum = 0.0f;
    
    for (int32_t i = 0; i < filterLength; ++i) {
        float n = i - (filterLength - 1) / 2.0f;
        
        // Sinc function
        float h;
        if (std::abs(n) < 1e-6f) {
            h = 2.0f * cutoff;
        } else {
            h = std::sin(2.0f * PI * cutoff * n) / (PI * n);
        }
        
        // Kaiser window (beta = 8 for good stopband attenuation)
        float x = 2.0f * i / (filterLength - 1) - 1.0f;
        float beta = 8.0f;
        float window;
        if (std::abs(x) <= 1.0f) {
            float arg = beta * std::sqrt(1.0f - x * x);
            // Simplified I0 approximation
            float t = arg / 3.75f;
            t = t * t;
            window = 1.0f + t * (3.5156229f + t * 3.0899424f);
            window /= 1.0f + 8.0f * (3.5156229f + 8.0f * 3.0899424f);  // Normalize
        } else {
            window = 0.0f;
        }
        
        m_upsampleFilter[i] = h * window;
        sum += m_upsampleFilter[i];
    }
    
    // Normalize
    for (int32_t i = 0; i < filterLength; ++i) {
        m_upsampleFilter[i] /= sum;
        m_upsampleFilter[i] *= m_factor;  // Compensate for zero-stuffing
        m_downsampleFilter[i] = m_upsampleFilter[i] / m_factor;  // Gain adjustment
    }
    
    // Initialize filter state
    m_upsampleState.resize(filterLength * m_channelCount, 0.0f);
    m_downsampleState.resize(filterLength * m_channelCount, 0.0f);
}

void Oversampler::upsample(const float* input, int32_t inputFrames,
                           float* output, int32_t& outputFrames) {
    
    outputFrames = inputFrames * m_factor;
    int32_t filterLength = static_cast<int32_t>(m_upsampleFilter.size());
    int32_t halfFilter = filterLength / 2;
    
    // Zero-stuff and filter
    for (int32_t i = 0; i < inputFrames; ++i) {
        for (int32_t f = 0; f < m_factor; ++f) {
            int32_t outIdx = i * m_factor + f;
            
            for (int32_t ch = 0; ch < m_channelCount; ++ch) {
                float sum = 0.0f;
                
                // Polyphase implementation
                for (int32_t k = 0; k < filterLength; ++k) {
                    int32_t inputIdx = i - halfFilter + k / m_factor;
                    if ((k % m_factor) == f && inputIdx >= 0 && inputIdx < inputFrames) {
                        sum += input[inputIdx * m_channelCount + ch] * m_upsampleFilter[k];
                    }
                }
                
                output[outIdx * m_channelCount + ch] = sum;
            }
        }
    }
}

void Oversampler::downsample(const float* input, int32_t inputFrames,
                             float* output, int32_t& outputFrames) {
    
    outputFrames = inputFrames / m_factor;
    int32_t filterLength = static_cast<int32_t>(m_downsampleFilter.size());
    int32_t halfFilter = filterLength / 2;
    
    // Filter and decimate
    for (int32_t i = 0; i < outputFrames; ++i) {
        int32_t inIdx = i * m_factor;
        
        for (int32_t ch = 0; ch < m_channelCount; ++ch) {
            float sum = 0.0f;
            
            for (int32_t k = 0; k < filterLength; ++k) {
                int32_t srcIdx = inIdx - halfFilter + k;
                if (srcIdx >= 0 && srcIdx < inputFrames) {
                    sum += input[srcIdx * m_channelCount + ch] * m_downsampleFilter[k];
                }
            }
            
            output[i * m_channelCount + ch] = sum;
        }
    }
}

// ============================================================================
// AntiAliasingFilter Implementation
// ============================================================================

AntiAliasingFilter::AntiAliasingFilter(int32_t sampleRate, int32_t channels, float cutoffRatio)
    : m_sampleRate(sampleRate)
    , m_channelCount(channels)
    , m_cutoffRatio(cutoffRatio)
{
    m_states.resize(NUM_STAGES * channels);
    m_coeffs.resize(NUM_STAGES);
    calculateCoeffs();
}

AntiAliasingFilter::~AntiAliasingFilter() = default;

void AntiAliasingFilter::setCutoff(float ratio) {
    m_cutoffRatio = std::clamp(ratio, 0.01f, 0.99f);
    calculateCoeffs();
}

void AntiAliasingFilter::calculateCoeffs() {
    // Design 8th-order Butterworth lowpass filter as cascade of 2nd-order sections
    float fc = m_cutoffRatio * 0.5f;  // Normalized frequency
    float omega = std::tan(PI * fc);
    float omega2 = omega * omega;
    
    // Butterworth poles for 8th order
    // Q values for each stage
    float Q[4] = { 0.5098f, 0.6013f, 0.9000f, 2.5629f };
    
    for (int32_t stage = 0; stage < NUM_STAGES; ++stage) {
        float alpha = omega / Q[stage];
        float norm = 1.0f / (1.0f + alpha + omega2);
        
        m_coeffs[stage].b0 = omega2 * norm;
        m_coeffs[stage].b1 = 2.0f * omega2 * norm;
        m_coeffs[stage].b2 = omega2 * norm;
        m_coeffs[stage].a1 = 2.0f * (omega2 - 1.0f) * norm;
        m_coeffs[stage].a2 = (1.0f - alpha + omega2) * norm;
    }
}

void AntiAliasingFilter::process(float* buffer, int32_t numFrames) {
    for (int32_t ch = 0; ch < m_channelCount; ++ch) {
        for (int32_t stage = 0; stage < NUM_STAGES; ++stage) {
            BiquadState& state = m_states[stage * m_channelCount + ch];
            const BiquadCoeffs& coeffs = m_coeffs[stage];
            
            for (int32_t i = 0; i < numFrames; ++i) {
                float x = buffer[i * m_channelCount + ch];
                
                // Direct Form II Transposed
                float y = coeffs.b0 * x + state.x1;
                state.x1 = coeffs.b1 * x - coeffs.a1 * y + state.x2;
                state.x2 = coeffs.b2 * x - coeffs.a2 * y;
                
                buffer[i * m_channelCount + ch] = y;
            }
        }
    }
}

void AntiAliasingFilter::reset() {
    for (auto& state : m_states) {
        state.x1 = state.x2 = state.y1 = state.y2 = 0.0f;
    }
}

} // namespace ultramusic
