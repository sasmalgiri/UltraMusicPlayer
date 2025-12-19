/**
 * UltraMusicPlayer - AudioEngine.cpp
 * 
 * Implementation of the main audio engine using Oboe.
 */

#include "../include/AudioEngine.h"
#include "../include/PhaseVocoder.h"
#include "../include/TimeStretcher.h"
#include "../include/PitchShifter.h"
#include "../include/FormantPreserver.h"

#include <android/log.h>
#include <cmath>
#include <algorithm>

#define LOG_TAG "UltraMusic-Engine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace ultramusic {

AudioEngine::AudioEngine() {
    LOGI("AudioEngine created");
}

AudioEngine::~AudioEngine() {
    shutdown();
    LOGI("AudioEngine destroyed");
}

bool AudioEngine::initialize(int32_t sampleRate, int32_t channelCount) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (m_isInitialized.load()) {
        LOGI("Already initialized, reinitializing...");
        shutdown();
    }
    
    m_sampleRate = sampleRate;
    m_channelCount = channelCount;
    
    LOGI("Initializing with sample rate: %d, channels: %d", sampleRate, channelCount);
    
    // Initialize processing components
    try {
        // Phase vocoder for high-quality processing
        PhaseVocoderConfig pvConfig;
        pvConfig.fftSize = 4096;
        pvConfig.hopSize = 1024;
        pvConfig.phaseLocking = true;
        pvConfig.transientDetection = true;
        
        m_phaseVocoder = std::make_unique<PhaseVocoder>(sampleRate, channelCount);
        m_phaseVocoder->configure(pvConfig);
        
        // Time stretcher (0.05x - 10.0x range)
        m_timeStretcher = std::make_unique<TimeStretcher>(sampleRate, channelCount);
        m_timeStretcher->setAlgorithm(TimeStretchAlgorithm::PHASE_VOCODER);
        
        // Pitch shifter (-36 to +36 semitones)
        m_pitchShifter = std::make_unique<PitchShifter>(sampleRate, channelCount);
        m_pitchShifter->setPreserveFormants(true);
        
        // Formant preserver for natural vocals
        m_formantPreserver = std::make_unique<FormantPreserver>(sampleRate, 4096);
        
        LOGI("Processing components initialized");
        
    } catch (const std::exception& e) {
        LOGE("Failed to initialize processing: %s", e.what());
        return false;
    }
    
    // Create audio stream
    if (!createAudioStream()) {
        LOGE("Failed to create audio stream");
        return false;
    }
    
    m_isInitialized.store(true);
    LOGI("AudioEngine initialized successfully");
    
    return true;
}

void AudioEngine::shutdown() {
    LOGI("Shutting down AudioEngine");
    
    m_isPlaying.store(false);
    m_isInitialized.store(false);
    
    closeAudioStream();
    
    // Clear processing components
    m_phaseVocoder.reset();
    m_timeStretcher.reset();
    m_pitchShifter.reset();
    m_formantPreserver.reset();
    m_resampler.reset();
    
    // Clear audio buffer
    m_audioBuffer.clear();
    m_totalFrames = 0;
}

bool AudioEngine::createAudioStream() {
    oboe::AudioStreamBuilder builder;
    
    builder.setDirection(oboe::Direction::Output)
           ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
           ->setSharingMode(oboe::SharingMode::Exclusive)
           ->setFormat(oboe::AudioFormat::Float)
           ->setChannelCount(m_channelCount)
           ->setSampleRate(m_sampleRate)
           ->setCallback(this)
           ->setFramesPerCallback(512)  // Low latency buffer
           ->setUsage(oboe::Usage::Media)
           ->setContentType(oboe::ContentType::Music);
    
    oboe::Result result = builder.openStream(m_audioStream);
    
    if (result != oboe::Result::OK) {
        LOGE("Failed to open stream: %s", oboe::convertToText(result));
        return false;
    }
    
    LOGI("Audio stream created: %dHz, %d channels, buffer: %d frames",
         m_audioStream->getSampleRate(),
         m_audioStream->getChannelCount(),
         m_audioStream->getBufferSizeInFrames());
    
    return true;
}

