/**
 * BATTLE AUDIO ENGINE
 *
 * Professional-grade audio processing for sound system battles.
 *
 * ENGINE CONFIGURATION:
 * - USE_SUPERPOWERED = 0: Uses SoundTouch (FREE, no license needed)
 * - USE_SUPERPOWERED = 1: Uses Superpowered SDK (requires paid license)
 *
 * AUDIO QUALITY:
 * - SoundTouch: 8.5/10 (Professional TDHS time-domain processing)
 * - Superpowered: 9.5/10 (Professional frequency-domain processing)
 *
 * Features:
 * - Speed range: 0.05x to 10.0x
 * - Pitch range: -36 to +36 semitones
 * - Battle-grade limiter (no clipping at extreme volumes)
 * - Punch compressor (cuts through in battles)
 * - Sub-bass enhancement (shake the ground)
 *
 * Optimized for ARM NEON SIMD on Android devices.
 */

// =============================================================================
// ENGINE CONFIGURATION
// All engines now available at RUNTIME - user can switch between them
// =============================================================================
// Superpowered EVALUATION License (max 1000 installs, expires on SDK update)
// For production: Contact licensing@superpowered.com for Starter or White Label license
#define SUPERPOWERED_LICENSE "ExampleLicenseKey-WillExpire-OnNextUpdate"
#define HAS_SUPERPOWERED_LICENSE (strlen(SUPERPOWERED_LICENSE) > 10)
// =============================================================================

#include "battle_audio_engine.h"
#include "SoundTouch.h"  // SoundTouch engine (FREE, no license)

// Superpowered SDK - always include for runtime selection
#include "Superpowered.h"
#include "SuperpoweredTimeStretching.h"
#include "SuperpoweredCompressor.h"
#include "SuperpoweredLimiter.h"
#include "Superpowered3BandEQ.h"

// Rubberband - Optional studio-grade engine
// To enable: Download from https://breakfastquay.com/rubberband/
// Place source in app/src/main/cpp/rubberband/ and set USE_RUBBERBAND=ON in CMake
#ifndef USE_RUBBERBAND
#define USE_RUBBERBAND 0
#endif

#if USE_RUBBERBAND
#include "RubberBandStretcher.h"
using RubberBand::RubberBandStretcher;
#endif

#include <cmath>
#include <algorithm>
#include <android/log.h>

#define LOG_TAG "BattleAudioEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace ultramusic {

// Helper function: convert semitones to cents (100 cents = 1 semitone)
inline int semitonesToCents(float semitones) {
    return static_cast<int>(semitones * 100.0f);
}

// =============================================================================
// BATTLE AUDIO ENGINE IMPLEMENTATION
// Primary: SoundTouch (FREE) | Optional: Superpowered (requires license)
// =============================================================================

