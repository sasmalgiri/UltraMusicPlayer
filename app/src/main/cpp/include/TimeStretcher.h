/**
 * UltraMusicPlayer - TimeStretcher.h
 * 
 * Advanced time stretching with extended range: 0.05x to 10.0x
 * Exceeds all existing apps with 0.01x precision.
 * 
 * Multiple algorithms for different quality/performance needs:
 * - Phase Vocoder (highest quality)
 * - WSOLA (low latency)
 * - Hybrid (adaptive)
 */

#ifndef ULTRA_MUSIC_TIME_STRETCHER_H
#define ULTRA_MUSIC_TIME_STRETCHER_H

#include <vector>
#include <memory>
#include <cstdint>
#include <functional>

namespace ultramusic {

// Forward declarations
class PhaseVocoder;
class MultiResolutionPhaseVocoder;

// Time stretch algorithm type
enum class TimeStretchAlgorithm {
    PHASE_VOCODER,          // Best quality, higher latency
    WSOLA,                  // Good quality, low latency
    HYBRID,                 // Automatic selection based on ratio
    MULTI_RESOLUTION        // Premium quality (uses MultiResolutionPhaseVocoder)
};

// Stretch mode for different content types
enum class StretchMode {
    GENERAL,        // Good for most content
    VOICE,          // Optimized for speech
    MUSIC,          // Optimized for music
    DRUMS,          // Preserve transients
    SOLO            // Single instrument
};

/**
 * TimeStretcher class
 * 
 * Industry-leading time stretching with:
 * - Extended range: 0.05x to 10.0x (20x slower to 10x faster)
 * - 0.01x precision for fine control
 * - Multiple quality modes
 * - Automatic transient preservation
 */
class TimeStretcher {
public:
    // Speed limits - exceeding competition
    static constexpr float MIN_SPEED = 0.05f;   // 20x slower (competition: 0.25x)
    static constexpr float MAX_SPEED = 10.0f;   // 10x faster (competition: 4x)
    static constexpr float SPEED_PRECISION = 0.01f;
    
    TimeStretcher(int32_t sampleRate, int32_t channels);
    ~TimeStretcher();
    
    // Configuration
    void setAlgorithm(TimeStretchAlgorithm algorithm);
    void setMode(StretchMode mode);
    void setQuality(int level);  // 0-100
    
    // Speed control with extended range
    void setSpeed(float speed);
    float getSpeed() const { return m_speed; }
    
    // Tap tempo - set speed to match a target BPM
    void setTargetBPM(float targetBPM, float originalBPM);
    
    // Speed ramp (gradual change over time)
    void setSpeedRamp(float startSpeed, float endSpeed, int64_t durationFrames);
    bool isRamping() const { return m_isRamping; }
    
    // Processing
    int32_t process(const float* input, int32_t inputFrames, 
                   float* output, int32_t maxOutputFrames);
    void reset();
    
    // Get output frames for given input frames at current speed
    int32_t getExpectedOutputFrames(int32_t inputFrames) const;
    
    // Latency
    int32_t getLatencyFrames() const;
    
    // Quality metrics
    float getQualityScore() const;  // 0.0 - 1.0
    
private:
    int32_t m_sampleRate;
    int32_t m_channelCount;
    
    float m_speed = 1.0f;
    TimeStretchAlgorithm m_algorithm = TimeStretchAlgorithm::PHASE_VOCODER;
    StretchMode m_mode = StretchMode::MUSIC;
    int m_qualityLevel = 80;
    
    // Speed ramping
    bool m_isRamping = false;
    float m_rampStartSpeed = 1.0f;
    float m_rampEndSpeed = 1.0f;
    int64_t m_rampDuration = 0;
    int64_t m_rampPosition = 0;
    
    // Processing engines
    std::unique_ptr<PhaseVocoder> m_phaseVocoder;
    std::unique_ptr<MultiResolutionPhaseVocoder> m_multiResVocoder;
    
    // WSOLA buffers
    std::vector<float> m_wsolaBuffer;
    std::vector<float> m_searchBuffer;
    int32_t m_wsolaPosition = 0;
    
    // Internal methods
    void updateAlgorithmParams();
    int32_t processPhaseVocoder(const float* input, int32_t inputFrames,
                                float* output, int32_t maxOutputFrames);
    int32_t processWSOLA(const float* input, int32_t inputFrames,
                         float* output, int32_t maxOutputFrames);
    
    // WSOLA similarity search
    int32_t findBestOverlapPosition(const float* target, int32_t targetLength);
    
    // Speed ramp calculation
    float calculateRampedSpeed();
};

/**
 * SpeedCurve class
 * 
 * Allows drawing custom speed curves throughout a track.
 * This is a unique feature not found in competitors.
 */
class SpeedCurve {
public:
    struct Point {
        double timeSeconds;
        float speed;
    };
    
    SpeedCurve();
    
    // Add/remove control points
    void addPoint(double timeSeconds, float speed);
    void removePoint(int index);
    void clear();
    
    // Get interpolated speed at any time
    float getSpeedAtTime(double timeSeconds) const;
    
    // Get all points
    const std::vector<Point>& getPoints() const { return m_points; }
    
    // Preset curves
    static SpeedCurve createLinearRamp(float startSpeed, float endSpeed, double duration);
    static SpeedCurve createSCurve(float startSpeed, float endSpeed, double duration);
    static SpeedCurve createPulse(float baseSpeed, float peakSpeed, double peakTime, double duration);
    
private:
    std::vector<Point> m_points;
    
    // Interpolation
    float interpolate(const Point& p1, const Point& p2, double time) const;
};

} // namespace ultramusic

#endif // ULTRA_MUSIC_TIME_STRETCHER_H