void AudioEngine::closeAudioStream() {
    if (m_audioStream) {
        m_audioStream->stop();
        m_audioStream->close();
        m_audioStream.reset();
    }
}

bool AudioEngine::loadAudioFile(const std::string& filePath) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    LOGI("Loading audio file: %s", filePath.c_str());
    
    // TODO: Implement audio file loading using Android's MediaExtractor
    // or a library like FFmpeg, libsndfile, etc.
    // For now, this is a placeholder.
    
    // After loading:
    // - m_audioBuffer contains the decoded audio data
    // - m_totalFrames is the total number of frames
    // - Reset position
    m_currentPosition.store(0);
    
    return true;
}

bool AudioEngine::loadAudioData(const float* data, int64_t frameCount, 
                                int32_t sampleRate, int32_t channels) {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    LOGI("Loading raw audio data: %lld frames, %dHz, %d channels",
         (long long)frameCount, sampleRate, channels);
    
    // Resize buffer and copy data
    m_audioBuffer.resize(frameCount * channels);
    std::copy(data, data + frameCount * channels, m_audioBuffer.begin());
    
    m_totalFrames = frameCount;
    m_sampleRate = sampleRate;
    m_channelCount = channels;
    m_currentPosition.store(0);
    
    return true;
}

void AudioEngine::unloadAudio() {
    std::lock_guard<std::mutex> lock(m_mutex);
    
    stop();
    m_audioBuffer.clear();
    m_totalFrames = 0;
    m_currentPosition.store(0);
}

void AudioEngine::play() {
    if (!m_isInitialized.load() || m_audioBuffer.empty()) {
        LOGE("Cannot play: not initialized or no audio loaded");
        return;
    }
    
    if (m_audioStream && !m_isPlaying.load()) {
        oboe::Result result = m_audioStream->start();
        if (result == oboe::Result::OK) {
            m_isPlaying.store(true);
            LOGI("Playback started");
            
            if (m_callback) {
                m_callback->onPlaybackStateChanged(true);
            }
        } else {
            LOGE("Failed to start playback: %s", oboe::convertToText(result));
        }
    }
}

void AudioEngine::pause() {
    if (m_audioStream && m_isPlaying.load()) {
        m_audioStream->pause();
        m_isPlaying.store(false);
        LOGI("Playback paused");
        
        if (m_callback) {
            m_callback->onPlaybackStateChanged(false);
        }
    }
}

void AudioEngine::stop() {
    if (m_audioStream) {
        m_audioStream->stop();
        m_isPlaying.store(false);
        m_currentPosition.store(0);
        LOGI("Playback stopped");
        
        if (m_callback) {
            m_callback->onPlaybackStateChanged(false);
        }
    }
}

void AudioEngine::seekTo(int64_t framePosition) {
    int64_t clamped = std::max(0LL, std::min(framePosition, m_totalFrames));
    m_currentPosition.store(clamped);
    LOGI("Seek to frame: %lld", (long long)clamped);
}

void AudioEngine::seekToTime(double seconds) {
    int64_t frame = static_cast<int64_t>(seconds * m_sampleRate);
    seekTo(frame);
}

// ============================================================================
// Parameter Control
// ============================================================================

void AudioEngine::setSpeed(float speed) {
    // Extended range: 0.05x to 10.0x
    float clamped = std::clamp(speed, 0.05f, 10.0f);
    m_params.speed = clamped;
    
    if (m_timeStretcher) {
        m_timeStretcher->setSpeed(clamped);
    }
    
    LOGI("Speed set to: %.3f", clamped);
}