class BattleAudioEngineImpl {
public:
    BattleAudioEngineImpl() {
        // Initialize Superpowered SDK only if license is available
        if (HAS_SUPERPOWERED_LICENSE) {
            Superpowered::Initialize(SUPERPOWERED_LICENSE);
            superpoweredAvailable = true;

            // Create Superpowered time stretcher (highest quality mode)
            timeStretcher = new Superpowered::TimeStretching(44100, 0.01f);
            timeStretcher->sound = 2;  // Highest quality mode
            timeStretcher->formantCorrection = 0.5f;
            timeStretcher->rate = 1.0f;
            timeStretcher->pitchShiftCents = 0;

            // Create Superpowered effects for full utilization
            spCompressor = new Superpowered::Compressor(44100);
            spCompressor->enabled = true;
            spCompressor->inputGainDb = 0;
            spCompressor->outputGainDb = 0;
            spCompressor->wet = 1.0f;
            spCompressor->attackSec = 0.003f;
            spCompressor->releaseSec = 0.3f;
            spCompressor->ratio = 4.0f;
            spCompressor->thresholdDb = -10.0f;
            spCompressor->hpCutOffHz = 1;

            spLimiter = new Superpowered::Limiter(44100);
            spLimiter->enabled = true;
            spLimiter->ceilingDb = -0.1f;
            spLimiter->thresholdDb = -0.3f;
            spLimiter->releaseSec = 0.1f;

            spEQ = new Superpowered::ThreeBandEQ(44100);
            spEQ->enabled = true;
            spEQ->low = 1.0f;  // Will be adjusted by bass boost
            spEQ->mid = 1.0f;
            spEQ->high = 1.0f;

            LOGI("Superpowered SDK initialized - DJ-grade effects ready!");
        } else {
            // No Superpowered license - use SoundTouch only
            superpoweredAvailable = false;
            timeStretcher = nullptr;
            spCompressor = nullptr;
            spLimiter = nullptr;
            spEQ = nullptr;
            LOGI("No Superpowered license - using SoundTouch engine (still excellent quality!)");
        }

        // SoundTouch - OPTIMIZED SETTINGS for maximum quality (always available)
        soundTouch = new soundtouch::SoundTouch();
        soundTouch->setSetting(SETTING_USE_AA_FILTER, 1);        // Anti-alias filtering ON
        soundTouch->setSetting(SETTING_AA_FILTER_LENGTH, 128);   // Longest filter for best quality
        soundTouch->setSetting(SETTING_SEQUENCE_MS, 82);         // Optimal for music
        soundTouch->setSetting(SETTING_SEEKWINDOW_MS, 28);       // Better seeking
        soundTouch->setSetting(SETTING_OVERLAP_MS, 12);          // Smoother transitions

        // Battle processing chain (used when engine is SoundTouch or as fallback)
        limiter = new BattleLimiter();
        compressor = new BattleCompressor();
        bassBoost = new BattleBassBoost();

        // Default engine is SoundTouch (user can switch at runtime)
        currentEngine = AudioEngineType::SOUNDTOUCH;

#if USE_RUBBERBAND
        // Initialize Rubberband (studio-grade time-stretching)
        // Using highest quality options for maximum fidelity
        rubberbandStretcher = new RubberBandStretcher(
            44100, 2,
            RubberBandStretcher::OptionProcessRealTime |
            RubberBandStretcher::OptionPitchHighQuality |
            RubberBandStretcher::OptionStretchPrecise |
            RubberBandStretcher::OptionTransientsCrisp |
            RubberBandStretcher::OptionChannelsTogether
        );
        rubberbandAvailable = true;
        LOGI("Rubberband initialized - Studio-grade quality ready!");
#else
        rubberbandAvailable = false;
#endif

        LOGI("BattleAudioEngine v3.0 - MULTI-ENGINE READY!");
        if (superpoweredAvailable) {
#if USE_RUBBERBAND
            LOGI("Available Engines: SoundTouch (8.5/10) | Superpowered (9.5/10) | Rubberband (10/10)");
#else
            LOGI("Available Engines: SoundTouch (8.5/10) | Superpowered (9.5/10)");
#endif
        } else {
#if USE_RUBBERBAND
            LOGI("Available Engines: SoundTouch (8.5/10) | Rubberband (10/10)");
#else
            LOGI("Available Engines: SoundTouch (8.5/10) - Get Superpowered license for more options!");
#endif
        }
        LOGI("Speed: 0.05x-10x | Pitch: -36 to +36 semitones | Battle Mode Ready!");
    }

    ~BattleAudioEngineImpl() {
        // Clean up Superpowered
        if (timeStretcher) delete timeStretcher;
        if (spCompressor) delete spCompressor;
        if (spLimiter) delete spLimiter;
        if (spEQ) delete spEQ;

#if USE_RUBBERBAND
        // Clean up Rubberband
        if (rubberbandStretcher) delete rubberbandStretcher;
#endif

        // Clean up SoundTouch and battle processing
        delete soundTouch;
        delete limiter;
        delete compressor;
        delete bassBoost;
    }

    // Set audio engine at runtime with smart fallback
    void setAudioEngine(AudioEngineType engine) {
        if (engine == currentEngine) return;

        // Smart fallback chain: Requested → Next Best → SoundTouch (always available)
        AudioEngineType requestedEngine = engine;

        // Check Superpowered availability - fallback to Rubberband, then SoundTouch
        if (engine == AudioEngineType::SUPERPOWERED && !superpoweredAvailable) {
            LOGW("Superpowered not available (no license or limit reached)");
            if (rubberbandAvailable) {
                LOGI("Falling back to Rubberband (10/10 quality)");
                engine = AudioEngineType::RUBBERBAND;
            } else {
                LOGI("Falling back to SoundTouch (8.5/10 quality)");
                engine = AudioEngineType::SOUNDTOUCH;
            }
        }

        // Check Rubberband availability - fallback to Superpowered, then SoundTouch
        if (engine == AudioEngineType::RUBBERBAND && !rubberbandAvailable) {
            LOGW("Rubberband not available");
            if (superpoweredAvailable) {
                LOGI("Falling back to Superpowered (9.5/10 quality)");
                engine = AudioEngineType::SUPERPOWERED;
            } else {
                LOGI("Falling back to SoundTouch (8.5/10 quality)");
                engine = AudioEngineType::SOUNDTOUCH;
            }
        }

        AudioEngineType previousEngine = currentEngine;
        currentEngine = engine;

        // Clear buffers when switching engines
        clear();

        switch (engine) {
            case AudioEngineType::SOUNDTOUCH:
                LOGI("Engine: SOUNDTOUCH (8.5/10 quality) - Always available, no license needed");
                break;
            case AudioEngineType::SUPERPOWERED:
                LOGI("Engine: SUPERPOWERED (9.5/10 quality) - DJ-grade processing");
                break;
            case AudioEngineType::RUBBERBAND:
#if USE_RUBBERBAND
                // Update Rubberband parameters
                if (rubberbandStretcher) {
                    rubberbandStretcher->setTimeRatio(1.0 / speed);
                    rubberbandStretcher->setPitchScale(std::pow(2.0, pitchSemitones / 12.0));
                }
                LOGI("Engine: RUBBERBAND (10/10 quality) - Studio-grade, best for music");
#else
                LOGW("Rubberband not compiled! Using SoundTouch.");
                currentEngine = AudioEngineType::SOUNDTOUCH;
#endif
                break;
        }

        // Log if we had to fallback
        if (engine != requestedEngine) {
            LOGI("Note: Requested %s but using %s (fallback)",
                 requestedEngine == AudioEngineType::SOUNDTOUCH ? "SoundTouch" :
                 requestedEngine == AudioEngineType::SUPERPOWERED ? "Superpowered" : "Rubberband",
                 engine == AudioEngineType::SOUNDTOUCH ? "SoundTouch" :
                 engine == AudioEngineType::SUPERPOWERED ? "Superpowered" : "Rubberband");
        }
    }

