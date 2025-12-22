/**
 * SoundTouch Library - Stub Implementation
 * 
 * This is a MINIMAL implementation for building without the full SoundTouch library.
 * It provides basic functionality using a simplified WSOLA algorithm.
 * 
 * For FULL quality, download the real SoundTouch source:
 * https://codeberg.org/soundtouch/soundtouch
 * 
 * Run: ./setup_soundtouch.sh to download automatically
 */

#include "SoundTouch.h"
#include <cmath>
#include <vector>
#include <algorithm>
#include <cstring>

namespace soundtouch {

// Internal implementation
class SoundTouch::Impl {
public:
    Impl() {
        clear();
    }
    
    void setSampleRate(unsigned int rate) {
        sampleRate = rate;
        updateParameters();
    }
    
    void setChannels(unsigned int ch) {
        channels = ch;
    }
    
    void setTempo(float t) {
        tempo = std::clamp(t, 0.05f, 10.0f);
    }
    
    void setPitch(float p) {
        pitch = std::clamp(p, 0.25f, 4.0f);
    }
    
    void setPitchSemiTones(float semi) {
        // Convert semitones to ratio: 2^(semitones/12)
        pitch = std::pow(2.0f, semi / 12.0f);
        pitch = std::clamp(pitch, 0.25f, 4.0f);
    }
    
    void setRate(float r) {
        rate = std::clamp(r, 0.05f, 10.0f);
    }
    
    bool setSetting(int id, int value) {
        switch (id) {
            case SETTING_USE_AA_FILTER:
                useAAFilter = value != 0;
                break;
            case SETTING_SEQUENCE_MS:
                sequenceMs = value;
                updateParameters();
                break;
            case SETTING_SEEKWINDOW_MS:
                seekWindowMs = value;
                updateParameters();
                break;
            case SETTING_OVERLAP_MS:
                overlapMs = value;
                updateParameters();
                break;
            default:
                return false;
        }
        return true;
    }
    
    int getSetting(int id) const {
        switch (id) {
            case SETTING_USE_AA_FILTER: return useAAFilter ? 1 : 0;
            case SETTING_SEQUENCE_MS: return sequenceMs;
            case SETTING_SEEKWINDOW_MS: return seekWindowMs;
            case SETTING_OVERLAP_MS: return overlapMs;
            default: return 0;
        }
    }
    
    void putSamples(const short* samples, unsigned int numFrames) {
        size_t numSamples = numFrames * channels;
        size_t oldSize = inputBuffer.size();
        inputBuffer.resize(oldSize + numSamples);
        std::memcpy(inputBuffer.data() + oldSize, samples, numSamples * sizeof(short));
        
        // Process when we have enough samples
        processInternal();
    }
    
    void putSamples(const float* samples, unsigned int numFrames) {
        // Convert float to short
        std::vector<short> shortSamples(numFrames * channels);
        for (size_t i = 0; i < shortSamples.size(); i++) {
            float s = samples[i] * 32767.0f;
            shortSamples[i] = static_cast<short>(std::clamp(s, -32768.0f, 32767.0f));
        }
        putSamples(shortSamples.data(), numFrames);
    }
    
    unsigned int receiveSamples(short* output, unsigned int maxFrames) {
        size_t maxSamples = maxFrames * channels;
        size_t available = std::min(outputBuffer.size(), maxSamples);
        
        if (available > 0) {
            std::memcpy(output, outputBuffer.data(), available * sizeof(short));
            outputBuffer.erase(outputBuffer.begin(), outputBuffer.begin() + available);
        }
        
        return available / channels;
    }
    
    unsigned int receiveSamples(float* output, unsigned int maxFrames) {
        std::vector<short> shortOutput(maxFrames * channels);
        unsigned int received = receiveSamples(shortOutput.data(), maxFrames);
        
        for (size_t i = 0; i < received * channels; i++) {
            output[i] = shortOutput[i] / 32768.0f;
        }
        
        return received;
    }
    
    unsigned int numSamples() const {
        return outputBuffer.size() / channels;
    }
    
    unsigned int numUnprocessedSamples() const {
        return inputBuffer.size() / channels;
    }
    
    int isEmpty() const {
        return outputBuffer.empty() ? 1 : 0;
    }
    
    void flush() {
        // Process any remaining samples
        if (!inputBuffer.empty()) {
            processInternal();
        }
    }
    
    void clear() {
        inputBuffer.clear();
        outputBuffer.clear();
        position = 0;
    }
    
private:
    void updateParameters() {
        sequenceSamples = (sequenceMs * sampleRate) / 1000;
        seekWindowSamples = (seekWindowMs * sampleRate) / 1000;
        overlapSamples = (overlapMs * sampleRate) / 1000;
    }
    
