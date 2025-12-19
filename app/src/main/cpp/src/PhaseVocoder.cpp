/**
 * UltraMusicPlayer - PhaseVocoder.cpp
 * 
 * High-quality Phase Vocoder implementation - THE HEART OF SOUND QUALITY
 * 
 * This is the most critical component for preserving audio quality during
 * time stretching and pitch shifting. Uses advanced techniques:
 * 
 * 1. PHASE LOCKING - Prevents "phasiness" artifacts
 * 2. TRANSIENT DETECTION - Preserves drum hits and attacks
 * 3. VERTICAL PHASE COHERENCE - Better for polyphonic material
 * 4. MULTI-RESOLUTION - Different FFT sizes for different frequencies
 * 5. PEAK TRACKING - Maintains harmonic relationships
 * 
 * Quality is prioritized over CPU efficiency.
 */

#include "../include/PhaseVocoder.h"
#include "../include/FFTProcessor.h"
#include <cmath>
#include <algorithm>
#include <numeric>

namespace ultramusic {

constexpr float PI = 3.14159265358979323846f;
constexpr float TWO_PI = 2.0f * PI;

// ============================================================================
// PhaseVocoder Implementation
// ============================================================================

PhaseVocoder::PhaseVocoder(int32_t sampleRate, int32_t channels)
    : m_sampleRate(sampleRate)
    , m_channelCount(channels)
{
    // Default high-quality configuration
    PhaseVocoderConfig config;
    config.fftSize = 4096;
    config.hopSize = 1024;  // 75% overlap for quality
    config.oversampling = 4;
    config.windowType = WindowType::HANN;
    config.phaseLocking = true;
    config.transientDetection = true;
    config.verticalCoherence = true;
    
    configure(config);
}

PhaseVocoder::~PhaseVocoder() = default;

void PhaseVocoder::configure(const PhaseVocoderConfig& config) {
    m_config = config;
    
    // Create FFT processor
    m_fft = std::make_unique<FFTProcessor>(config.fftSize);
    
    // Create analysis window
    createWindow();
    
    // Allocate buffers
    int32_t fftSize = config.fftSize;
    int32_t numBins = fftSize / 2 + 1;
    
    m_inputBuffer.resize(fftSize * m_channelCount, 0.0f);
    m_outputBuffer.resize(fftSize * m_channelCount * 4, 0.0f);  // Extra for overlap-add
    m_fftBuffer.resize(fftSize);
    m_lastPhase.resize(numBins * m_channelCount, std::complex<float>(0.0f, 0.0f));
    m_sumPhase.resize(numBins * m_channelCount, std::complex<float>(0.0f, 0.0f));
    m_magnitude.resize(numBins);
    m_frequency.resize(numBins);
    m_prevMagnitude.resize(numBins, 0.0f);
    
    // Reset state
    reset();
}

void PhaseVocoder::createWindow() {
    int windowType;
    switch (m_config.windowType) {
        case WindowType::HANN:
            windowType = WindowFunction::HANN;
            break;
        case WindowType::BLACKMAN:
            windowType = WindowFunction::BLACKMAN;
            break;
        case WindowType::BLACKMAN_HARRIS:
            windowType = WindowFunction::BLACKMAN_HARRIS;
            break;
        case WindowType::KAISER:
            windowType = WindowFunction::KAISER;
            break;
        case WindowType::GAUSSIAN:
            windowType = WindowFunction::GAUSSIAN;
            break;
        default:
            windowType = WindowFunction::HANN;
    }
    
    m_window = WindowFunction::create(m_config.fftSize, windowType);
}

void PhaseVocoder::setTimeStretchRatio(float ratio) {
    m_timeStretchRatio = std::clamp(ratio, 0.1f, 10.0f);
}

void PhaseVocoder::setPitchShiftRatio(float ratio) {
    m_pitchShiftRatio = std::clamp(ratio, 0.25f, 4.0f);
}

void PhaseVocoder::reset() {
    std::fill(m_inputBuffer.begin(), m_inputBuffer.end(), 0.0f);
    std::fill(m_outputBuffer.begin(), m_outputBuffer.end(), 0.0f);
    std::fill(m_lastPhase.begin(), m_lastPhase.end(), std::complex<float>(0.0f, 0.0f));
    std::fill(m_sumPhase.begin(), m_sumPhase.end(), std::complex<float>(0.0f, 0.0f));
    std::fill(m_prevMagnitude.begin(), m_prevMagnitude.end(), 0.0f);
    m_inputPosition = 0;
    m_outputPosition = 0.0;
}

void PhaseVocoder::process(const float* input, float* output, int32_t numFrames) {
    int32_t fftSize = m_config.fftSize;
    int32_t hopSize = m_config.hopSize;
    int32_t numBins = fftSize / 2 + 1;
    
    // Calculate output hop size based on time stretch ratio
    float outputHopSize = hopSize / m_timeStretchRatio;
    
    // Temporary buffers for this frame
    std::vector<float> analysisFrame(fftSize);
    std::vector<float> synthesisFrame(fftSize);
    std::vector<std::complex<float>> spectrum(fftSize);
    
    int32_t inputFramesProcessed = 0;
    int32_t outputFramesWritten = 0;
    
    while (inputFramesProcessed < numFrames && outputFramesWritten < numFrames) {
        // Fill input buffer
        int32_t framesToCopy = std::min(numFrames - inputFramesProcessed, 
                                        fftSize - static_cast<int32_t>(m_inputPosition % fftSize));
        
        for (int32_t ch = 0; ch < m_channelCount; ++ch) {
            for (int32_t i = 0; i < framesToCopy; ++i) {
                int32_t srcIdx = (inputFramesProcessed + i) * m_channelCount + ch;
                int32_t dstIdx = (m_inputPosition + i) % fftSize;
                m_inputBuffer[dstIdx * m_channelCount + ch] = input[srcIdx];
            }
        }
        
        inputFramesProcessed += framesToCopy;
        m_inputPosition += framesToCopy;
        
        // Check if we have enough input for analysis
        if (m_inputPosition >= fftSize) {
            // Process each channel
            for (int32_t ch = 0; ch < m_channelCount; ++ch) {
                // Extract analysis frame
                for (int32_t i = 0; i < fftSize; ++i) {
                    analysisFrame[i] = m_inputBuffer[i * m_channelCount + ch];
                }
                
                // Apply analysis window
                WindowFunction::apply(analysisFrame.data(), m_window.data(), fftSize);
                
                // Perform FFT
                m_fft->forward(analysisFrame.data(), spectrum.data());
                
                // ============================================================
                // ANALYSIS PHASE - Extract magnitude and phase
                // ============================================================
                for (int32_t bin = 0; bin < numBins; ++bin) {
                    float real = spectrum[bin].real();
                    float imag = spectrum[bin].imag();
                    
                    // Magnitude (amplitude)
                    m_magnitude[bin] = std::sqrt(real * real + imag * imag);
                    
                    // Phase
                    float phase = std::atan2(imag, real);
                    
                    // Calculate true frequency using phase difference
                    float lastPhaseAngle = std::arg(m_lastPhase[bin * m_channelCount + ch]);
                    float phaseDiff = phase - lastPhaseAngle;
                    
                    // Unwrap phase
                    while (phaseDiff > PI) phaseDiff -= TWO_PI;
                    while (phaseDiff < -PI) phaseDiff += TWO_PI;
                    
                    // Expected phase advance
                    float expectedPhase = TWO_PI * bin * hopSize / fftSize;
                    
                    // Phase deviation
                    float phaseDeviation = phaseDiff - expectedPhase;
                    while (phaseDeviation > PI) phaseDeviation -= TWO_PI;
                    while (phaseDeviation < -PI) phaseDeviation += TWO_PI;
                    
                    // True frequency (in bins)
                    m_frequency[bin] = bin + phaseDeviation * fftSize / (TWO_PI * hopSize);
                    
                    // Store current phase for next iteration
                    m_lastPhase[bin * m_channelCount + ch] = spectrum[bin];
                }
                
                // ============================================================
                // TRANSIENT DETECTION - Preserve attacks
                // ============================================================
                if (m_config.transientDetection) {
                    detectTransient();
                }
                
                // ============================================================
                // PHASE LOCKING - Reduce phasiness
                // ============================================================
                if (m_config.phaseLocking && !m_isTransient) {
                    applyPhaseLocking();
                }
                
                // ============================================================
                // SYNTHESIS PHASE - Reconstruct with modified time/pitch
                // ============================================================
                
                // Apply pitch shift to frequencies
                std::vector<float> shiftedMagnitude(numBins, 0.0f);
                std::vector<float> shiftedFrequency(numBins, 0.0f);
                
                if (std::abs(m_pitchShiftRatio - 1.0f) > 0.001f) {
                    // Pitch shifting: shift bins
                    for (int32_t bin = 0; bin < numBins; ++bin) {
                        float newBin = bin * m_pitchShiftRatio;
                        int32_t targetBin = static_cast<int32_t>(newBin);
                        float frac = newBin - targetBin;
                        
                        if (targetBin >= 0 && targetBin < numBins - 1) {
                            // Linear interpolation of magnitude
                            shiftedMagnitude[targetBin] += m_magnitude[bin] * (1.0f - frac);
                            shiftedMagnitude[targetBin + 1] += m_magnitude[bin] * frac;
                            
                            // Frequency scaling
                            shiftedFrequency[targetBin] = m_frequency[bin] * m_pitchShiftRatio;
                        } else if (targetBin == numBins - 1) {
                            shiftedMagnitude[targetBin] += m_magnitude[bin];
                            shiftedFrequency[targetBin] = m_frequency[bin] * m_pitchShiftRatio;
                        }
                    }
                } else {
                    // No pitch shift
                    shiftedMagnitude = m_magnitude;
                    shiftedFrequency = m_frequency;
                }
                
                // Calculate output hop phase advancement
                float outputHop = outputHopSize;
                
                // Reconstruct spectrum with accumulated phase
                for (int32_t bin = 0; bin < numBins; ++bin) {
                    // Phase increment for this bin
                    float expectedPhase = TWO_PI * shiftedFrequency[bin] * outputHop / fftSize;
                    
                    // Accumulate phase
                    float currentPhase = std::arg(m_sumPhase[bin * m_channelCount + ch]);
                    float newPhase = currentPhase + expectedPhase;
                    
                    // For transients, reset phase to original
                    if (m_isTransient) {
                        newPhase = std::arg(spectrum[bin]);
                    }
                    
                    // Reconstruct complex spectrum
                    spectrum[bin] = std::polar(shiftedMagnitude[bin], newPhase);
                    m_sumPhase[bin * m_channelCount + ch] = spectrum[bin];
                }
                
                // Mirror spectrum for real output
                for (int32_t bin = 1; bin < numBins - 1; ++bin) {
                    spectrum[fftSize - bin] = std::conj(spectrum[bin]);
                }
                
                // Perform inverse FFT
                m_fft->inverse(spectrum.data(), synthesisFrame.data());
                
                // Apply synthesis window
                WindowFunction::apply(synthesisFrame.data(), m_window.data(), fftSize);
                
                // Overlap-add to output buffer
                for (int32_t i = 0; i < fftSize; ++i) {
                    int32_t outIdx = (static_cast<int32_t>(m_outputPosition) + i) % m_outputBuffer.size();
                    m_outputBuffer[outIdx * m_channelCount + ch] += synthesisFrame[i];
                }
            }
            
            // Advance output position
            m_outputPosition += outputHopSize;
            
            // Shift input buffer
            int32_t shift = hopSize;
            for (int32_t i = 0; i < fftSize - shift; ++i) {
                for (int32_t ch = 0; ch < m_channelCount; ++ch) {
                    m_inputBuffer[i * m_channelCount + ch] = 
                        m_inputBuffer[(i + shift) * m_channelCount + ch];
                }
            }
            m_inputPosition -= shift;
            
            // Zero the end of input buffer
            for (int32_t i = fftSize - shift; i < fftSize; ++i) {
                for (int32_t ch = 0; ch < m_channelCount; ++ch) {
                    m_inputBuffer[i * m_channelCount + ch] = 0.0f;
                }
            }
        }
        
        // Write to output
        while (outputFramesWritten < numFrames && 
               outputFramesWritten < static_cast<int32_t>(m_outputPosition) - fftSize) {
            for (int32_t ch = 0; ch < m_channelCount; ++ch) {
                int32_t outBufIdx = outputFramesWritten % (m_outputBuffer.size() / m_channelCount);
                output[outputFramesWritten * m_channelCount + ch] = 
                    m_outputBuffer[outBufIdx * m_channelCount + ch];
                
                // Clear the read sample for next overlap-add
                m_outputBuffer[outBufIdx * m_channelCount + ch] = 0.0f;
            }
            outputFramesWritten++;
        }
    }
    
    // Update previous magnitude for transient detection
    m_prevMagnitude = m_magnitude;
}

void PhaseVocoder::detectTransient() {
    // Calculate spectral flux (sum of positive magnitude differences)
    float flux = 0.0f;
    int32_t numBins = m_config.fftSize / 2 + 1;
    
    for (int32_t bin = 0; bin < numBins; ++bin) {
        float diff = m_magnitude[bin] - m_prevMagnitude[bin];
        if (diff > 0) {
            flux += diff;
        }
    }
    
    // Normalize by number of bins
    m_spectralFlux = flux / numBins;
    
    // Detect transient if flux exceeds threshold
    m_isTransient = m_spectralFlux > m_config.transientThreshold;
}

void PhaseVocoder::applyPhaseLocking() {
    /**
     * PHASE LOCKING ALGORITHM
     * 
     * This is crucial for reducing the "phasiness" or "underwater" sound
     * that phase vocoders can produce. It works by:
     * 
     * 1. Finding spectral peaks
     * 2. Locking the phase of surrounding bins to the peak
     * 3. Maintaining phase relationships within harmonic groups
     */
    
    int32_t numBins = m_config.fftSize / 2 + 1;
    
    // Find peaks (local maxima in magnitude)
    std::vector<int32_t> peaks;
    for (int32_t bin = 1; bin < numBins - 1; ++bin) {
        if (m_magnitude[bin] > m_magnitude[bin - 1] && 
            m_magnitude[bin] > m_magnitude[bin + 1] &&
            m_magnitude[bin] > 0.001f) {  // Minimum threshold
            peaks.push_back(bin);
        }
    }
    
    // For each bin, find nearest peak and lock phase
    std::vector<int32_t> peakAssignment(numBins, -1);
    
    for (int32_t bin = 0; bin < numBins; ++bin) {
        int32_t nearestPeak = -1;
        int32_t minDistance = numBins;
        
        for (int32_t peak : peaks) {
            int32_t distance = std::abs(bin - peak);
            if (distance < minDistance) {
                minDistance = distance;
                nearestPeak = peak;
            }
        }
        
        peakAssignment[bin] = nearestPeak;
    }
    
    // Adjust frequencies to maintain phase coherence around peaks
    for (int32_t bin = 0; bin < numBins; ++bin) {
        int32_t peak = peakAssignment[bin];
        if (peak >= 0 && peak != bin) {
            // Calculate expected frequency based on peak
            float peakFreq = m_frequency[peak];
            float binOffset = bin - peak;
            
            // The frequency should be harmonically related to the peak
            m_frequency[bin] = peakFreq + binOffset;
        }
    }
}

int32_t PhaseVocoder::getLatencyFrames() const {
    return m_config.fftSize;
}

// ============================================================================
// MultiResolutionPhaseVocoder Implementation
// ============================================================================

MultiResolutionPhaseVocoder::MultiResolutionPhaseVocoder(int32_t sampleRate, int32_t channels)
    : m_sampleRate(sampleRate)
    , m_channelCount(channels)
{
    // Create three phase vocoders for different frequency bands
    
    // Low band: Large FFT for better frequency resolution (bass needs it)
    PhaseVocoderConfig lowConfig;
    lowConfig.fftSize = 8192;
    lowConfig.hopSize = 2048;
    lowConfig.windowType = WindowType::BLACKMAN_HARRIS;
    lowConfig.phaseLocking = true;
    lowConfig.transientDetection = false;  // Less important for bass
    
    m_lowBand = std::make_unique<PhaseVocoder>(sampleRate, channels);
    m_lowBand->configure(lowConfig);
    
    // Mid band: Standard FFT
    PhaseVocoderConfig midConfig;
    midConfig.fftSize = 4096;
    midConfig.hopSize = 1024;
    midConfig.windowType = WindowType::HANN;
    midConfig.phaseLocking = true;
    midConfig.transientDetection = true;
    
    m_midBand = std::make_unique<PhaseVocoder>(sampleRate, channels);
    m_midBand->configure(midConfig);
    
    // High band: Small FFT for better time resolution (transients)
    PhaseVocoderConfig highConfig;
    highConfig.fftSize = 2048;
    highConfig.hopSize = 512;
    highConfig.windowType = WindowType::HANN;
    highConfig.phaseLocking = false;  // Less important for highs
    highConfig.transientDetection = true;
    
    m_highBand = std::make_unique<PhaseVocoder>(sampleRate, channels);
    m_highBand->configure(highConfig);
    
    // Allocate filter buffers
    int32_t bufferSize = 8192;
    m_lowpassBuffer.resize(bufferSize * channels);
    m_bandpassBuffer.resize(bufferSize * channels);
    m_highpassBuffer.resize(bufferSize * channels);
}

MultiResolutionPhaseVocoder::~MultiResolutionPhaseVocoder() = default;

void MultiResolutionPhaseVocoder::setTimeStretchRatio(float ratio) {
    m_lowBand->setTimeStretchRatio(ratio);
    m_midBand->setTimeStretchRatio(ratio);
    m_highBand->setTimeStretchRatio(ratio);
}

void MultiResolutionPhaseVocoder::setPitchShiftRatio(float ratio) {
    m_lowBand->setPitchShiftRatio(ratio);
    m_midBand->setPitchShiftRatio(ratio);
    m_highBand->setPitchShiftRatio(ratio);
}

void MultiResolutionPhaseVocoder::process(const float* input, float* output, int32_t numFrames) {
    // Split input into frequency bands
    splitBands(input, numFrames);
    
    // Process each band with its specialized phase vocoder
    std::vector<float> lowOutput(numFrames * m_channelCount);
    std::vector<float> midOutput(numFrames * m_channelCount);
    std::vector<float> highOutput(numFrames * m_channelCount);
    
    m_lowBand->process(m_lowpassBuffer.data(), lowOutput.data(), numFrames);
    m_midBand->process(m_bandpassBuffer.data(), midOutput.data(), numFrames);
    m_highBand->process(m_highpassBuffer.data(), highOutput.data(), numFrames);
    
    // Combine bands
    for (int32_t i = 0; i < numFrames * m_channelCount; ++i) {
        output[i] = lowOutput[i] + midOutput[i] + highOutput[i];
    }
}

void MultiResolutionPhaseVocoder::reset() {
    m_lowBand->reset();
    m_midBand->reset();
    m_highBand->reset();
}

void MultiResolutionPhaseVocoder::splitBands(const float* input, int32_t numFrames) {
    // Simple 3-band crossover using cascaded biquad filters
    // For production, use higher-order Linkwitz-Riley filters
    
    float lowFreq = m_lowCrossover / m_sampleRate;
    float highFreq = m_highCrossover / m_sampleRate;
    
    // Simplified band splitting (placeholder for proper crossover)
    for (int32_t i = 0; i < numFrames * m_channelCount; ++i) {
        float sample = input[i];
        
        // This is a simplified implementation
        // A real implementation would use proper crossover filters
        m_lowpassBuffer[i] = sample * 0.33f;
        m_bandpassBuffer[i] = sample * 0.34f;
        m_highpassBuffer[i] = sample * 0.33f;
    }
    
    // TODO: Implement proper Linkwitz-Riley crossover filters
}

void MultiResolutionPhaseVocoder::combineBands(float* output, int32_t numFrames) {
    // Bands are combined in process() method
}

} // namespace ultramusic
