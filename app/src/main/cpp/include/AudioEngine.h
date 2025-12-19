/**
 * UltraMusicPlayer - AudioEngine.h
 * 
 * High-performance audio engine with professional-grade time stretching
 * and pitch shifting. Designed to exceed all existing music speed changers.
 * 
 * Features:
 * - Speed range: 0.05x to 10.0x (industry-leading)
 * - Pitch range: -36 to +36 semitones (3 octaves)
 * - Cent-level precision: 0.01 cent accuracy
 * - Multiple quality modes for different use cases
 * - Formant preservation for natural vocals
 * - Real-time processing with Oboe
 */

#ifndef ULTRA_MUSIC_AUDIO_ENGINE_H
#define ULTRA_MUSIC_AUDIO_ENGINE_H

#include <oboe/Oboe.h>
#include <memory>
#include <vector>
#include <atomic>
#include <mutex>
#include <functional>

namespace ultramusic {

// Quality modes for different use cases
enum class QualityMode {
    ULTRA_HIGH,     // Best quality, highest CPU (offline export)
    HIGH,           // Great quality, moderate CPU (default)
    BALANCED,       // Good quality, balanced CPU
    PERFORMANCE,    // Acceptable quality, low CPU (old devices)
    VOICE,          // Optimized for speech/vocals
    INSTRUMENT,     // Optimized for instruments
    PERCUSSION      // Optimized for drums/percussion
};

// Processing algorithm selection
enum class Algorithm {
    PHASE_VOCODER,      // Frequency domain - highest quality
    WSOLA,              // Time domain - lower latency
    HYBRID,             // Adaptive switching
    ELASTIQUE_STYLE     // Our premium algorithm
};

// Audio processing parameters
struct AudioParams {
    // Speed control (0.05 to 10.0)
    float speed = 1.0f;
    
    // Pitch in semitones (-36 to +36)
    float pitchSemitones = 0.0f;
    
    // Fine pitch in cents (-100 to +100)
    float pitchCents = 0.0f;
    
    // Formant shift (independent of pitch)
    float formantShift = 0.0f;
    
    // Preserve formants when pitch shifting
    bool preserveFormants = true;
    
    // Quality mode
    QualityMode quality = QualityMode::HIGH;
    
    // Algorithm selection
    Algorithm algorithm = Algorithm::PHASE_VOCODER;
    
    // Enable transient preservation
    bool preserveTransients = true;
    
    // Phase locking for better quality
    bool phaseLocking = true;
    
    // Anti-aliasing filter
    bool antiAliasing = true;
};

// Loop region for A-B repeat
struct LoopRegion {
    int64_t startFrame = 0;
    int64_t endFrame = 0;
    bool enabled = false;
};

// Callback interface for audio events
class AudioEngineCallback {
public:
    virtual ~AudioEngineCallback() = default;
    virtual void onPlaybackPositionChanged(int64_t positionFrames) = 0;
    virtual void onPlaybackStateChanged(bool isPlaying) = 0;
    virtual void onError(const std::string& message) = 0;
};

/**
 * Main Audio Engine class
 * 
 * Handles all audio processing with Oboe for low-latency playback.
 * Implements multiple high-quality DSP algorithms.
 */
class AudioEngine : public oboe::AudioStreamCallback {
public:
    AudioEngine();
    ~AudioEngine();

    // Initialization
    bool initialize(int32_t sampleRate = 44100, int32_t channelCount = 2);
    void shutdown();
    
    // Audio file loading
    bool loadAudioFile(const std::string& filePath);
    bool loadAudioData(const float* data, int64_t frameCount, int32_t sampleRate, int32_t channels);
    void unloadAudio();
    
    // Playback control
    void play();
    void pause();
    void stop();
    void seekTo(int64_t framePosition);
    void seekToTime(double seconds);
    
    // Parameter control - EXTENDED RANGES
    void setSpeed(float speed);                    // 0.05 to 10.0
    void setPitchSemitones(float semitones);       // -36 to +36
    void setPitchCents(float cents);               // -100 to +100 (fine tune)
    void setFormantShift(float shift);             // -12 to +12
    void setPreserveFormants(bool preserve);
    void setQualityMode(QualityMode mode);
    void setAlgorithm(Algorithm algorithm);
    
    // Combined pitch (semitones + cents)
    float getTotalPitchShift() const;
    
    // Loop control
    void setLoopRegion(int64_t startFrame, int64_t endFrame);
    void enableLoop(bool enable);
    void clearLoop();
    
    // State queries
    bool isPlaying() const { return m_isPlaying.load(); }
    int64_t getCurrentPosition() const { return m_currentPosition.load(); }
    int64_t getTotalFrames() const { return m_totalFrames; }
    double getCurrentTimeSeconds() const;
    double getTotalTimeSeconds() const;
    float getCurrentSpeed() const { return m_params.speed; }
    float getCurrentPitchSemitones() const { return m_params.pitchSemitones; }
    
    // BPM detection
    float detectBPM();
    void setTargetBPM(float bpm);
    
    // Callback registration
    void setCallback(AudioEngineCallback* callback) { m_callback = callback; }
    
    // Export processed audio
    bool exportToFile(const std::string& outputPath, const std::string& format = "wav");
    
    // Oboe callback
    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* audioStream,
        void* audioData,
        int32_t numFrames) override;

private:
    // Audio stream
    std::shared_ptr<oboe::AudioStream> m_audioStream;
    
    // Audio data buffer
    std::vector<float> m_audioBuffer;
    int64_t m_totalFrames = 0;
    int32_t m_sampleRate = 44100;
    int32_t m_channelCount = 2;
    
    // Processing components (forward declarations)
    class PhaseVocoder;
    class TimeStretcher;
    class PitchShifter;
    class FormantPreserver;
    class Resampler;
    
    std::unique_ptr<PhaseVocoder> m_phaseVocoder;
    std::unique_ptr<TimeStretcher> m_timeStretcher;
    std::unique_ptr<PitchShifter> m_pitchShifter;
    std::unique_ptr<FormantPreserver> m_formantPreserver;
    std::unique_ptr<Resampler> m_resampler;
    
    // Parameters
    AudioParams m_params;
    LoopRegion m_loopRegion;
    
    // State
    std::atomic<bool> m_isPlaying{false};
    std::atomic<bool> m_isInitialized{false};
    std::atomic<int64_t> m_currentPosition{0};
    
    // Mutex for thread safety
    mutable std::mutex m_mutex;
    
    // Callback
    AudioEngineCallback* m_callback = nullptr;
    
    // Internal processing
    void processAudioBlock(float* output, int32_t numFrames);
    void updateProcessingChain();
    float calculatePitchRatio() const;
    
    // Stream management
    bool createAudioStream();
    void closeAudioStream();
};

} // namespace ultramusic

#endif // ULTRA_MUSIC_AUDIO_ENGINE_H