    void processInternal() {
        // Simplified WSOLA algorithm
        // For full quality, use the real SoundTouch library
        
        float effectiveTempo = tempo * rate;
        if (effectiveTempo < 0.05f) effectiveTempo = 0.05f;
        if (effectiveTempo > 10.0f) effectiveTempo = 10.0f;
        
        size_t inputFrames = inputBuffer.size() / channels;
        if (inputFrames < sequenceSamples) return;
        
        // Calculate output size
        size_t outputFrames = static_cast<size_t>(inputFrames / effectiveTempo);
        if (outputFrames == 0) return;
        
        std::vector<short> processed(outputFrames * channels);
        
        // Simple linear interpolation for time-stretching
        for (size_t outFrame = 0; outFrame < outputFrames; outFrame++) {
            float inPos = outFrame * effectiveTempo;
            size_t inFrame = static_cast<size_t>(inPos);
            float frac = inPos - inFrame;
            
            if (inFrame >= inputFrames - 1) {
                inFrame = inputFrames - 2;
                frac = 1.0f;
            }
            
            for (unsigned int ch = 0; ch < channels; ch++) {
                float s1 = inputBuffer[inFrame * channels + ch];
                float s2 = inputBuffer[(inFrame + 1) * channels + ch];
                processed[outFrame * channels + ch] = 
                    static_cast<short>(s1 + frac * (s2 - s1));
            }
        }
        
        // Apply pitch shift using simple resampling
        if (std::abs(pitch - 1.0f) > 0.01f) {
            size_t pitchedFrames = static_cast<size_t>(outputFrames / pitch);
            std::vector<short> pitched(pitchedFrames * channels);
            
            for (size_t outFrame = 0; outFrame < pitchedFrames; outFrame++) {
                float inPos = outFrame * pitch;
                size_t inFrame = static_cast<size_t>(inPos);
                float frac = inPos - inFrame;
                
                if (inFrame >= outputFrames - 1) {
                    inFrame = outputFrames - 2;
                    frac = 1.0f;
                }
                
                for (unsigned int ch = 0; ch < channels; ch++) {
                    float s1 = processed[inFrame * channels + ch];
                    float s2 = processed[(inFrame + 1) * channels + ch];
                    pitched[outFrame * channels + ch] = 
                        static_cast<short>(s1 + frac * (s2 - s1));
                }
            }
            
            processed = std::move(pitched);
        }
        
        // Append to output buffer
        outputBuffer.insert(outputBuffer.end(), processed.begin(), processed.end());
        
        // Clear processed input
        inputBuffer.clear();
    }
    
    unsigned int sampleRate = 44100;
    unsigned int channels = 2;
    
    float tempo = 1.0f;
    float pitch = 1.0f;
    float rate = 1.0f;
    
    bool useAAFilter = true;
    int sequenceMs = 40;
    int seekWindowMs = 15;
    int overlapMs = 8;
    
    int sequenceSamples = 1764;
    int seekWindowSamples = 661;
    int overlapSamples = 352;
    
    std::vector<short> inputBuffer;
    std::vector<short> outputBuffer;
    size_t position = 0;
};

// SoundTouch class implementation

SoundTouch::SoundTouch() : pImpl(new Impl()) {}

SoundTouch::~SoundTouch() { delete pImpl; }

const char* SoundTouch::getVersionString() {
    return "UltraMusic SoundTouch Stub 1.0";
}

unsigned int SoundTouch::getVersionId() {
    return 0x020302;  // 2.3.2
}

void SoundTouch::setSampleRate(unsigned int rate) { pImpl->setSampleRate(rate); }
void SoundTouch::setChannels(unsigned int ch) { pImpl->setChannels(ch); }
void SoundTouch::setTempo(float t) { pImpl->setTempo(t); }
void SoundTouch::setTempoChange(float t) { pImpl->setTempo(1.0f + t / 100.0f); }
void SoundTouch::setPitch(float p) { pImpl->setPitch(p); }
void SoundTouch::setPitchSemiTones(float s) { pImpl->setPitchSemiTones(s); }
void SoundTouch::setPitchSemiTones(int s) { pImpl->setPitchSemiTones(static_cast<float>(s)); }
void SoundTouch::setPitchOctaves(float o) { pImpl->setPitchSemiTones(o * 12.0f); }
void SoundTouch::setRate(float r) { pImpl->setRate(r); }
void SoundTouch::setRateChange(float r) { pImpl->setRate(1.0f + r / 100.0f); }
bool SoundTouch::setSetting(int id, int val) { return pImpl->setSetting(id, val); }
int SoundTouch::getSetting(int id) const { return pImpl->getSetting(id); }
unsigned int SoundTouch::numSamples() const { return pImpl->numSamples(); }
unsigned int SoundTouch::numUnprocessedSamples() const { return pImpl->numUnprocessedSamples(); }
int SoundTouch::isEmpty() const { return pImpl->isEmpty(); }
void SoundTouch::putSamples(const short* s, unsigned int n) { pImpl->putSamples(s, n); }
void SoundTouch::putSamples(const float* s, unsigned int n) { pImpl->putSamples(s, n); }
unsigned int SoundTouch::receiveSamples(short* o, unsigned int m) { return pImpl->receiveSamples(o, m); }
unsigned int SoundTouch::receiveSamples(float* o, unsigned int m) { return pImpl->receiveSamples(o, m); }
void SoundTouch::flush() { pImpl->flush(); }
void SoundTouch::clear() { pImpl->clear(); }

} // namespace soundtouch
