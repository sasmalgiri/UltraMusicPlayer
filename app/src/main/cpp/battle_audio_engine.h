/**
 * BATTLE AUDIO ENGINE - Header
 * 
 * Professional audio processing for sound system battles.
 */

#ifndef BATTLE_AUDIO_ENGINE_H
#define BATTLE_AUDIO_ENGINE_H

#include <cstdint>
#include <vector>
#include <cmath>

namespace ultramusic {

// =============================================================================
// BATTLE LIMITER - Prevents clipping at extreme volumes
// =============================================================================

class BattleLimiter {
public:
    BattleLimiter() = default;
    
    void configure(int sampleRate, int channels) {
        this->sampleRate = sampleRate;
        this->channels = channels;
        
        // Attack/release in samples
        attackSamples = static_cast<int>(attackMs * sampleRate / 1000.0f);
        releaseSamples = static_cast<int>(releaseMs * sampleRate / 1000.0f);
        
        // Lookahead buffer
        lookaheadSamples = static_cast<int>(lookaheadMs * sampleRate / 1000.0f);
        lookaheadBuffer.resize(lookaheadSamples * channels, 0.0f);
        lookaheadIndex = 0;
        
        reset();
    }
    
    void setEnabled(bool enabled) { this->enabled = enabled; }
    void setThreshold(float thresholdDb) { 
        this->thresholdDb = thresholdDb;
        threshold = std::pow(10.0f, thresholdDb / 20.0f);
    }
    void setCeiling(float ceilingDb) {
        this->ceilingDb = ceilingDb;
        ceiling = std::pow(10.0f, ceilingDb / 20.0f);
    }
    
    void process(float* samples, int numSamples) {
        if (!enabled) return;
        
        for (int i = 0; i < numSamples; i += channels) {
            // Find peak in this frame
            float peak = 0.0f;
            for (int ch = 0; ch < channels; ch++) {
                peak = std::max(peak, std::abs(samples[i + ch]));
            }
            
            // Calculate gain reduction
            float targetGain = 1.0f;
            if (peak > threshold) {
                targetGain = threshold / peak;
            }
            
            // Smooth gain changes
            if (targetGain < currentGain) {
                // Attack - fast
                currentGain = currentGain + (targetGain - currentGain) * attackCoeff;
            } else {
                // Release - slow
                currentGain = currentGain + (targetGain - currentGain) * releaseCoeff;
            }
            
            // Apply gain with soft knee
            float gain = std::min(currentGain, 1.0f);
            for (int ch = 0; ch < channels; ch++) {
                samples[i + ch] *= gain;
                
                // Hard clip at ceiling (safety)
                samples[i + ch] = std::clamp(samples[i + ch], -ceiling, ceiling);
            }
        }
    }
    
    void reset() {
        currentGain = 1.0f;
        attackCoeff = 1.0f - std::exp(-2.2f / attackSamples);
        releaseCoeff = 1.0f - std::exp(-2.2f / releaseSamples);
    }
    
private:
    bool enabled = true;
    int sampleRate = 44100;
    int channels = 2;
    
    float thresholdDb = -0.3f;  // Limiter threshold
    float ceilingDb = -0.1f;    // Hard ceiling
    float threshold = 0.966f;
    float ceiling = 0.989f;
    
    float attackMs = 0.5f;      // Very fast attack
    float releaseMs = 100.0f;   // Smooth release
    float lookaheadMs = 1.5f;   // Lookahead for true peak limiting
    
    int attackSamples = 22;
    int releaseSamples = 4410;
    int lookaheadSamples = 66;
    
    float attackCoeff = 0.1f;
    float releaseCoeff = 0.001f;
    float currentGain = 1.0f;
    
    std::vector<float> lookaheadBuffer;
    int lookaheadIndex = 0;
};

// =============================================================================
// BATTLE COMPRESSOR - Adds punch and presence
// =============================================================================

class BattleCompressor {
public:
    BattleCompressor() = default;
    
    void configure(int sampleRate, int channels) {
        this->sampleRate = sampleRate;
        this->channels = channels;
        reset();
    }
    
    void setEnabled(bool enabled) { this->enabled = enabled; }
    void setThreshold(float thresholdDb) { 
        this->thresholdDb = thresholdDb;
        threshold = std::pow(10.0f, thresholdDb / 20.0f);
    }
    void setRatio(float ratio) { this->ratio = std::max(1.0f, ratio); }
    void setAttack(float attackMs) { this->attackMs = attackMs; }
    void setRelease(float releaseMs) { this->releaseMs = releaseMs; }
    void setMakeupGain(float gainDb) { 
        this->makeupGainDb = gainDb;
        makeupGain = std::pow(10.0f, gainDb / 20.0f);
    }
    