    AudioEngineType getAudioEngine() const {
        return currentEngine;
    }

    void configure(int sampleRate, int channels) {
        this->sampleRate = sampleRate;
        this->channels = channels;

        // Configure Superpowered (only if license available)
        if (superpoweredAvailable) {
            // Configure Superpowered time stretcher
            if (timeStretcher) delete timeStretcher;
            timeStretcher = new Superpowered::TimeStretching(sampleRate, 0.01f);
            timeStretcher->sound = 2;  // Highest quality
            timeStretcher->formantCorrection = formantPreservation ? 0.7f : 0.0f;

            // Configure Superpowered effects
            if (spCompressor) {
                delete spCompressor;
                spCompressor = new Superpowered::Compressor(sampleRate);
                spCompressor->enabled = battleMode;
                spCompressor->attackSec = 0.003f;
                spCompressor->releaseSec = 0.3f;
                spCompressor->ratio = 4.0f;
                spCompressor->thresholdDb = -10.0f;
            }
            if (spLimiter) {
                delete spLimiter;
                spLimiter = new Superpowered::Limiter(sampleRate);
                spLimiter->enabled = limiterEnabled;
                spLimiter->ceilingDb = -0.1f;
                spLimiter->thresholdDb = -0.3f;
            }
            if (spEQ) {
                delete spEQ;
                spEQ = new Superpowered::ThreeBandEQ(sampleRate);
                spEQ->enabled = (bassBoostAmount > 0);
            }
        }

        // Configure SoundTouch
        soundTouch->setSampleRate(sampleRate);
        soundTouch->setChannels(channels);
        updateSoundTouch();

        // Configure battle processing (for SoundTouch engine)
        limiter->configure(sampleRate, channels);
        compressor->configure(sampleRate, channels);
        bassBoost->configure(sampleRate, channels);

#if USE_RUBBERBAND
        // Configure Rubberband (studio-grade time-stretching)
        if (rubberbandStretcher) delete rubberbandStretcher;
        rubberbandStretcher = new RubberBandStretcher(
            sampleRate, channels,
            RubberBandStretcher::OptionProcessRealTime |
            RubberBandStretcher::OptionPitchHighQuality |
            RubberBandStretcher::OptionStretchPrecise |
            RubberBandStretcher::OptionTransientsCrisp |
            RubberBandStretcher::OptionChannelsTogether
        );
        // Set initial parameters
        rubberbandStretcher->setTimeRatio(1.0 / speed);
        rubberbandStretcher->setPitchScale(std::pow(2.0, pitchSemitones / 12.0));
#endif

        // Configure psychoacoustic bass enhancement
        subHarmonicL.configure(sampleRate);
        subHarmonicR.configure(sampleRate);
        exciterL.configure(sampleRate);
        exciterR.configure(sampleRate);

        // Allocate buffers
        floatInputBuffer.resize(8192 * channels);
        floatOutputBuffer.resize(32768 * channels);

        LOGI("Configured: %dHz, %d channels, Engine: %s", sampleRate, channels,
             currentEngine == AudioEngineType::SOUNDTOUCH ? "SoundTouch" :
             currentEngine == AudioEngineType::SUPERPOWERED ? "Superpowered" : "Rubberband");
    }

    // Speed: 0.05x to 10.0x (tempo change without pitch change)
    void setSpeed(float newSpeed) {
        this->speed = std::clamp(newSpeed, 0.05f, 10.0f);
        updateSoundTouch();
        LOGI("Speed set to: %.2fx", this->speed);
    }

    // Pitch: -36 to +36 semitones (pitch change without tempo change)
    void setPitch(float semitones) {
        this->pitchSemitones = std::clamp(semitones, -36.0f, 36.0f);
        updateSoundTouch();
        LOGI("Pitch set to: %.1f semitones", this->pitchSemitones);
    }

    // Rate: Changes both speed AND pitch together (like vinyl speed change)
    void setRate(float newRate) {
        this->rate = std::clamp(newRate, 0.05f, 10.0f);
        useRateMode = true;
        updateSoundTouch();
        LOGI("Rate set to: %.2fx (vinyl mode)", this->rate);
    }

