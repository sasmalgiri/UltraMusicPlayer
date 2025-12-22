/**
 * BATTLE BASS BOOST Implementation
 * 
 * Sub-bass enhancement with harmonic generation for maximum impact.
 * Designed to shake sound systems and rattle buildings.
 */

#include "battle_audio_engine.h"
#include <cmath>

namespace ultramusic {

// Sub-harmonic synthesizer - generates octave-below frequencies
class SubHarmonicSynthesizer {
public:
    void configure(int sampleRate) {
        this->sampleRate = sampleRate;
        
        // Low pass filter at 80Hz for sub detection
        float fc = 80.0f / sampleRate;
        float x = std::exp(-2.0f * M_PI * fc);
        lpCoeff = 1.0f - x;
        
        reset();
    }
    
    void setAmount(float amount) {
        this->amount = std::clamp(amount, 0.0f, 1.0f);
    }
    
    float process(float input) {
        // Low pass to isolate bass
        lpState = lpState + lpCoeff * (input - lpState);
        float bass = lpState;
        
        // Detect zero crossings for sub-harmonic generation
        bool currentPositive = bass > 0;
        if (currentPositive != lastPositive) {
            // Zero crossing - toggle sub oscillator
            subPhase = !subPhase;
            lastPositive = currentPositive;
        }
        
        // Generate sub-harmonic (half frequency square wave smoothed)
        float subOsc = subPhase ? 1.0f : -1.0f;
        
        // Low pass the square wave to make it more sine-like
        subLpState = subLpState + 0.01f * (subOsc - subLpState);
        
        // Mix sub with original, scaled by bass envelope
        float envelope = std::abs(bass);
        return input + subLpState * envelope * amount;
    }
    
    void reset() {
        lpState = 0;
        subLpState = 0;
        subPhase = false;
        lastPositive = false;
    }
    
private:
    int sampleRate = 44100;
    float amount = 0.3f;
    
    float lpCoeff = 0.01f;
    float lpState = 0;
    
    bool subPhase = false;
    bool lastPositive = false;
    float subLpState = 0;
};

// Harmonic exciter for bass presence
class BassExciter {
public:
    void configure(int sampleRate) {
        this->sampleRate = sampleRate;
        
        // Bandpass around 60-120Hz
        float fc = 90.0f / sampleRate;
        lpCoeff = 1.0f - std::exp(-2.0f * M_PI * fc);
        hpCoeff = 1.0f - std::exp(-2.0f * M_PI * 40.0f / sampleRate);
        
        reset();
    }
    
    void setAmount(float amount) {
        this->amount = std::clamp(amount, 0.0f, 1.0f);
    }
    
    float process(float input) {
        // Bandpass filter
        lpState = lpState + lpCoeff * (input - lpState);
        hpState = hpState + hpCoeff * (lpState - hpState);
        float band = lpState - hpState;
        
        // Soft saturation for harmonics
        float saturated = std::tanh(band * 3.0f) / 3.0f;
        
        // Add harmonics back
        return input + (saturated - band) * amount;
    }
    
    void reset() {
        lpState = 0;
        hpState = 0;
    }
    
private:
    int sampleRate = 44100;
    float amount = 0.5f;
    
    float lpCoeff = 0.01f;
    float hpCoeff = 0.005f;
    float lpState = 0;
    float hpState = 0;
};

// MEGA BASS - The ultimate bass enhancement
void megaBass(float* samples, int numSamples, int channels, int sampleRate, float intensity) {
    static SubHarmonicSynthesizer subSynth[2];
    static BassExciter exciter[2];
    static bool initialized = false;
    
    if (!initialized) {
        for (int ch = 0; ch < 2; ch++) {
            subSynth[ch].configure(sampleRate);
            exciter[ch].configure(sampleRate);
        }
        initialized = true;
    }
    
    for (int ch = 0; ch < channels && ch < 2; ch++) {
        subSynth[ch].setAmount(intensity * 0.3f);
        exciter[ch].setAmount(intensity * 0.5f);
    }
    
    for (int i = 0; i < numSamples; i += channels) {
        for (int ch = 0; ch < channels && ch < 2; ch++) {
            float sample = samples[i + ch];
            
            // Add sub-harmonics
            sample = subSynth[ch].process(sample);
            
            // Add harmonic excitement
            sample = exciter[ch].process(sample);
            
            samples[i + ch] = sample;
        }
    }
}

} // namespace ultramusic
