/**
 * UltraMusicPlayer - PitchShifter.h
 * 
 * Professional pitch shifting with extended range: -36 to +36 semitones (3 octaves!)
 * With cent-level precision (0.01 cent accuracy).
 * 
 * Advanced features:
 * - Formant preservation for natural vocals
 * - Independent formant control
 * - Reference pitch tuning (A=432Hz, etc.)
 * - Harmony generation
 * - Scale locking
 */

#ifndef ULTRA_MUSIC_PITCH_SHIFTER_H
#define ULTRA_MUSIC_PITCH_SHIFTER_H

#include <vector>
#include <memory>
#include <cstdint>
#include <array>

namespace ultramusic {

// Forward declarations
class PhaseVocoder;
class FormantPreserver;
class Resampler;

// Pitch shifting algorithm
enum class PitchAlgorithm {
    PHASE_VOCODER,      // Frequency domain - best quality
    RESAMPLING,         // Simple but introduces time change
    GRANULAR,           // Granular synthesis approach
    PSOLA               // Pitch-Synchronous Overlap-Add
};

// Musical scales for scale locking
enum class MusicalScale {
    CHROMATIC,      // All semitones
    MAJOR,
    MINOR,
    HARMONIC_MINOR,
    MELODIC_MINOR,
    PENTATONIC_MAJOR,
    PENTATONIC_MINOR,
    BLUES,
    DORIAN,
    PHRYGIAN,
    LYDIAN,
    MIXOLYDIAN,
    LOCRIAN
};

// Root notes
enum class RootNote {
    C = 0, Cs = 1, D = 2, Ds = 3, E = 4, F = 5,
    Fs = 6, G = 7, Gs = 8, A = 9, As = 10, B = 11
};

/**
 * PitchShifter class
 * 
 * Industry-leading pitch shifting with:
 * - Extended range: -36 to +36 semitones (competition: ±12)
 * - Cent precision: 0.01 cent accuracy
 * - Formant preservation for natural vocals
 * - Independent formant control
 */
class PitchShifter {
public:
    // Extended pitch limits - 3 octaves!
    static constexpr float MIN_SEMITONES = -36.0f;
    static constexpr float MAX_SEMITONES = 36.0f;
    static constexpr float CENT_PRECISION = 0.01f;
    
    // Formant limits
    static constexpr float MIN_FORMANT_SHIFT = -24.0f;  // -2 octaves
    static constexpr float MAX_FORMANT_SHIFT = 24.0f;   // +2 octaves
    
    PitchShifter(int32_t sampleRate, int32_t channels);
    ~PitchShifter();
    
    // Algorithm selection
    void setAlgorithm(PitchAlgorithm algorithm);
    
    // Main pitch control
    void setPitchSemitones(float semitones);        // -36 to +36
    void setPitchCents(float cents);                // -100 to +100 (fine tune)
    void setPitchRatio(float ratio);                // Direct ratio (0.25 to 4.0)
    
    // Get total pitch shift
    float getTotalSemitones() const;
    float getPitchRatio() const;
    
    // Formant control - key to natural sound
    void setPreserveFormants(bool preserve);
    void setFormantShift(float semitones);          // Independent formant shift
    void setFormantScale(float scale);              // 0.5 to 2.0
    bool isFormantPreserved() const { return m_preserveFormants; }
    
    // Reference pitch (A = 440Hz by default)
    void setReferencePitch(float frequencyHz);      // e.g., 432Hz
    float getReferencePitch() const { return m_referencePitch; }
    
    // Scale locking - snap pitch to musical scale
    void enableScaleLock(bool enable);
    void setScale(MusicalScale scale, RootNote root);
    float snapToScale(float semitones) const;
    
    // Vibrato effect
    void setVibrato(float depthCents, float rateHz);
    void enableVibrato(bool enable);
    
    // Processing
    int32_t process(const float* input, int32_t inputFrames,
                   float* output, int32_t maxOutputFrames);
    void reset();
    
    // Latency
    int32_t getLatencyFrames() const;
    
    // Analysis
    float detectPitch(const float* input, int32_t numFrames);  // Returns Hz
    float detectKeySignature(const float* input, int32_t numFrames);  // Estimates key
    
private:
    int32_t m_sampleRate;
    int32_t m_channelCount;
    
    // Pitch parameters
    float m_semitones = 0.0f;
    float m_cents = 0.0f;
    float m_formantShift = 0.0f;
    float m_referencePitch = 440.0f;
    bool m_preserveFormants = true;
    
    // Scale locking
    bool m_scaleLockEnabled = false;
    MusicalScale m_scale = MusicalScale::CHROMATIC;
    RootNote m_rootNote = RootNote::C;
    std::array<bool, 12> m_scaleNotes;
    
    // Vibrato
    bool m_vibratoEnabled = false;
    float m_vibratoDepth = 0.0f;
    float m_vibratoRate = 0.0f;
    float m_vibratoPhase = 0.0f;
    
    // Processing components
    PitchAlgorithm m_algorithm = PitchAlgorithm::PHASE_VOCODER;
    std::unique_ptr<PhaseVocoder> m_phaseVocoder;
    std::unique_ptr<FormantPreserver> m_formantPreserver;
    std::unique_ptr<Resampler> m_resampler;
    
    // Internal methods
    void updateScale();
    float calculatePitchRatio() const;
    float applyVibrato(float basePitch);
    void processWithFormantPreservation(const float* input, float* output, int32_t numFrames);
};

/**
 * HarmonyGenerator class
 * 
 * Generates harmonies based on the input pitch.
 * Can create up to 8 harmony voices.
 */
class HarmonyGenerator {
public:
    struct HarmonyVoice {
        float semitones = 0.0f;     // Pitch offset
        float volume = 1.0f;        // 0.0 to 1.0
        float pan = 0.0f;           // -1.0 (L) to 1.0 (R)
        bool enabled = false;
    };
    
    static constexpr int MAX_VOICES = 8;
    
    HarmonyGenerator(int32_t sampleRate, int32_t channels);
    ~HarmonyGenerator();
    
    // Configure harmony voices
    void setVoice(int index, const HarmonyVoice& voice);
    HarmonyVoice& getVoice(int index);
    
    // Preset harmonies
    void setPreset(const std::string& preset);  // "thirds", "fifths", "octave", "chord"
    
    // Scale-aware harmonies
    void setScale(MusicalScale scale, RootNote root);
    void enableScaleAware(bool enable);
    
    // Processing
    void process(const float* input, float* output, int32_t numFrames);
    
private:
    int32_t m_sampleRate;
    int32_t m_channelCount;
    
    std::array<HarmonyVoice, MAX_VOICES> m_voices;
    std::array<std::unique_ptr<PitchShifter>, MAX_VOICES> m_pitchShifters;
    
    bool m_scaleAware = false;
    MusicalScale m_scale = MusicalScale::MAJOR;
    RootNote m_rootNote = RootNote::C;
};

/**
 * PitchCurve class
 * 
 * Similar to SpeedCurve, allows drawing pitch changes throughout a track.
 */
class PitchCurve {
public:
    struct Point {
        double timeSeconds;
        float semitones;
        float cents;
    };
    
    PitchCurve();
    
    void addPoint(double timeSeconds, float semitones, float cents = 0.0f);
    void removePoint(int index);
    void clear();
    
    float getSemitonesAtTime(double timeSeconds) const;
    float getCentsAtTime(double timeSeconds) const;
    
    const std::vector<Point>& getPoints() const { return m_points; }
    
private:
    std::vector<Point> m_points;
};

} // namespace ultramusic

#endif // ULTRA_MUSIC_PITCH_SHIFTER_H
