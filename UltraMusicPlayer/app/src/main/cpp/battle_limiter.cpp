/**
 * BATTLE LIMITER Implementation
 * 
 * True peak limiter with lookahead for zero clipping.
 * Essential for sound system battles where volume is MAXIMUM.
 */

#include "battle_audio_engine.h"

// Implementation is in header (inline class)
// This file is for any additional limiter functions

namespace ultramusic {

// Advanced true peak detection with oversampling
float detectTruePeak(const float* samples, int numSamples, int channels) {
    float maxPeak = 0.0f;
    
    for (int i = 0; i < numSamples; i++) {
        float absVal = std::abs(samples[i]);
        if (absVal > maxPeak) {
            maxPeak = absVal;
        }
    }
    
    // Estimate inter-sample peaks (simplified)
    // Full implementation would use 4x oversampling
    for (int i = 1; i < numSamples - 1; i++) {
        // Parabolic interpolation for inter-sample peak estimation
        float y0 = samples[i - 1];
        float y1 = samples[i];
        float y2 = samples[i + 1];
        
        // If this is a local maximum
        if ((y1 > y0 && y1 > y2) || (y1 < y0 && y1 < y2)) {
            float d = (y0 - y2) / (2.0f * (y0 - 2.0f * y1 + y2));
            if (std::abs(d) < 1.0f) {
                float peak = y1 - 0.25f * (y0 - y2) * d;
                float absPeak = std::abs(peak);
                if (absPeak > maxPeak) {
                    maxPeak = absPeak;
                }
            }
        }
    }
    
    return maxPeak;
}

// Soft clip function for gentle limiting
float softClip(float input, float threshold) {
    if (std::abs(input) <= threshold) {
        return input;
    }
    
    // Soft saturation curve
    float sign = input > 0 ? 1.0f : -1.0f;
    float absInput = std::abs(input);
    float excess = absInput - threshold;
    float compressed = threshold + (1.0f - threshold) * std::tanh(excess / (1.0f - threshold));
    
    return sign * compressed;
}

} // namespace ultramusic