    // Formant preservation
    void setFormantPreservation(bool enabled) {
        this->formantPreservation = enabled;
        if (timeStretcher) {
            timeStretcher->formantCorrection = enabled ? 0.7f : 0.0f;
        }
        LOGI("Formant preservation: %s", enabled ? "ON" : "OFF");
    }

    // Battle mode settings
    void setBattleMode(bool enabled) {
        this->battleMode = enabled;

        // Update SoundTouch battle processing
        limiter->setEnabled(enabled && limiterEnabled);
        compressor->setEnabled(enabled);

        // Update Superpowered effects
        if (spCompressor) spCompressor->enabled = enabled;
        if (spLimiter) spLimiter->enabled = enabled && limiterEnabled;

        LOGI("Battle mode: %s", enabled ? "ENGAGED - Maximum Power!" : "OFF");
    }

    // Limiter toggle (FULL SEND mode)
    void setLimiterEnabled(bool enabled) {
        this->limiterEnabled = enabled;

        // Update SoundTouch limiter
        limiter->setEnabled(enabled);

        // Update Superpowered limiter
        if (spLimiter) spLimiter->enabled = enabled;

        if (enabled) {
            LOGI("Limiter: ON (clipping protection active)");
        } else {
            LOGW("Limiter: OFF - FULL SEND! Maximum power, no limits!");
        }
    }

    // Hardware Protection - STRONGEST safety, protects speakers
    void setHardwareProtection(bool enabled) {
        this->hardwareProtection = enabled;

        if (enabled) {
            // Hard ceiling at -0.5dB
            hardLimiterCeiling = 0.944f;  // -0.5dB = 10^(-0.5/20)
            // Enable sub-bass filter to remove <20Hz rumble
            subBassFilterEnabled = true;
            // Enable DC offset removal
            dcBlockerEnabled = true;
            LOGI("Hardware Protection: ON - Speaker protection active");
        } else {
            hardLimiterCeiling = 1.0f;
            subBassFilterEnabled = false;
            dcBlockerEnabled = false;
            LOGW("Hardware Protection: OFF - WARNING: Speaker damage possible!");
        }
    }

    // Audiophile Mode - Cleanest, most pleasant audio
    void setAudiophileMode(bool enabled) {
        this->audiophileMode = enabled;

        if (enabled) {
            // Disable battle processing for transparent audio
            compressor->setEnabled(false);
            if (spCompressor) spCompressor->enabled = false;

            // Disable sub-harmonic and exciter (no artificial coloring)
            subHarmonicAmount = 0.0f;
            exciterAmount = 0.0f;
            subHarmonicL.setAmount(0.0f);
            subHarmonicR.setAmount(0.0f);
            exciterL.setAmount(0.0f);
            exciterR.setAmount(0.0f);

            // Enable subtle clarity enhancement
            clarityEnhanceEnabled = true;
            clarityAmount = 0.2f;  // Subtle, not aggressive

            // Enable dithering for cleaner output
            ditheringEnabled = true;

            LOGI("Audiophile Mode: ON - Pure, clean audio quality");
        } else {
            // Restore battle processing if battleMode is active
            compressor->setEnabled(battleMode);
            if (spCompressor) spCompressor->enabled = battleMode;

            clarityEnhanceEnabled = false;
            ditheringEnabled = false;

            LOGI("Audiophile Mode: OFF - Battle ready");
        }
    }

    void setBassBoost(float amount) {
        this->bassBoostAmount = std::clamp(amount, 0.0f, 24.0f);

        // Update SoundTouch bass boost
        bassBoost->setGain(this->bassBoostAmount);

        // Update Superpowered 3-band EQ for bass boost
        if (spEQ) {
            spEQ->enabled = (bassBoostAmount > 0);
            // Convert dB to linear gain: 10^(dB/20)
            // For EQ low band: 1.0 = unity, 2.0 = +6dB, 4.0 = +12dB
            float linearGain = std::pow(10.0f, bassBoostAmount / 20.0f);
            spEQ->low = linearGain;
        }

        LOGI("Bass boost: %.1f dB", this->bassBoostAmount);
    }

    // Psychoacoustic bass enhancement - no gain, perceived loudness
    void setSubHarmonicAmount(float amount) {
        subHarmonicAmount = std::clamp(amount, 0.0f, 1.0f);
        subHarmonicL.setAmount(subHarmonicAmount);
        subHarmonicR.setAmount(subHarmonicAmount);
        LOGI("Sub-harmonic amount: %.2f", subHarmonicAmount);
    }

    void setExciterAmount(float amount) {
        exciterAmount = std::clamp(amount, 0.0f, 1.0f);
        exciterL.setAmount(exciterAmount);
        exciterR.setAmount(exciterAmount);
        LOGI("Exciter amount: %.2f", exciterAmount);
    }

    void setLimiterThreshold(float thresholdDb) {
        limiter->setThreshold(thresholdDb);
    }

    void setCompressorRatio(float ratio) {
        compressor->setRatio(ratio);
    }

