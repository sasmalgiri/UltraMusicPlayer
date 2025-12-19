/**
 * UltraMusicPlayer - Resampler.h
 * 
 * Studio-quality sample rate conversion using windowed sinc interpolation.
 * This is CRITICAL for maintaining audio quality during pitch shifting
 * and when the output sample rate differs from input.
 * 
 * Quality Features:
 * - Windowed sinc interpolation (best quality)
 * - Configurable filter length for quality/CPU tradeoff
 * - Anti-aliasing for downsampling
 * - Oversampling support for ultra-high quality
 */

#ifndef ULTRA_MUSIC_RESAMPLER_H
#define ULTRA_MUSIC_RESAMPLER_H

#include <vector>
#include <cstdint>
#include <cmath>

namespace ultramusic {

// Resampler quality levels
enum class ResamplerQuality {
    FAST,           // Linear interpolation - fast but low quality
    MEDIUM,         // 16-point sinc - good balance
    HIGH,           // 32-point sinc - high quality
    ULTRA,          // 64-point sinc - best quality
    MASTERING       // 128-point sinc - studio mastering quality
};

/**
 * High-quality Resampler using windowed sinc interpolation
 * 
 * Sinc interpolation is the theoretically perfect reconstruction
 * method, but requires infinite filter length. We use a windowed
 * version with Kaiser window for optimal stopband attenuation.
 */
class Resampler {
public:
    Resampler(int32_t inputSampleRate, int32_t outputSampleRate, int32_t channels);
    ~Resampler();
    
    // Set quality level
    void setQuality(ResamplerQuality quality);
    
    // Set resampling ratio directly (for pitch shifting)
    void setRatio(double ratio);
    
    // Get current ratio
    double getRatio() const { return m_ratio; }
    
    // Process audio
    // Returns number of output frames produced
    int32_t process(const float* input, int32_t inputFrames,
                   float* output, int32_t maxOutputFrames);
    
    // Get expected output frames for given input frames
    int32_t getExpectedOutputFrames(int32_t inputFrames) const;
    
    // Get required input frames to produce given output frames
    int32_t getRequiredInputFrames(int32_t outputFrames) const;
    
    // Reset internal state (call when seeking)
    void reset();
    
    // Get latency in samples
    int32_t getLatency() const;
    
private:
    int32_t m_inputSampleRate;
    int32_t m_outputSampleRate;
    int32_t m_channelCount;
    double m_ratio;
    
    // Filter parameters
    int32_t m_filterLength = 32;  // Number of taps on each side
    float m_kaiserBeta = 8.0f;    // Kaiser window parameter
    
    // Precomputed sinc filter tables
    int32_t m_tableSize = 512;    // Interpolation table resolution
    std::vector<float> m_sincTable;
    std::vector<float> m_diffTable;  // For linear interpolation between table entries
    
    // Input buffer for filter overlap
    std::vector<float> m_inputBuffer;
    int32_t m_inputBufferSize = 0;
    
    // Fractional position tracking
    double m_position = 0.0;
    
    // Quality setting
    ResamplerQuality m_quality = ResamplerQuality::HIGH;
    
    // Initialize filter tables
    void initSincTable();
    
    // Kaiser window function
    float kaiser(float x, float beta) const;
    
    // Modified Bessel function I0
    float bessel_i0(float x) const;
    
    // Sinc function
    float sinc(float x) const;
    
    // Get interpolated filter value
    float getFilterValue(float fractionalDelay) const;
    
    // Process single output sample
    float processSample(const float* input, int32_t inputFrames, 
                       int32_t channel, double position) const;
};

/**
 * Oversampling processor for ultra-high quality
 * 
 * Oversampling helps with:
 * - Reducing aliasing in nonlinear processing
 * - Better frequency resolution for pitch shifting
 * - Smoother time stretching at extreme ratios
 */
class Oversampler {
public:
    Oversampler(int32_t sampleRate, int32_t channels, int32_t factor = 4);
    ~Oversampler();
    
    // Upsample input
    void upsample(const float* input, int32_t inputFrames,
                  float* output, int32_t& outputFrames);
    
    // Downsample back to original rate
    void downsample(const float* input, int32_t inputFrames,
                    float* output, int32_t& outputFrames);
    
    // Get oversampling factor
    int32_t getFactor() const { return m_factor; }
    
    // Get oversampled sample rate
    int32_t getOversampledRate() const { return m_sampleRate * m_factor; }
    
private:
    int32_t m_sampleRate;
    int32_t m_channelCount;
    int32_t m_factor;
    
    // Upsampling/downsampling filters
    std::vector<float> m_upsampleFilter;
    std::vector<float> m_downsampleFilter;
    
    // Filter state
    std::vector<float> m_upsampleState;
    std::vector<float> m_downsampleState;
    
    void initFilters();
};

/**
 * Anti-aliasing filter for downsampling
 * 
 * Uses a steep low-pass filter before decimation
 * to prevent aliasing artifacts.
 */
class AntiAliasingFilter {
public:
    AntiAliasingFilter(int32_t sampleRate, int32_t channels, float cutoffRatio = 0.45f);
    ~AntiAliasingFilter();
    
    // Set cutoff as ratio of Nyquist (0.0 - 1.0)
    void setCutoff(float ratio);
    
    // Process audio
    void process(float* buffer, int32_t numFrames);
    
    // Reset filter state
    void reset();
    
private:
    int32_t m_sampleRate;
    int32_t m_channelCount;
    float m_cutoffRatio;
    
    // Biquad filter cascade for steep rolloff
    struct BiquadState {
        float x1 = 0, x2 = 0;
        float y1 = 0, y2 = 0;
    };
    
    struct BiquadCoeffs {
        float b0, b1, b2;
        float a1, a2;
    };
    
    static constexpr int NUM_STAGES = 4;  // 8th order filter
    std::vector<BiquadState> m_states;
    std::vector<BiquadCoeffs> m_coeffs;
    
    void calculateCoeffs();
};

} // namespace ultramusic

#endif // ULTRA_MUSIC_RESAMPLER_H
