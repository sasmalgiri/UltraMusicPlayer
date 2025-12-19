/**
 * UltraMusicPlayer - PhaseVocoder.h
 * 
 * High-quality Phase Vocoder implementation for time stretching
 * and pitch shifting with minimal artifacts.
 * 
 * This is the heart of our audio quality - uses advanced techniques:
 * - Overlapping windows with Hann/Blackman functions
 * - Phase unwrapping and phase locking
 * - Transient detection and preservation
 * - Multi-resolution analysis for better bass response
 * - Vertical phase coherence for polyphonic material
 */

#ifndef ULTRA_MUSIC_PHASE_VOCODER_H
#define ULTRA_MUSIC_PHASE_VOCODER_H

#include <vector>
#include <complex>
#include <memory>
#include <cmath>

namespace ultramusic {

// FFT window types for different quality/performance tradeoffs
enum class WindowType {
    HANN,           // Standard, good all-around
    BLACKMAN,       // Better frequency resolution
    BLACKMAN_HARRIS,// Excellent side lobe rejection
    KAISER,         // Adjustable tradeoff
    GAUSSIAN        // Smooth, good for vocals
};

// Phase vocoder configuration
struct PhaseVocoderConfig {
    int32_t fftSize = 4096;         // FFT size (power of 2)
    int32_t hopSize = 1024;         // Hop size (typically fftSize/4)
    int32_t oversampling = 4;       // Oversampling factor
    WindowType windowType = WindowType::HANN;
    bool phaseLocking = true;       // Enable phase locking for better quality
    bool transientDetection = true; // Detect and preserve transients
    bool verticalCoherence = true;  // Maintain phase coherence across channels
    float transientThreshold = 1.5f;// Threshold for transient detection
};

/**
 * PhaseVocoder class
 * 
 * Implements STFT-based time stretching and pitch shifting
 * with multiple quality enhancement techniques.
 */
class PhaseVocoder {
public:
    PhaseVocoder(int32_t sampleRate, int32_t channels);
    ~PhaseVocoder();
    
    // Configuration
    void configure(const PhaseVocoderConfig& config);
    void setTimeStretchRatio(float ratio);  // 0.1 to 10.0
    void setPitchShiftRatio(float ratio);   // 0.25 to 4.0
    
    // Processing
    void process(const float* input, float* output, int32_t numFrames);
    void reset();
    
    // Get latency introduced by processing
    int32_t getLatencyFrames() const;
    
    // Analysis functions
    bool isTransient() const { return m_isTransient; }
    float getSpectralFlux() const { return m_spectralFlux; }
    
private:
    // Sample rate and channels
    int32_t m_sampleRate;
    int32_t m_channelCount;
    
    // Configuration
    PhaseVocoderConfig m_config;
    float m_timeStretchRatio = 1.0f;
    float m_pitchShiftRatio = 1.0f;
    
    // FFT buffers
    std::vector<float> m_window;
    std::vector<float> m_inputBuffer;
    std::vector<float> m_outputBuffer;
    std::vector<std::complex<float>> m_fftBuffer;
    std::vector<std::complex<float>> m_lastPhase;
    std::vector<std::complex<float>> m_sumPhase;
    std::vector<float> m_magnitude;
    std::vector<float> m_frequency;
    
    // Analysis buffers
    std::vector<float> m_prevMagnitude;
    float m_spectralFlux = 0.0f;
    bool m_isTransient = false;
    
    // Position tracking
    int64_t m_inputPosition = 0;
    double m_outputPosition = 0.0;
    
    // Internal methods
    void createWindow();
    void performFFT(const float* input, std::complex<float>* output);
    void performIFFT(const std::complex<float>* input, float* output);
    void analyzeFrame(const float* frame);
    void synthesizeFrame(float* frame);
    void detectTransient();
    void applyPhaseLocking();
    
    // Optimized FFT implementation
    class FFTProcessor;
    std::unique_ptr<FFTProcessor> m_fft;
};

/**
 * Multi-resolution Phase Vocoder
 * 
 * Uses different FFT sizes for different frequency bands:
 * - Large FFT for bass (better frequency resolution)
 * - Medium FFT for mids
 * - Small FFT for highs (better time resolution)
 * 
 * This gives us the best of both worlds and exceeds simple phase vocoders.
 */
class MultiResolutionPhaseVocoder {
public:
    MultiResolutionPhaseVocoder(int32_t sampleRate, int32_t channels);
    ~MultiResolutionPhaseVocoder();
    
    void setTimeStretchRatio(float ratio);
    void setPitchShiftRatio(float ratio);
    void process(const float* input, float* output, int32_t numFrames);
    void reset();
    
private:
    // Three phase vocoders for different frequency bands
    std::unique_ptr<PhaseVocoder> m_lowBand;   // Bass: 8192 FFT
    std::unique_ptr<PhaseVocoder> m_midBand;   // Mids: 4096 FFT
    std::unique_ptr<PhaseVocoder> m_highBand;  // Highs: 2048 FFT
    
    // Crossover filters
    std::vector<float> m_lowpassBuffer;
    std::vector<float> m_bandpassBuffer;
    std::vector<float> m_highpassBuffer;
    
    // Crossover frequencies
    float m_lowCrossover = 250.0f;   // Hz
    float m_highCrossover = 4000.0f; // Hz
    
    int32_t m_sampleRate;
    int32_t m_channelCount;
    
    void splitBands(const float* input, int32_t numFrames);
    void combineBands(float* output, int32_t numFrames);
};

} // namespace ultramusic

#endif // ULTRA_MUSIC_PHASE_VOCODER_H