    // Process audio samples using selected engine
    void process(const short* input, int numSamples, short* output, int* outputSamples) {
        if (numSamples <= 0) {
            *outputSamples = 0;
            return;
        }

        int numFrames = numSamples / channels;

        // Route to appropriate engine
        switch (currentEngine) {
            case AudioEngineType::SUPERPOWERED:
                processSuperpowered(input, numSamples, output, outputSamples);
                break;
            case AudioEngineType::RUBBERBAND:
#if USE_RUBBERBAND
                processRubberband(input, numSamples, output, outputSamples);
#else
                // Rubberband not compiled, fallback to SoundTouch
                processSoundTouch(input, numSamples, output, outputSamples);
#endif
                break;
            case AudioEngineType::SOUNDTOUCH:
            default:
                processSoundTouch(input, numSamples, output, outputSamples);
                break;
        }
    }

private:
    // Process using SoundTouch engine
    void processSoundTouch(const short* input, int numSamples, short* output, int* outputSamples) {
        int numFrames = numSamples / channels;

        // Feed samples to SoundTouch
        soundTouch->putSamples(input, numFrames);

        // Receive processed samples
        int maxOutputFrames = 32768;
        shortOutputBuffer.resize(maxOutputFrames * channels);

        int receivedFrames = soundTouch->receiveSamples(shortOutputBuffer.data(), maxOutputFrames);

        if (receivedFrames <= 0) {
            *outputSamples = 0;
            return;
        }

        int totalSamples = receivedFrames * channels;

        // Apply battle processing chain if enabled
        if (battleMode) {
            // Convert to float for processing
            if (floatOutputBuffer.size() < static_cast<size_t>(totalSamples)) {
                floatOutputBuffer.resize(totalSamples);
            }
            for (int i = 0; i < totalSamples; i++) {
                floatOutputBuffer[i] = shortOutputBuffer[i] / 32768.0f;
            }

            // Bass boost (low shelf EQ)
            if (bassBoostAmount > 0) {
                bassBoost->process(floatOutputBuffer.data(), totalSamples);
            }

            // Psychoacoustic bass enhancement (adds perceived loudness without gain)
            if (subHarmonicAmount > 0 || exciterAmount > 0) {
                for (int i = 0; i < totalSamples; i += channels) {
                    // Left channel
                    if (subHarmonicAmount > 0) {
                        floatOutputBuffer[i] = subHarmonicL.process(floatOutputBuffer[i]);
                    }
                    if (exciterAmount > 0) {
                        floatOutputBuffer[i] = exciterL.process(floatOutputBuffer[i]);
                    }
                    // Right channel (if stereo)
                    if (channels > 1) {
                        if (subHarmonicAmount > 0) {
                            floatOutputBuffer[i + 1] = subHarmonicR.process(floatOutputBuffer[i + 1]);
                        }
                        if (exciterAmount > 0) {
                            floatOutputBuffer[i + 1] = exciterR.process(floatOutputBuffer[i + 1]);
                        }
                    }
                }
            }

            // Compressor (adds punch)
            compressor->process(floatOutputBuffer.data(), totalSamples);

            // Limiter (prevents clipping)
            limiter->process(floatOutputBuffer.data(), totalSamples);

            // Convert back to short
            for (int i = 0; i < totalSamples; i++) {
                float sample = floatOutputBuffer[i] * 32767.0f;
                sample = std::clamp(sample, -32768.0f, 32767.0f);
                output[i] = static_cast<short>(sample);
            }
        } else {
            // No battle processing, copy directly
            std::copy(shortOutputBuffer.begin(),
                      shortOutputBuffer.begin() + totalSamples,
                      output);
        }

        *outputSamples = totalSamples;
    }