    void process(float* samples, int numSamples) {
        if (!enabled) return;
        
        float attackCoeff = std::exp(-1.0f / (attackMs * sampleRate / 1000.0f));
        float releaseCoeff = std::exp(-1.0f / (releaseMs * sampleRate / 1000.0f));
        
        for (int i = 0; i < numSamples; i += channels) {
            // Detect peak
            float peak = 0.0f;
            for (int ch = 0; ch < channels; ch++) {
                peak = std::max(peak, std::abs(samples[i + ch]));
            }
            
            // Envelope follower
            if (peak > envelope) {
                envelope = attackCoeff * envelope + (1.0f - attackCoeff) * peak;
            } else {
                envelope = releaseCoeff * envelope + (1.0f - releaseCoeff) * peak;
            }
            
            // Calculate gain reduction
            float gain = 1.0f;
            if (envelope > threshold) {
                float overDb = 20.0f * std::log10(envelope / threshold);
                float reducedDb = overDb / ratio;
                gain = std::pow(10.0f, (reducedDb - overDb) / 20.0f);
            }
            
            // Smooth gain
            currentGain = 0.9f * currentGain + 0.1f * gain;
            
            // Apply gain + makeup
            for (int ch = 0; ch < channels; ch++) {
                samples[i + ch] *= currentGain * makeupGain;
            }
        }
    }
    
    void reset() {
        envelope = 0.0f;
        currentGain = 1.0f;
    }
    
private:
    bool enabled = true;
    int sampleRate = 44100;
    int channels = 2;
    
    float thresholdDb = -12.0f;  // Compress above -12dB
    float threshold = 0.25f;
    float ratio = 4.0f;          // 4:1 compression
    float attackMs = 5.0f;       // Fast attack for punch
    float releaseMs = 100.0f;    // Medium release
    float makeupGainDb = 6.0f;   // +6dB makeup
    float makeupGain = 2.0f;
    
    float envelope = 0.0f;
    float currentGain = 1.0f;
};

// =============================================================================
// BATTLE BASS BOOST - Sub-bass enhancement for maximum impact
// =============================================================================

class BattleBassBoost {
public:
    BattleBassBoost() = default;
    
    void configure(int sampleRate, int channels) {
        this->sampleRate = sampleRate;
        this->channels = channels;
        
        // Calculate filter coefficients for low shelf at 80Hz
        calculateCoefficients();
        
        // Initialize filter states
        filterStates.resize(channels * 2, 0.0f);  // 2 states per channel (biquad)
        
        reset();
    }
    
    void setEnabled(bool enabled) { this->enabled = enabled; }
    void setGain(float gainDb) { 
        this->gainDb = std::clamp(gainDb, 0.0f, 24.0f);
        calculateCoefficients();
    }
    void setFrequency(float freq) { 
        this->frequency = std::clamp(freq, 20.0f, 200.0f);
        calculateCoefficients();
    }
    
    void process(float* samples, int numSamples) {
        if (!enabled || gainDb <= 0) return;
        
        for (int i = 0; i < numSamples; i += channels) {
            for (int ch = 0; ch < channels; ch++) {
                float input = samples[i + ch];
                
                // Biquad filter (low shelf)
                int stateIdx = ch * 2;
                float output = b0 * input + b1 * filterStates[stateIdx] + 
                              b2 * filterStates[stateIdx + 1];
                output -= a1 * filterStates[stateIdx] + a2 * filterStates[stateIdx + 1];
                
                // Update states
                filterStates[stateIdx + 1] = filterStates[stateIdx];
                filterStates[stateIdx] = input;
                
                samples[i + ch] = output;
            }
        }
    }
    
    void reset() {
        std::fill(filterStates.begin(), filterStates.end(), 0.0f);
    }
    
private:
    void calculateCoefficients() {
        // Low shelf filter design
        float A = std::pow(10.0f, gainDb / 40.0f);
        float w0 = 2.0f * M_PI * frequency / sampleRate;
        float cosW0 = std::cos(w0);
        float sinW0 = std::sin(w0);
        float alpha = sinW0 / 2.0f * std::sqrt((A + 1.0f / A) * (1.0f / 0.707f - 1.0f) + 2.0f);
        
        float a0 = (A + 1) + (A - 1) * cosW0 + 2 * std::sqrt(A) * alpha;
        
        b0 = (A * ((A + 1) - (A - 1) * cosW0 + 2 * std::sqrt(A) * alpha)) / a0;
        b1 = (2 * A * ((A - 1) - (A + 1) * cosW0)) / a0;
        b2 = (A * ((A + 1) - (A - 1) * cosW0 - 2 * std::sqrt(A) * alpha)) / a0;
        a1 = (-2 * ((A - 1) + (A + 1) * cosW0)) / a0;
        a2 = ((A + 1) + (A - 1) * cosW0 - 2 * std::sqrt(A) * alpha) / a0;
    }
    
    bool enabled = true;
    int sampleRate = 44100;
    int channels = 2;
    
    float gainDb = 6.0f;       // Default +6dB bass boost
    float frequency = 80.0f;   // Center frequency for boost
    
    // Biquad coefficients
    float b0 = 1.0f, b1 = 0.0f, b2 = 0.0f;
    float a1 = 0.0f, a2 = 0.0f;
    
    std::vector<float> filterStates;
};

} // namespace ultramusic

#endif // BATTLE_AUDIO_ENGINE_H