void AudioEngine::setPitchSemitones(float semitones) {
    // Extended range: -36 to +36 semitones
    float clamped = std::clamp(semitones, -36.0f, 36.0f);
    m_params.pitchSemitones = clamped;
    
    if (m_pitchShifter) {
        m_pitchShifter->setPitchSemitones(clamped);
    }
    
    LOGI("Pitch set to: %.2f semitones", clamped);
}

void AudioEngine::setPitchCents(float cents) {
    // Fine tuning: -100 to +100 cents
    float clamped = std::clamp(cents, -100.0f, 100.0f);
    m_params.pitchCents = clamped;
    
    if (m_pitchShifter) {
        m_pitchShifter->setPitchCents(clamped);
    }
}

void AudioEngine::setFormantShift(float shift) {
    m_params.formantShift = std::clamp(shift, -24.0f, 24.0f);
    
    if (m_pitchShifter) {
        m_pitchShifter->setFormantShift(shift);
    }
}

void AudioEngine::setPreserveFormants(bool preserve) {
    m_params.preserveFormants = preserve;
    
    if (m_pitchShifter) {
        m_pitchShifter->setPreserveFormants(preserve);
    }
}

void AudioEngine::setQualityMode(QualityMode mode) {
    m_params.quality = mode;
    updateProcessingChain();
}

void AudioEngine::setAlgorithm(Algorithm algorithm) {
    m_params.algorithm = algorithm;
    updateProcessingChain();
}

float AudioEngine::getTotalPitchShift() const {
    return m_params.pitchSemitones + (m_params.pitchCents / 100.0f);
}

// ============================================================================
// Loop Control
// ============================================================================

void AudioEngine::setLoopRegion(int64_t startFrame, int64_t endFrame) {
    m_loopRegion.startFrame = std::max(0LL, startFrame);
    m_loopRegion.endFrame = std::min(endFrame, m_totalFrames);
    LOGI("Loop region: %lld - %lld", (long long)m_loopRegion.startFrame, 
         (long long)m_loopRegion.endFrame);
}

void AudioEngine::enableLoop(bool enable) {
    m_loopRegion.enabled = enable;
    LOGI("Loop %s", enable ? "enabled" : "disabled");
}

void AudioEngine::clearLoop() {
    m_loopRegion.enabled = false;
    m_loopRegion.startFrame = 0;
    m_loopRegion.endFrame = 0;
}

// ============================================================================
// Time Queries
// ============================================================================

double AudioEngine::getCurrentTimeSeconds() const {
    return static_cast<double>(m_currentPosition.load()) / m_sampleRate;
}

double AudioEngine::getTotalTimeSeconds() const {
    return static_cast<double>(m_totalFrames) / m_sampleRate;
}

// ============================================================================
// Audio Callback
// ============================================================================

oboe::DataCallbackResult AudioEngine::onAudioReady(
        oboe::AudioStream* audioStream,
        void* audioData,
        int32_t numFrames) {
    
    float* output = static_cast<float*>(audioData);
    
    if (!m_isPlaying.load() || m_audioBuffer.empty()) {
        // Output silence
        std::fill(output, output + numFrames * m_channelCount, 0.0f);
        return oboe::DataCallbackResult::Continue;
    }
    
    // Process audio
    processAudioBlock(output, numFrames);
    
    return oboe::DataCallbackResult::Continue;
}