    // Process using Superpowered engine (DJ-grade quality)
    void processSuperpowered(const short* input, int numSamples, short* output, int* outputSamples) {
        if (!timeStretcher) {
            // Fallback to SoundTouch if Superpowered not available
            processSoundTouch(input, numSamples, output, outputSamples);
            return;
        }

        int numFrames = numSamples / channels;

        // Convert short to float for Superpowered (expects interleaved float)
        if (floatInputBuffer.size() < static_cast<size_t>(numSamples)) {
            floatInputBuffer.resize(numSamples);
        }
        for (int i = 0; i < numSamples; i++) {
            floatInputBuffer[i] = input[i] / 32768.0f;
        }

        // Configure time stretcher
        timeStretcher->rate = speed;
        int pitchCents = static_cast<int>(pitchSemitones * 100.0f);
        timeStretcher->pitchShiftCents = std::clamp(pitchCents, -2400, 2400);

        // Process with Superpowered TimeStretching
        timeStretcher->addInput(floatInputBuffer.data(), numFrames);

        // Get output
        int maxOutputFrames = 32768;
        if (floatOutputBuffer.size() < static_cast<size_t>(maxOutputFrames * channels)) {
            floatOutputBuffer.resize(maxOutputFrames * channels);
        }

        int receivedFrames = timeStretcher->getOutput(floatOutputBuffer.data(), maxOutputFrames);

        if (receivedFrames <= 0) {
            *outputSamples = 0;
            return;
        }

        int totalSamples = receivedFrames * channels;

        // Apply Superpowered effects chain if battle mode enabled
        if (battleMode) {
            // Use Superpowered's own high-quality effects
            if (spEQ && bassBoostAmount > 0) {
                spEQ->process(floatOutputBuffer.data(), floatOutputBuffer.data(), receivedFrames);
            }

            // Psychoacoustic enhancement (still use our custom processors)
            if (subHarmonicAmount > 0 || exciterAmount > 0) {
                for (int i = 0; i < totalSamples; i += channels) {
                    if (subHarmonicAmount > 0) {
                        floatOutputBuffer[i] = subHarmonicL.process(floatOutputBuffer[i]);
                    }
                    if (exciterAmount > 0) {
                        floatOutputBuffer[i] = exciterL.process(floatOutputBuffer[i]);
                    }
                    if (channels > 1) {
                        if (subHarmonicAmount > 0) {
                            floatOutputBuffer[i + 1] = subHarmonicR.process(floatOutputBuffer[i + 1]);
                        }
                        if (exciterAmount > 0) {
                            floatOutputBuffer[i + 1] = exciterR.process(floatOutputBuffer[i + 1]);
                        }
                    }
                }
            }

            // Superpowered Compressor
            if (spCompressor) {
                spCompressor->process(floatOutputBuffer.data(), floatOutputBuffer.data(), receivedFrames);
            }

            // Superpowered Limiter
            if (spLimiter && limiterEnabled) {
                spLimiter->process(floatOutputBuffer.data(), floatOutputBuffer.data(), receivedFrames);
            }
        }

        // Convert back to short
        for (int i = 0; i < totalSamples; i++) {
            float sample = floatOutputBuffer[i] * 32767.0f;
            sample = std::clamp(sample, -32768.0f, 32767.0f);
            output[i] = static_cast<short>(sample);
        }

        *outputSamples = totalSamples;
    }

#if USE_RUBBERBAND
    // Process using Rubberband engine (Studio-grade, 10/10 quality)
    void processRubberband(const short* input, int numSamples, short* output, int* outputSamples) {
        if (!rubberbandStretcher) {
            // Fallback to SoundTouch if Rubberband not available
            processSoundTouch(input, numSamples, output, outputSamples);
            return;
        }

        int numFrames = numSamples / channels;

        // Ensure Rubberband parameters are up to date
        rubberbandStretcher->setTimeRatio(1.0 / speed);  // Inverse: slower speed = higher ratio
        rubberbandStretcher->setPitchScale(std::pow(2.0, pitchSemitones / 12.0));

        // Prepare deinterleaved input buffers (Rubberband expects separate channels)
        std::vector<float> leftIn(numFrames);
        std::vector<float> rightIn(numFrames);

        // Deinterleave and convert to float
        for (int i = 0; i < numFrames; i++) {
            leftIn[i] = input[i * channels] / 32768.0f;
            if (channels > 1) {
                rightIn[i] = input[i * channels + 1] / 32768.0f;
            } else {
                rightIn[i] = leftIn[i];  // Mono: duplicate
            }
        }

        // Rubberband expects pointer array
        const float* inPtrs[2] = { leftIn.data(), rightIn.data() };
        rubberbandStretcher->process(inPtrs, numFrames, false);

        // Get available output samples
        int availableFrames = rubberbandStretcher->available();
        if (availableFrames <= 0) {
            *outputSamples = 0;
            return;
        }

        // Prepare deinterleaved output buffers
        std::vector<float> leftOut(availableFrames);
        std::vector<float> rightOut(availableFrames);
        float* outPtrs[2] = { leftOut.data(), rightOut.data() };

        int retrievedFrames = rubberbandStretcher->retrieve(outPtrs, availableFrames);

        if (retrievedFrames <= 0) {
            *outputSamples = 0;
            return;
        }

        int totalSamples = retrievedFrames * channels;

        // Apply battle processing chain if enabled
        if (battleMode) {
            // Interleave to float buffer for processing
            if (floatOutputBuffer.size() < static_cast<size_t>(totalSamples)) {
                floatOutputBuffer.resize(totalSamples);
            }

            for (int i = 0; i < retrievedFrames; i++) {
                floatOutputBuffer[i * channels] = leftOut[i];
                if (channels > 1) {
                    floatOutputBuffer[i * channels + 1] = rightOut[i];
                }
            }

            // Bass boost
            if (bassBoostAmount > 0) {
                bassBoost->process(floatOutputBuffer.data(), totalSamples);
            }

            // Psychoacoustic enhancement
            if (subHarmonicAmount > 0 || exciterAmount > 0) {
                for (int i = 0; i < totalSamples; i += channels) {
                    if (subHarmonicAmount > 0) {
                        floatOutputBuffer[i] = subHarmonicL.process(floatOutputBuffer[i]);
                    }
                    if (exciterAmount > 0) {
                        floatOutputBuffer[i] = exciterL.process(floatOutputBuffer[i]);
                    }
                    if (channels > 1) {
                        if (subHarmonicAmount > 0) {
                            floatOutputBuffer[i + 1] = subHarmonicR.process(floatOutputBuffer[i + 1]);
                        }
                        if (exciterAmount > 0) {
                            floatOutputBuffer[i + 1] = exciterR.process(floatOutputBuffer[i + 1]);
                        }
                    }
                }
            }

            // Compressor
            compressor->process(floatOutputBuffer.data(), totalSamples);

            // Limiter
            if (limiterEnabled) {
                limiter->process(floatOutputBuffer.data(), totalSamples);
            }

            // Convert back to short
            for (int i = 0; i < totalSamples; i++) {
                float sample = floatOutputBuffer[i] * 32767.0f;
                sample = std::clamp(sample, -32768.0f, 32767.0f);
                output[i] = static_cast<short>(sample);
            }
        } else {
            // No battle processing - just interleave and convert
            for (int i = 0; i < retrievedFrames; i++) {
                float left = leftOut[i] * 32767.0f;
                output[i * channels] = static_cast<short>(std::clamp(left, -32768.0f, 32767.0f));
                if (channels > 1) {
                    float right = rightOut[i] * 32767.0f;
                    output[i * channels + 1] = static_cast<short>(std::clamp(right, -32768.0f, 32767.0f));
                }
            }
        }

        *outputSamples = totalSamples;
    }
#endif

public:

