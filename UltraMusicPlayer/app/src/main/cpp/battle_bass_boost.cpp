/**
 * BATTLE BASS BOOST Implementation
 *
 * Sub-bass enhancement with harmonic generation for maximum impact.
 * Designed to shake sound systems and rattle buildings.
 */

#include "battle_audio_engine.h"
#include <cmath>

namespace ultramusic {

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
