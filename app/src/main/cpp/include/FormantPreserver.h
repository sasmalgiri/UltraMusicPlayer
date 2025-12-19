/**
 * UltraMusicPlayer - FormantPreserver.h
 * 
 * Formant preservation for natural-sounding pitch shifts.
 * This is what prevents the "chipmunk" or "demon" effect when
 * shifting pitch, especially important for vocals.
 * 
 * Uses envelope estimation to separate:
 * 1. Spectral envelope (formants - vocal characteristics)
 * 2. Fine structure (pitch information)
 * 
 * Then we can shift pitch while keeping formants intact.
 */

#ifndef ULTRA_MUSIC_FORMANT_PRESERVER_H
#define ULTRA_MUSIC_FORMANT_PRESERVER_H

#include <vector>
#include <memory>
#include <cstdint>

namespace ultramusic {

// Forward declarations
class FFTProcessor;

// Envelope estimation method
enum class EnvelopeMethod {
    CEPSTRUM,           // Cepstral analysis - standard approach
    TRUE_ENVELOPE,      // True envelope estimation
    LPC,                // Linear Predictive Coding
    DISCRETE_CEPSTRUM   // Discrete cepstrum
};

/**
 * FormantPreserver class
 * 
 * Separates spectral envelope from fine structure to enable
 * pitch shifting without changing vocal characteristics.
 */
class FormantPreserver {
public:
    FormantPreserver(int32_t sampleRate, int32_t fftSize);
    ~FormantPreserver();
    
    // Configuration
    void setMethod(EnvelopeMethod method);
    void setEnvelopeOrder(int order);  // For LPC/cepstrum (typically 30-60)
    
    // Processing
    void processSpectrum(float* magnitudes, float* phases, 
                        float pitchShiftRatio, float formantShiftRatio);
    
    // Extract formants from spectrum
    void extractEnvelope(const float* magnitudes, float* envelope);
    
    // Apply envelope to spectrum
    void applyEnvelope(float* magnitudes, const float* envelope);
    
    // Formant analysis
    struct FormantInfo {
        float frequency;    // Hz
        float bandwidth;    // Hz
        float amplitude;    // dB
    };
    std::vector<FormantInfo> detectFormants(const float* magnitudes);
    
private:
    int32_t m_sampleRate;
    int32_t m_fftSize;
    int32_t m_envelopeOrder = 40;
    EnvelopeMethod m_method = EnvelopeMethod::TRUE_ENVELOPE;
    
    // FFT for cepstral analysis
    std::unique_ptr<FFTProcessor> m_fft;
    
    // Work buffers
    std::vector<float> m_logMagnitude;
    std::vector<float> m_cepstrum;
    std::vector<float> m_envelope;
    std::vector<float> m_fineStructure;
    
    // LPC coefficients
    std::vector<float> m_lpcCoeffs;
    
    // Methods for different envelope estimation
    void estimateCepstrum(const float* magnitudes, float* envelope);
    void estimateTrueEnvelope(const float* magnitudes, float* envelope);
    void estimateLPC(const float* magnitudes, float* envelope);
    
    // Shift envelope for formant shifting
    void shiftEnvelope(float* envelope, float shiftRatio);
};

/**
 * VocalEnhancer class
 * 
 * Additional processing for vocal clarity and presence.
 */
class VocalEnhancer {
public:
    VocalEnhancer(int32_t sampleRate);
    ~VocalEnhancer();
    
    // De-essing (reduce harsh 's' sounds)
    void setDeEsser(float threshold, float ratio);
    void enableDeEsser(bool enable);
    
    // Presence boost
    void setPresence(float amount);  // 0.0 to 1.0
    
    // Air (high frequency boost)
    void setAir(float amount);  // 0.0 to 1.0
    
    // Warmth (low-mid boost)
    void setWarmth(float amount);  // 0.0 to 1.0
    
    // Processing
    void process(float* buffer, int32_t numFrames);
    
private:
    int32_t m_sampleRate;
    
    bool m_deEsserEnabled = false;
    float m_deEsserThreshold = -20.0f;
    float m_deEsserRatio = 4.0f;
    
    float m_presence = 0.0f;
    float m_air = 0.0f;
    float m_warmth = 0.0f;
    
    // Filter states
    std::vector<float> m_filterStates;
};

} // namespace ultramusic

#endif // ULTRA_MUSIC_FORMANT_PRESERVER_H