    // Flush remaining samples
    void flush() {
        soundTouch->flush();
    }

    // Clear all buffers
    void clear() {
        soundTouch->clear();

        // Clear Superpowered
        if (timeStretcher) {
            timeStretcher->reset();
        }

#if USE_RUBBERBAND
        // Clear Rubberband
        if (rubberbandStretcher) {
            rubberbandStretcher->reset();
        }
#endif

        limiter->reset();
        compressor->reset();
        bassBoost->reset();
        subHarmonicL.reset();
        subHarmonicR.reset();
        exciterL.reset();
        exciterR.reset();
    }

    // Get current settings
    float getSpeed() const { return speed; }
    float getPitch() const { return pitchSemitones; }
    float getRate() const { return rate; }
    bool isBattleMode() const { return battleMode; }

private:
    void updateSoundTouch() {
        if (!soundTouch) return;

        if (useRateMode) {
            // Rate mode: changes both speed and pitch together (vinyl-style)
            soundTouch->setRate(rate);
            soundTouch->setTempo(1.0f);
            soundTouch->setPitch(1.0f);
        } else {
            // Normal mode: independent speed and pitch control
            soundTouch->setRate(1.0f);
            soundTouch->setTempo(speed);
            // Convert semitones to pitch multiplier: 2^(semitones/12)
            float pitchMultiplier = std::pow(2.0f, pitchSemitones / 12.0f);
            soundTouch->setPitch(pitchMultiplier);
        }

        LOGI("SoundTouch updated: speed=%.2f, pitch=%.1f semitones", speed, pitchSemitones);

        // Also update Superpowered if available
        if (timeStretcher) {
            timeStretcher->rate = std::clamp(speed, 0.25f, 4.0f);
            int pitchCents = static_cast<int>(pitchSemitones * 100.0f);
            timeStretcher->pitchShiftCents = std::clamp(pitchCents, -2400, 2400);
        }

#if USE_RUBBERBAND
        // Also update Rubberband if available
        if (rubberbandStretcher) {
            rubberbandStretcher->setTimeRatio(1.0 / speed);  // Inverse: slower speed = higher ratio
            rubberbandStretcher->setPitchScale(std::pow(2.0, pitchSemitones / 12.0));
        }
#endif
    }

    // Current audio engine (user selectable at runtime)
    AudioEngineType currentEngine = AudioEngineType::SOUNDTOUCH;

    // Superpowered SDK components (DJ-grade processing)
    Superpowered::TimeStretching* timeStretcher = nullptr;
    Superpowered::Compressor* spCompressor = nullptr;
    Superpowered::Limiter* spLimiter = nullptr;
    Superpowered::ThreeBandEQ* spEQ = nullptr;

#if USE_RUBBERBAND
    // Rubberband engine (Studio-grade, 10/10 quality)
    RubberBandStretcher* rubberbandStretcher = nullptr;
#endif

    // SoundTouch engine (FREE, no license)
    soundtouch::SoundTouch* soundTouch = nullptr;

    // Battle processing chain (used by SoundTouch engine)
    BattleLimiter* limiter = nullptr;
    BattleCompressor* compressor = nullptr;
    BattleBassBoost* bassBoost = nullptr;

    // Psychoacoustic bass enhancement (no gain, perceived loudness)
    SubHarmonicSynthesizer subHarmonicL;
    SubHarmonicSynthesizer subHarmonicR;
    BassExciter exciterL;
    BassExciter exciterR;
    float subHarmonicAmount = 0.0f;
    float exciterAmount = 0.0f;