void AudioEngine::processAudioBlock(float* output, int32_t numFrames) {
    int64_t position = m_currentPosition.load();
    
    // Temporary buffer for processing
    std::vector<float> tempBuffer(numFrames * m_channelCount);
    
    // Read from source buffer
    for (int32_t i = 0; i < numFrames; ++i) {
        int64_t srcPos = position + i;
        
        // Handle loop
        if (m_loopRegion.enabled && srcPos >= m_loopRegion.endFrame) {
            srcPos = m_loopRegion.startFrame + 
                     ((srcPos - m_loopRegion.startFrame) % 
                      (m_loopRegion.endFrame - m_loopRegion.startFrame));
        }
        
        // Check bounds
        if (srcPos >= m_totalFrames) {
            // End of audio - fill with silence
            for (int32_t ch = 0; ch < m_channelCount; ++ch) {
                tempBuffer[i * m_channelCount + ch] = 0.0f;
            }
            continue;
        }
        
        // Copy from source
        for (int32_t ch = 0; ch < m_channelCount; ++ch) {
            tempBuffer[i * m_channelCount + ch] = 
                m_audioBuffer[srcPos * m_channelCount + ch];
        }
    }
    
    // Apply time stretching if speed != 1.0
    if (m_timeStretcher && std::abs(m_params.speed - 1.0f) > 0.001f) {
        // Time stretcher processes and outputs different frame count
        // For real implementation, need proper buffering
        m_timeStretcher->process(tempBuffer.data(), numFrames,
                                 output, numFrames);
    } else {
        // Copy directly if no time stretch
        std::copy(tempBuffer.begin(), tempBuffer.end(), output);
    }
    
    // Apply pitch shifting if pitch != 0
    if (m_pitchShifter && std::abs(getTotalPitchShift()) > 0.001f) {
        m_pitchShifter->process(output, numFrames, output, numFrames);
    }
    
    // Update position (accounting for speed)
    int64_t framesAdvanced = static_cast<int64_t>(numFrames * m_params.speed);
    m_currentPosition.store(position + framesAdvanced);
    
    // Notify position change
    if (m_callback) {
        m_callback->onPlaybackPositionChanged(m_currentPosition.load());
    }
}

void AudioEngine::updateProcessingChain() {
    // Update processing parameters based on quality mode
    switch (m_params.quality) {
        case QualityMode::ULTRA_HIGH:
            if (m_timeStretcher) {
                m_timeStretcher->setAlgorithm(TimeStretchAlgorithm::MULTI_RESOLUTION);
                m_timeStretcher->setQuality(100);
            }
            break;
            
        case QualityMode::HIGH:
            if (m_timeStretcher) {
                m_timeStretcher->setAlgorithm(TimeStretchAlgorithm::PHASE_VOCODER);
                m_timeStretcher->setQuality(80);
            }
            break;
            
        case QualityMode::BALANCED:
            if (m_timeStretcher) {
                m_timeStretcher->setAlgorithm(TimeStretchAlgorithm::HYBRID);
                m_timeStretcher->setQuality(60);
            }
            break;
            
        case QualityMode::PERFORMANCE:
            if (m_timeStretcher) {
                m_timeStretcher->setAlgorithm(TimeStretchAlgorithm::WSOLA);
                m_timeStretcher->setQuality(40);
            }
            break;
            
        case QualityMode::VOICE:
            if (m_timeStretcher) {
                m_timeStretcher->setMode(StretchMode::VOICE);
            }
            break;
            
        case QualityMode::INSTRUMENT:
            if (m_timeStretcher) {
                m_timeStretcher->setMode(StretchMode::SOLO);
            }
            break;
            
        case QualityMode::PERCUSSION:
            if (m_timeStretcher) {
                m_timeStretcher->setMode(StretchMode::DRUMS);
            }
            break;
    }
}

float AudioEngine::calculatePitchRatio() const {
    float totalSemitones = getTotalPitchShift();
    return std::pow(2.0f, totalSemitones / 12.0f);
}

float AudioEngine::detectBPM() {
    // TODO: Implement BPM detection algorithm
    // Uses onset detection and autocorrelation
    return 120.0f; // Placeholder
}

void AudioEngine::setTargetBPM(float bpm) {
    float detectedBPM = detectBPM();
    if (detectedBPM > 0) {
        float newSpeed = bpm / detectedBPM;
        setSpeed(newSpeed);
    }
}

bool AudioEngine::exportToFile(const std::string& outputPath, const std::string& format) {
    // TODO: Implement export functionality
    LOGI("Exporting to: %s (format: %s)", outputPath.c_str(), format.c_str());
    return true;
}

} // namespace ultramusic
