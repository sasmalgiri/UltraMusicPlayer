/**
 * BATTLE COMPRESSOR Implementation
 * 
 * Multi-band compression optimized for sound system battles.
 * Adds punch in the mids and weight in the lows.
 */

#include "battle_audio_engine.h"

namespace ultramusic {

// Gain reduction calculation with soft knee
float calculateGainReduction(float inputDb, float threshold, float ratio, float knee) {
    // Below threshold
    if (inputDb < threshold - knee / 2) {
        return 0.0f;
    }
    
    // Above threshold + knee
    if (inputDb > threshold + knee / 2) {
        return (threshold - inputDb) * (1.0f - 1.0f / ratio);
    }
    
    // In knee region (smooth transition)
    float x = inputDb - threshold + knee / 2;
    return x * x / (2.0f * knee) * (1.0f - 1.0f / ratio);
}

// Convert linear to dB
float linearToDb(float linear) {
    if (linear <= 0.00001f) return -100.0f;
    return 20.0f * std::log10(linear);
}

// Convert dB to linear
float dbToLinear(float db) {
    return std::pow(10.0f, db / 20.0f);
}

// Parallel compression for more punch
void parallelCompress(float* samples, int numSamples, float wetDry) {
    // Heavy compression settings
    const float threshold = -20.0f;
    const float ratio = 8.0f;
    const float attack = 0.001f;
    const float release = 0.1f;
    const float makeupGain = 12.0f;  // Heavy makeup for NY compression
    
    static float envelope = 0.0f;
    
    for (int i = 0; i < numSamples; i++) {
        float input = samples[i];
        float absInput = std::abs(input);
        
        // Envelope detection
        float coeff = absInput > envelope ? attack : release;
        envelope = envelope + coeff * (absInput - envelope);
        
        // Gain calculation
        float inputDb = linearToDb(envelope);
        float gainReduction = calculateGainReduction(inputDb, threshold, ratio, 6.0f);
        float gain = dbToLinear(gainReduction + makeupGain);
        
        // Mix dry and wet
        float compressed = input * gain;
        samples[i] = input * (1.0f - wetDry) + compressed * wetDry;
    }
}

} // namespace ultramusic
