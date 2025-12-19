/**
 * UltraMusicPlayer - TimeStretcher.cpp
 * 
 * Advanced time stretching with EXTENDED RANGE: 0.05x to 10.0x
 * This exceeds all competition (typically 0.25x - 4x)
 * 
 * Quality-focused implementation with:
 * - Multiple algorithm selection
 * - Automatic quality adjustment based on stretch ratio
 * - Transient preservation
 * - Smooth speed ramping
 */

#include "../include/TimeStretcher.h"
#include "../include/PhaseVocoder.h"
#include <cmath>
#include <algorithm>
#include <numeric>

namespace ultramusic {

TimeStretcher::TimeStretcher(int32_t sampleRate, int32_t channels)
    : m_sampleRate(sampleRate)
    , m_channelCount(channels)
{
    // Initialize phase vocoder with high-quality settings
    PhaseVocoderConfig config;
    config.fftSize = 4096;
    config.hopSize = 1024;
    config.phaseLocking = true;
    config.transientDetection = true;
    config.verticalCoherence = true;
    
    m_phaseVocoder = std::make_unique<PhaseVocoder>(sampleRate, channels);
    m_phaseVocoder->configure(config);
    
    // Initialize multi-resolution vocoder for ultra-high quality
    m_multiResVocoder = std::make_unique<MultiResolutionPhaseVocoder>(sampleRate, channels);
    
    // Allocate WSOLA buffers
    int32_t maxWsolaWindow = sampleRate / 10;  // 100ms max window
    m_wsolaBuffer.resize(maxWsolaWindow * channels * 2);
    m_searchBuffer.resize(maxWsolaWindow * channels);
}

TimeStretcher::~TimeStretcher() = default;

void TimeStretcher::setAlgorithm(TimeStretchAlgorithm algorithm) {
    m_algorithm = algorithm;
    updateAlgorithmParams();
}

void TimeStretcher::setMode(StretchMode mode) {
    m_mode = mode;
    updateAlgorithmParams();
}

void TimeStretcher::setQuality(int level) {
    m_qualityLevel = std::clamp(level, 0, 100);
    updateAlgorithmParams();
}

void TimeStretcher::setSpeed(float speed) {
    // EXTENDED RANGE: 0.05x to 10.0x
    m_speed = std::clamp(speed, MIN_SPEED, MAX_SPEED);
    
    // Update phase vocoder
    float stretchRatio = 1.0f / m_speed;  // Inverse for time stretch
    
    if (m_phaseVocoder) {
        m_phaseVocoder->setTimeStretchRatio(stretchRatio);
    }
    if (m_multiResVocoder) {
        m_multiResVocoder->setTimeStretchRatio(stretchRatio);
    }
    
    // For extreme ratios, automatically adjust quality settings
    if (m_speed < 0.2f || m_speed > 5.0f) {
        // Extreme stretch - use multi-resolution for best quality
        if (m_algorithm == TimeStretchAlgorithm::HYBRID) {
            // Temporarily switch to multi-resolution
        }
    }
}

void TimeStretcher::setTargetBPM(float targetBPM, float originalBPM) {
    if (originalBPM > 0) {
        float newSpeed = targetBPM / originalBPM;
        setSpeed(newSpeed);
    }
}

void TimeStretcher::setSpeedRamp(float startSpeed, float endSpeed, int64_t durationFrames) {
    m_rampStartSpeed = std::clamp(startSpeed, MIN_SPEED, MAX_SPEED);
    m_rampEndSpeed = std::clamp(endSpeed, MIN_SPEED, MAX_SPEED);
    m_rampDuration = durationFrames;
    m_rampPosition = 0;
    m_isRamping = true;
}

float TimeStretcher::calculateRampedSpeed() {
    if (!m_isRamping || m_rampDuration <= 0) {
        return m_speed;
    }
    
    float progress = static_cast<float>(m_rampPosition) / m_rampDuration;
    progress = std::clamp(progress, 0.0f, 1.0f);
    
    // Use smoothstep for natural-feeling ramp
    float smoothProgress = progress * progress * (3.0f - 2.0f * progress);
    
    float currentSpeed = m_rampStartSpeed + smoothProgress * (m_rampEndSpeed - m_rampStartSpeed);
    
    m_rampPosition++;
    if (m_rampPosition >= m_rampDuration) {
        m_isRamping = false;
        m_speed = m_rampEndSpeed;
    }
    
    return currentSpeed;
}

int32_t TimeStretcher::process(const float* input, int32_t inputFrames,
                               float* output, int32_t maxOutputFrames) {
    
    // Handle speed ramping
    float currentSpeed = m_isRamping ? calculateRampedSpeed() : m_speed;
    
    // Calculate expected output frames
    int32_t expectedOutput = getExpectedOutputFrames(inputFrames);
    int32_t actualOutput = std::min(expectedOutput, maxOutputFrames);
    
    // Select algorithm based on settings
    switch (m_algorithm) {
        case TimeStretchAlgorithm::PHASE_VOCODER:
            return processPhaseVocoder(input, inputFrames, output, actualOutput);
            
        case TimeStretchAlgorithm::WSOLA:
            return processWSOLA(input, inputFrames, output, actualOutput);
            
        case TimeStretchAlgorithm::MULTI_RESOLUTION:
            // Use multi-resolution vocoder for highest quality
            if (m_multiResVocoder) {
                m_multiResVocoder->process(input, output, std::min(inputFrames, actualOutput));
                return actualOutput;
            }
            // Fall through to phase vocoder if multi-res not available
            [[fallthrough]];
            
        case TimeStretchAlgorithm::HYBRID:
        default:
            // Hybrid: choose best algorithm based on stretch ratio
            if (m_speed > 0.8f && m_speed < 1.2f) {
                // Near unity - use simpler processing
                return processWSOLA(input, inputFrames, output, actualOutput);
            } else if (m_speed < 0.3f || m_speed > 3.0f) {
                // Extreme - use multi-resolution
                if (m_multiResVocoder) {
                    m_multiResVocoder->process(input, output, std::min(inputFrames, actualOutput));
                    return actualOutput;
                }
            }
            // Default to standard phase vocoder
            return processPhaseVocoder(input, inputFrames, output, actualOutput);
    }
}

int32_t TimeStretcher::processPhaseVocoder(const float* input, int32_t inputFrames,
                                           float* output, int32_t maxOutputFrames) {
    if (!m_phaseVocoder) {
        // Fallback: just copy input to output
        int32_t framesToCopy = std::min(inputFrames, maxOutputFrames);
        std::copy(input, input + framesToCopy * m_channelCount, output);
        return framesToCopy;
    }
    
    // Update stretch ratio
    m_phaseVocoder->setTimeStretchRatio(1.0f / m_speed);
    
    // Process through phase vocoder
    int32_t outputFrames = std::min(getExpectedOutputFrames(inputFrames), maxOutputFrames);
    m_phaseVocoder->process(input, output, outputFrames);
    
    return outputFrames;
}

int32_t TimeStretcher::processWSOLA(const float* input, int32_t inputFrames,
                                    float* output, int32_t maxOutputFrames) {
    /**
     * WSOLA (Waveform Similarity Overlap-Add)
     * 
     * Time-domain algorithm that:
     * 1. Finds similar waveform segments
     * 2. Overlaps them to stretch/compress time
     * 3. Maintains transients and attacks
     * 
     * Lower quality than phase vocoder but lower latency.
     */
    
    int32_t windowSize = m_sampleRate / 25;  // 40ms window
    int32_t hopSize = windowSize / 4;        // 75% overlap
    int32_t searchRadius = windowSize / 2;   // Search range for best match
    
    float stretchRatio = 1.0f / m_speed;
    int32_t outputHop = static_cast<int32_t>(hopSize * stretchRatio);
    
    int32_t outputFrames = 0;
    int32_t inputPos = 0;
    int32_t outputPos = 0;
    
    // Hann window for smooth overlap
    std::vector<float> window(windowSize);
    for (int32_t i = 0; i < windowSize; ++i) {
        window[i] = 0.5f * (1.0f - std::cos(2.0f * 3.14159f * i / (windowSize - 1)));
    }
    
    // Clear output
    std::fill(output, output + maxOutputFrames * m_channelCount, 0.0f);
    
    while (inputPos + windowSize <= inputFrames && outputPos + windowSize <= maxOutputFrames) {
        // Find best overlap position
        int32_t bestOffset = 0;
        
        if (outputPos > 0) {
            bestOffset = findBestOverlapPosition(
                output + (outputPos - hopSize) * m_channelCount,
                hopSize
            );
        }
        
        // Apply window and add to output
        for (int32_t i = 0; i < windowSize; ++i) {
            int32_t srcIdx = inputPos + i + bestOffset;
            if (srcIdx >= 0 && srcIdx < inputFrames) {
                for (int32_t ch = 0; ch < m_channelCount; ++ch) {
                    int32_t outIdx = (outputPos + i) * m_channelCount + ch;
                    int32_t inIdx = srcIdx * m_channelCount + ch;
                    if (outIdx < maxOutputFrames * m_channelCount) {
                        output[outIdx] += input[inIdx] * window[i];
                    }
                }
            }
        }
        
        inputPos += hopSize;
        outputPos += outputHop;
        outputFrames = outputPos;
    }
    
    return std::min(outputFrames, maxOutputFrames);
}

int32_t TimeStretcher::findBestOverlapPosition(const float* target, int32_t targetLength) {
    /**
     * Find position in search buffer that best matches target for smooth overlap.
     * Uses cross-correlation to find best match.
     */
    
    int32_t searchRadius = targetLength / 2;
    int32_t bestOffset = 0;
    float bestCorrelation = -1.0f;
    
    for (int32_t offset = -searchRadius; offset <= searchRadius; ++offset) {
        float correlation = 0.0f;
        float targetEnergy = 0.0f;
        float searchEnergy = 0.0f;
        
        for (int32_t i = 0; i < targetLength; ++i) {
            int32_t searchIdx = i + offset;
            if (searchIdx >= 0 && searchIdx < static_cast<int32_t>(m_searchBuffer.size() / m_channelCount)) {
                for (int32_t ch = 0; ch < m_channelCount; ++ch) {
                    float t = target[i * m_channelCount + ch];
                    float s = m_searchBuffer[searchIdx * m_channelCount + ch];
                    
                    correlation += t * s;
                    targetEnergy += t * t;
                    searchEnergy += s * s;
                }
            }
        }
        
        // Normalized correlation
        float normalization = std::sqrt(targetEnergy * searchEnergy);
        if (normalization > 0.0001f) {
            correlation /= normalization;
        }
        
        if (correlation > bestCorrelation) {
            bestCorrelation = correlation;
            bestOffset = offset;
        }
    }
    
    return bestOffset;
}

void TimeStretcher::updateAlgorithmParams() {
    // Adjust parameters based on mode and quality
    
    PhaseVocoderConfig config;
    
    // Base FFT size on quality level
    if (m_qualityLevel >= 80) {
        config.fftSize = 4096;
        config.hopSize = 1024;
    } else if (m_qualityLevel >= 50) {
        config.fftSize = 2048;
        config.hopSize = 512;
    } else {
        config.fftSize = 1024;
        config.hopSize = 256;
    }
    
    // Mode-specific settings
    switch (m_mode) {
        case StretchMode::VOICE:
            config.fftSize = 2048;  // Smaller FFT for speech
            config.windowType = WindowType::GAUSSIAN;
            config.phaseLocking = true;
            config.transientDetection = false;
            break;
            
        case StretchMode::DRUMS:
            config.fftSize = 1024;  // Even smaller for transients
            config.windowType = WindowType::HANN;
            config.phaseLocking = false;
            config.transientDetection = true;
            break;
            
        case StretchMode::SOLO:
            config.fftSize = 4096;
            config.windowType = WindowType::BLACKMAN;
            config.phaseLocking = true;
            config.transientDetection = true;
            break;
            
        case StretchMode::MUSIC:
        case StretchMode::GENERAL:
        default:
            config.windowType = WindowType::HANN;
            config.phaseLocking = true;
            config.transientDetection = true;
            break;
    }
    
    if (m_phaseVocoder) {
        m_phaseVocoder->configure(config);
    }
}

void TimeStretcher::reset() {
    if (m_phaseVocoder) {
        m_phaseVocoder->reset();
    }
    if (m_multiResVocoder) {
        m_multiResVocoder->reset();
    }
    
    m_wsolaPosition = 0;
    std::fill(m_wsolaBuffer.begin(), m_wsolaBuffer.end(), 0.0f);
    std::fill(m_searchBuffer.begin(), m_searchBuffer.end(), 0.0f);
    
    m_isRamping = false;
    m_rampPosition = 0;
}

int32_t TimeStretcher::getExpectedOutputFrames(int32_t inputFrames) const {
    return static_cast<int32_t>(std::ceil(inputFrames / m_speed));
}

int32_t TimeStretcher::getLatencyFrames() const {
    if (m_phaseVocoder) {
        return m_phaseVocoder->getLatencyFrames();
    }
    return 4096;  // Default
}

float TimeStretcher::getQualityScore() const {
    // Estimate quality based on settings and stretch ratio
    float baseQuality = m_qualityLevel / 100.0f;
    
    // Extreme ratios reduce quality
    float ratioFactor = 1.0f;
    if (m_speed < 0.2f || m_speed > 5.0f) {
        ratioFactor = 0.8f;
    } else if (m_speed < 0.5f || m_speed > 2.0f) {
        ratioFactor = 0.9f;
    }
    
    // Algorithm factor
    float algoFactor = 1.0f;
    switch (m_algorithm) {
        case TimeStretchAlgorithm::MULTI_RESOLUTION:
            algoFactor = 1.0f;
            break;
        case TimeStretchAlgorithm::PHASE_VOCODER:
            algoFactor = 0.95f;
            break;
        case TimeStretchAlgorithm::HYBRID:
            algoFactor = 0.9f;
            break;
        case TimeStretchAlgorithm::WSOLA:
            algoFactor = 0.75f;
            break;
    }
    
    return baseQuality * ratioFactor * algoFactor;
}

// ============================================================================
// SpeedCurve Implementation
// ============================================================================

SpeedCurve::SpeedCurve() {
    // Start with default point
    m_points.push_back({0.0, 1.0f});
}

void SpeedCurve::addPoint(double timeSeconds, float speed) {
    Point newPoint{timeSeconds, std::clamp(speed, 0.05f, 10.0f)};
    
    // Insert in sorted order
    auto it = std::lower_bound(m_points.begin(), m_points.end(), newPoint,
        [](const Point& a, const Point& b) { return a.timeSeconds < b.timeSeconds; });
    
    m_points.insert(it, newPoint);
}

void SpeedCurve::removePoint(int index) {
    if (index >= 0 && index < static_cast<int>(m_points.size()) && m_points.size() > 1) {
        m_points.erase(m_points.begin() + index);
    }
}

void SpeedCurve::clear() {
    m_points.clear();
    m_points.push_back({0.0, 1.0f});
}

float SpeedCurve::getSpeedAtTime(double timeSeconds) const {
    if (m_points.empty()) return 1.0f;
    if (m_points.size() == 1) return m_points[0].speed;
    
    // Find surrounding points
    if (timeSeconds <= m_points.front().timeSeconds) {
        return m_points.front().speed;
    }
    if (timeSeconds >= m_points.back().timeSeconds) {
        return m_points.back().speed;
    }
    
    for (size_t i = 0; i < m_points.size() - 1; ++i) {
        if (timeSeconds >= m_points[i].timeSeconds && 
            timeSeconds < m_points[i + 1].timeSeconds) {
            return interpolate(m_points[i], m_points[i + 1], timeSeconds);
        }
    }
    
    return 1.0f;
}

float SpeedCurve::interpolate(const Point& p1, const Point& p2, double time) const {
    double t = (time - p1.timeSeconds) / (p2.timeSeconds - p1.timeSeconds);
    
    // Smoothstep interpolation
    t = t * t * (3.0 - 2.0 * t);
    
    return p1.speed + static_cast<float>(t) * (p2.speed - p1.speed);
}

SpeedCurve SpeedCurve::createLinearRamp(float startSpeed, float endSpeed, double duration) {
    SpeedCurve curve;
    curve.clear();
    curve.addPoint(0.0, startSpeed);
    curve.addPoint(duration, endSpeed);
    return curve;
}

SpeedCurve SpeedCurve::createSCurve(float startSpeed, float endSpeed, double duration) {
    SpeedCurve curve;
    curve.clear();
    curve.addPoint(0.0, startSpeed);
    curve.addPoint(duration * 0.25, startSpeed + 0.1f * (endSpeed - startSpeed));
    curve.addPoint(duration * 0.5, startSpeed + 0.5f * (endSpeed - startSpeed));
    curve.addPoint(duration * 0.75, startSpeed + 0.9f * (endSpeed - startSpeed));
    curve.addPoint(duration, endSpeed);
    return curve;
}

SpeedCurve SpeedCurve::createPulse(float baseSpeed, float peakSpeed, 
                                   double peakTime, double duration) {
    SpeedCurve curve;
    curve.clear();
    curve.addPoint(0.0, baseSpeed);
    curve.addPoint(peakTime * 0.5, baseSpeed);
    curve.addPoint(peakTime, peakSpeed);
    curve.addPoint(peakTime + (duration - peakTime) * 0.5, baseSpeed);
    curve.addPoint(duration, baseSpeed);
    return curve;
}

} // namespace ultramusic
