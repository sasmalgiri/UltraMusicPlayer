/**
 * SoundTouch Library Header
 * 
 * This is a minimal header for the SoundTouch audio processing library.
 * For the full library, download from: https://codeberg.org/nicholaspark/soundtouch
 * 
 * SoundTouch provides:
 * - Time-stretch (change tempo without pitch)
 * - Pitch-shift (change pitch without tempo)
 * - Rate transposition (change both together)
 */

#ifndef SOUNDTOUCH_H
#define SOUNDTOUCH_H

#include <cstddef>

// Settings
#define SETTING_USE_AA_FILTER       0
#define SETTING_AA_FILTER_LENGTH    1
#define SETTING_USE_QUICKSEEK       2
#define SETTING_SEQUENCE_MS         3
#define SETTING_SEEKWINDOW_MS       4
#define SETTING_OVERLAP_MS          5
#define SETTING_NOMINAL_INPUT_SEQUENCE    6
#define SETTING_NOMINAL_OUTPUT_SEQUENCE   7
#define SETTING_INITIAL_LATENCY           8

namespace soundtouch {

/// SoundTouch audio processing class
class SoundTouch {
public:
    SoundTouch();
    virtual ~SoundTouch();
    
    /// Get SoundTouch library version
    static const char* getVersionString();
    static unsigned int getVersionId();
    
    /// Set sample rate
    void setSampleRate(unsigned int sampleRate);
    
    /// Set number of channels (1=mono, 2=stereo)
    void setChannels(unsigned int numChannels);
    
    /// Set tempo (time-stretch without pitch change)
    /// 1.0 = original, 2.0 = 2x faster, 0.5 = 2x slower
    void setTempo(float newTempo);
    
    /// Set tempo change in percent (-50 to +100)
    void setTempoChange(float newTempo);
    
    /// Set pitch (without tempo change)
    /// 1.0 = original, 2.0 = octave up, 0.5 = octave down
    void setPitch(float newPitch);
    
    /// Set pitch in semitones (-36 to +36 typical)
    void setPitchSemiTones(float newPitch);
    void setPitchSemiTones(int newPitch);
    
    /// Set pitch in octaves
    void setPitchOctaves(float newPitch);
    
    /// Set rate (changes both speed and pitch together)
    void setRate(float newRate);
    
    /// Set rate change in percent
    void setRateChange(float newRate);
    
    /// Set processing setting
    /// @param settingId - Setting ID (SETTING_xxx constants)
    /// @param value - Setting value
    /// @return true if successful
    bool setSetting(int settingId, int value);
    
    /// Get processing setting value
    int getSetting(int settingId) const;
    
    /// Get number of samples in input buffer
    unsigned int numSamples() const;
    
    /// Get number of unprocessed samples
    unsigned int numUnprocessedSamples() const;
    
    /// Check if there are samples to output
    int isEmpty() const;
    
    /// Feed samples to process
    /// @param samples - Pointer to sample buffer
    /// @param numSamples - Number of sample FRAMES (not individual samples)
    void putSamples(const short *samples, unsigned int numSamples);
    void putSamples(const float *samples, unsigned int numSamples);
    
    /// Receive processed samples
    /// @param output - Buffer for output samples
    /// @param maxSamples - Maximum frames to receive
    /// @return Number of frames written
    unsigned int receiveSamples(short *output, unsigned int maxSamples);
    unsigned int receiveSamples(float *output, unsigned int maxSamples);
    
    /// Flush processing pipeline
    void flush();
    
    /// Clear all samples
    void clear();
    
private:
    class Impl;
    Impl* pImpl;
};

} // namespace soundtouch

#endif // SOUNDTOUCH_H