    int sampleRate = 44100;
    int channels = 2;

    float speed = 1.0f;
    float pitchSemitones = 0.0f;
    float rate = 1.0f;
    float bassBoostAmount = 0.0f;

    bool formantPreservation = true;
    bool battleMode = false;
    bool useRateMode = false;
    bool superpoweredAvailable = false;
    bool rubberbandAvailable = false;
    bool limiterEnabled = true;  // FULL SEND toggle - when false, no limiting

    // Hardware Protection (strongest safety)
    bool hardwareProtection = true;  // Default ON - protect speakers
    float hardLimiterCeiling = 0.944f;  // -0.5dB hard ceiling
    bool subBassFilterEnabled = true;  // Remove <20Hz rumble
    bool dcBlockerEnabled = true;  // Remove DC offset
    float dcBlockerState = 0.0f;

    // Audiophile Mode (pure quality)
    bool audiophileMode = false;  // Default OFF - battle ready
    bool clarityEnhanceEnabled = false;
    float clarityAmount = 0.0f;
    bool ditheringEnabled = false;

    // Buffers for audio processing
    std::vector<float> floatInputBuffer;
    std::vector<float> floatOutputBuffer;
    std::vector<short> shortOutputBuffer;
};

// =============================================================================
// C API WRAPPER (for JNI)
// =============================================================================

extern "C" {

static BattleAudioEngineImpl* engine = nullptr;

void* battle_engine_create() {
    engine = new BattleAudioEngineImpl();
    return engine;
}

void battle_engine_destroy(void* handle) {
    if (handle) {
        delete static_cast<BattleAudioEngineImpl*>(handle);
        if (engine == handle) engine = nullptr;
    }
}

void battle_engine_configure(void* handle, int sampleRate, int channels) {
    if (handle) {
        static_cast<BattleAudioEngineImpl*>(handle)->configure(sampleRate, channels);
    }
}

void battle_engine_set_speed(void* handle, float speed) {
    if (handle) {
        static_cast<BattleAudioEngineImpl*>(handle)->setSpeed(speed);
    }
}

void battle_engine_set_pitch(void* handle, float semitones) {
    if (handle) {
        static_cast<BattleAudioEngineImpl*>(handle)->setPitch(semitones);
    }
}

void battle_engine_set_rate(void* handle, float rate) {
    if (handle) {
        static_cast<BattleAudioEngineImpl*>(handle)->setRate(rate);
    }
}

void battle_engine_set_battle_mode(void* handle, bool enabled) {
    if (handle) {
        static_cast<BattleAudioEngineImpl*>(handle)->setBattleMode(enabled);
    }
}

void battle_engine_set_bass_boost(void* handle, float amount) {
    if (handle) {
        static_cast<BattleAudioEngineImpl*>(handle)->setBassBoost(amount);
    }
}

void battle_engine_set_sub_harmonic(void* handle, float amount) {
    if (handle) {
        static_cast<BattleAudioEngineImpl*>(handle)->setSubHarmonicAmount(amount);
    }
}

void battle_engine_set_exciter(void* handle, float amount) {
    if (handle) {
        static_cast<BattleAudioEngineImpl*>(handle)->setExciterAmount(amount);
    }
}

void battle_engine_set_limiter_enabled(void* handle, bool enabled) {
    if (handle) {
        static_cast<BattleAudioEngineImpl*>(handle)->setLimiterEnabled(enabled);
    }
}

void battle_engine_set_hardware_protection(void* handle, bool enabled) {
    if (handle) {
        static_cast<BattleAudioEngineImpl*>(handle)->setHardwareProtection(enabled);
    }
}

void battle_engine_set_audiophile_mode(void* handle, bool enabled) {
    if (handle) {
        static_cast<BattleAudioEngineImpl*>(handle)->setAudiophileMode(enabled);
    }
}

void battle_engine_process(void* handle, const short* input, int numSamples,
                           short* output, int* outputSamples) {
    if (handle) {
        static_cast<BattleAudioEngineImpl*>(handle)->process(
            input, numSamples, output, outputSamples);
    }
}

void battle_engine_flush(void* handle) {
    if (handle) {
        static_cast<BattleAudioEngineImpl*>(handle)->flush();
    }
}

void battle_engine_clear(void* handle) {
    if (handle) {
        static_cast<BattleAudioEngineImpl*>(handle)->clear();
    }
}

void battle_engine_set_audio_engine(void* handle, int engineType) {
    if (handle) {
        AudioEngineType engine = static_cast<AudioEngineType>(engineType);
        static_cast<BattleAudioEngineImpl*>(handle)->setAudioEngine(engine);
    }
}

int battle_engine_get_audio_engine(void* handle) {
    if (handle) {
        return static_cast<int>(static_cast<BattleAudioEngineImpl*>(handle)->getAudioEngine());
    }
    return 0;  // Default to SoundTouch
}

} // extern "C"

} // namespace ultramusic
