/**
 * UltraMusicPlayer - FormantPreserver.cpp
 * 
 * Formant preservation implementation using True Envelope estimation.
 * This is CRITICAL for maintaining natural vocal quality when pitch shifting.
 * 
 * Without formant preservation:
 * - Pitch up = chipmunk voice
 * - Pitch down = demon voice
 * 
 * With formant preservation:
 * - Pitch changes but voice character remains natural
 */

#include "../include/FormantPreserver.h"
#include "../include/FFTProcessor.h"
#include <cmath>
#include <algorithm>
#include <numeric>

namespace ultramusic {

static constexpr float PI = 3.14159265358979323846f;
static constexpr float LOG_MIN = 1e-10f;

FormantPreserver::FormantPreserver(int32_t sampleRate, int32_t fftSize)
    : m_sampleRate(sampleRate)
    , m_fftSize(fftSize)
{
    m_fft = std::make_unique<FFTProcessor>(fftSize);
    
    int numBins = fftSize / 2 + 1;
    m_logMagnitude.resize(numBins);
    m_cepstrum.resize(fftSize);
    m_envelope.resize(numBins);
    m_fineStructure.resize(numBins);
    m_lpcCoeffs.resize(m_envelopeOrder + 1);
}

FormantPreserver::~FormantPreserver() = default;

void FormantPreserver::setMethod(EnvelopeMethod method) {
    m_method = method;
}

void FormantPreserver::setEnvelopeOrder(int order) {
    m_envelopeOrder = std::clamp(order, 10, 100);
    m_lpcCoeffs.resize(m_envelopeOrder + 1);
}

void FormantPreserver::processSpectrum(float* magnitudes, float* phases,
                                       float pitchShiftRatio, float formantShiftRatio) {
    int numBins = m_fftSize / 2 + 1;
    
    // Step 1: Extract spectral envelope (formants)
    extractEnvelope(magnitudes, m_envelope.data());
    
    // Step 2: Calculate fine structure (pitch-related harmonics)
    for (int i = 0; i < numBins; ++i) {
        if (m_envelope[i] > LOG_MIN) {
            m_fineStructure[i] = magnitudes[i] / m_envelope[i];
        } else {
            m_fineStructure[i] = 1.0f;
        }
    }
    
    // Step 3: Shift envelope if formant shift is requested
    if (std::abs(formantShiftRatio - 1.0f) > 0.001f) {
        shiftEnvelope(m_envelope.data(), formantShiftRatio);
    }
    
    // Step 4: Shift fine structure for pitch shifting
    std::vector<float> shiftedFineStructure(numBins, 0.0f);
    if (std::abs(pitchShiftRatio - 1.0f) > 0.001f) {
        for (int destBin = 0; destBin < numBins; ++destBin) {
            float srcBin = destBin / pitchShiftRatio;
            int srcBinInt = static_cast<int>(srcBin);
            float frac = srcBin - srcBinInt;
            
            if (srcBinInt >= 0 && srcBinInt < numBins - 1) {
                // Linear interpolation
                shiftedFineStructure[destBin] = 
                    m_fineStructure[srcBinInt] * (1.0f - frac) +
                    m_fineStructure[srcBinInt + 1] * frac;
            } else if (srcBinInt >= 0 && srcBinInt < numBins) {
                shiftedFineStructure[destBin] = m_fineStructure[srcBinInt];
            }
        }
    } else {
        shiftedFineStructure = m_fineStructure;
    }
    
    // Step 5: Recombine envelope with shifted fine structure
    for (int i = 0; i < numBins; ++i) {
        magnitudes[i] = m_envelope[i] * shiftedFineStructure[i];
    }
}

void FormantPreserver::extractEnvelope(const float* magnitudes, float* envelope) {
    switch (m_method) {
        case EnvelopeMethod::CEPSTRUM:
            estimateCepstrum(magnitudes, envelope);
            break;
        case EnvelopeMethod::TRUE_ENVELOPE:
            estimateTrueEnvelope(magnitudes, envelope);
            break;
        case EnvelopeMethod::LPC:
            estimateLPC(magnitudes, envelope);
            break;
        case EnvelopeMethod::DISCRETE_CEPSTRUM:
            estimateCepstrum(magnitudes, envelope);  // Fallback
            break;
    }
}

void FormantPreserver::estimateCepstrum(const float* magnitudes, float* envelope) {
    int numBins = m_fftSize / 2 + 1;
    
    // Convert to log magnitude
    for (int i = 0; i < numBins; ++i) {
        m_logMagnitude[i] = std::log(std::max(magnitudes[i], LOG_MIN));
    }
    
    // Mirror for full spectrum (for real cepstrum)
    std::vector<std::complex<float>> spectrum(m_fftSize);
    for (int i = 0; i < numBins; ++i) {
        spectrum[i] = std::complex<float>(m_logMagnitude[i], 0.0f);
    }
    for (int i = numBins; i < m_fftSize; ++i) {
        spectrum[i] = spectrum[m_fftSize - i];
    }
    
    // IFFT to get cepstrum
    m_fft->inverseComplex(spectrum.data());
    
    // Liftering: keep only low quefrency components (envelope)
    for (int i = m_envelopeOrder; i < m_fftSize - m_envelopeOrder; ++i) {
        spectrum[i] = std::complex<float>(0.0f, 0.0f);
    }
    
    // FFT back to get smoothed envelope
    m_fft->forwardComplex(spectrum.data());
    
    // Convert back from log
    for (int i = 0; i < numBins; ++i) {
        envelope[i] = std::exp(spectrum[i].real());
    }
}

void FormantPreserver::estimateTrueEnvelope(const float* magnitudes, float* envelope) {
    /**
     * True Envelope algorithm:
     * Iteratively fits an envelope that touches the spectral peaks.
     * This gives more accurate formant estimation than cepstral smoothing.
     */
    int numBins = m_fftSize / 2 + 1;
    int numIterations = 10;
    
    // Initialize envelope as the original spectrum
    std::copy(magnitudes, magnitudes + numBins, envelope);
    
    for (int iter = 0; iter < numIterations; ++iter) {
        // Convert to log domain
        for (int i = 0; i < numBins; ++i) {
            m_logMagnitude[i] = std::log(std::max(envelope[i], LOG_MIN));
        }
        
        // Smooth using cepstral method
        std::vector<std::complex<float>> spectrum(m_fftSize);
        for (int i = 0; i < numBins; ++i) {
            spectrum[i] = std::complex<float>(m_logMagnitude[i], 0.0f);
        }
        for (int i = numBins; i < m_fftSize; ++i) {
            spectrum[i] = spectrum[m_fftSize - i];
        }
        
        m_fft->inverseComplex(spectrum.data());
        
        // Stronger liftering for true envelope
        int lifterOrder = m_envelopeOrder / 2;
        for (int i = lifterOrder; i < m_fftSize - lifterOrder; ++i) {
            spectrum[i] = std::complex<float>(0.0f, 0.0f);
        }
        
        m_fft->forwardComplex(spectrum.data());
        
        // Update envelope: take maximum of smoothed and original
        for (int i = 0; i < numBins; ++i) {
            float smoothed = std::exp(spectrum[i].real());
            envelope[i] = std::max(smoothed, magnitudes[i]);
        }
    }
    
    // Final smoothing pass
    estimateCepstrum(envelope, envelope);
}

void FormantPreserver::estimateLPC(const float* magnitudes, float* envelope) {
    /**
     * Linear Predictive Coding (LPC) for envelope estimation.
     * Uses Levinson-Durbin recursion.
     */
    int numBins = m_fftSize / 2 + 1;
    
    // Convert spectrum to autocorrelation
    std::vector<float> autocorr(m_envelopeOrder + 1, 0.0f);
    
    for (int lag = 0; lag <= m_envelopeOrder; ++lag) {
        for (int i = 0; i < numBins - lag; ++i) {
            autocorr[lag] += magnitudes[i] * magnitudes[i + lag];
        }
    }
    
    // Levinson-Durbin recursion
    std::vector<float> coeffs(m_envelopeOrder + 1, 0.0f);
    std::vector<float> temp(m_envelopeOrder + 1, 0.0f);
    float error = autocorr[0];
    
    for (int i = 1; i <= m_envelopeOrder; ++i) {
        float lambda = 0.0f;
        for (int j = 0; j < i; ++j) {
            lambda += coeffs[j] * autocorr[i - j];
        }
        lambda = (autocorr[i] - lambda) / error;
        
        for (int j = 0; j < i; ++j) {
            temp[j] = coeffs[j] - lambda * coeffs[i - 1 - j];
        }
        temp[i] = lambda;
        
        std::copy(temp.begin(), temp.begin() + i + 1, coeffs.begin());
        error *= (1.0f - lambda * lambda);
    }
    
    // Convert LPC coefficients to envelope
    float gain = std::sqrt(error);
    for (int bin = 0; bin < numBins; ++bin) {
        float omega = PI * bin / (numBins - 1);
        std::complex<float> sum(1.0f, 0.0f);
        
        for (int k = 1; k <= m_envelopeOrder; ++k) {
            float angle = omega * k;
            sum -= coeffs[k] * std::complex<float>(std::cos(angle), -std::sin(angle));
        }
        
        envelope[bin] = gain / std::abs(sum);
    }
}

void FormantPreserver::shiftEnvelope(float* envelope, float shiftRatio) {
    int numBins = m_fftSize / 2 + 1;
    std::vector<float> shifted(numBins, 0.0f);
    
    for (int destBin = 0; destBin < numBins; ++destBin) {
        float srcBin = destBin / shiftRatio;
        int srcBinInt = static_cast<int>(srcBin);
        float frac = srcBin - srcBinInt;
        
        if (srcBinInt >= 0 && srcBinInt < numBins - 1) {
            shifted[destBin] = 
                envelope[srcBinInt] * (1.0f - frac) +
                envelope[srcBinInt + 1] * frac;
        } else if (srcBinInt >= 0 && srcBinInt < numBins) {
            shifted[destBin] = envelope[srcBinInt];
        } else {
            shifted[destBin] = envelope[0];  // Extend with first value
        }
    }
    
    std::copy(shifted.begin(), shifted.end(), envelope);
}

void FormantPreserver::applyEnvelope(float* magnitudes, const float* envelope) {
    int numBins = m_fftSize / 2 + 1;
    
    // Extract current envelope
    std::vector<float> currentEnvelope(numBins);
    extractEnvelope(magnitudes, currentEnvelope.data());
    
    // Replace with new envelope while preserving fine structure
    for (int i = 0; i < numBins; ++i) {
        if (currentEnvelope[i] > LOG_MIN) {
            float fineStructure = magnitudes[i] / currentEnvelope[i];
            magnitudes[i] = envelope[i] * fineStructure;
        }
    }
}

std::vector<FormantPreserver::FormantInfo> FormantPreserver::detectFormants(
        const float* magnitudes) {
    std::vector<FormantInfo> formants;
    int numBins = m_fftSize / 2 + 1;
    
    // Extract envelope first
    std::vector<float> envelope(numBins);
    extractEnvelope(magnitudes, envelope.data());
    
    // Find peaks in envelope (formants)
    float freqPerBin = static_cast<float>(m_sampleRate) / m_fftSize;
    
    for (int i = 2; i < numBins - 2; ++i) {
        if (envelope[i] > envelope[i-1] && envelope[i] > envelope[i+1] &&
            envelope[i] > envelope[i-2] && envelope[i] > envelope[i+2]) {
            
            // Parabolic interpolation for more accurate peak location
            float alpha = std::log(std::max(envelope[i-1], LOG_MIN));
            float beta = std::log(std::max(envelope[i], LOG_MIN));
            float gamma = std::log(std::max(envelope[i+1], LOG_MIN));
            
            float peakOffset = 0.5f * (alpha - gamma) / (alpha - 2*beta + gamma);
            float peakBin = i + peakOffset;
            
            FormantInfo info;
            info.frequency = peakBin * freqPerBin;
            info.amplitude = 20.0f * std::log10(std::max(envelope[i], LOG_MIN));
            
            // Estimate bandwidth from -3dB points
            float peakLevel = envelope[i];
            float threshold = peakLevel * 0.707f;  // -3dB
            
            int lowBin = i, highBin = i;
            while (lowBin > 0 && envelope[lowBin] > threshold) lowBin--;
            while (highBin < numBins-1 && envelope[highBin] > threshold) highBin++;
            
            info.bandwidth = (highBin - lowBin) * freqPerBin;
            
            formants.push_back(info);
        }
    }
    
    // Sort by frequency
    std::sort(formants.begin(), formants.end(),
              [](const FormantInfo& a, const FormantInfo& b) {
                  return a.frequency < b.frequency;
              });
    
    return formants;
}

// ============================================================================
// VocalEnhancer Implementation
// ============================================================================

VocalEnhancer::VocalEnhancer(int32_t sampleRate)
    : m_sampleRate(sampleRate)
{
    // Initialize filter states
    m_filterStates.resize(10, 0.0f);
}

VocalEnhancer::~VocalEnhancer() = default;

void VocalEnhancer::setDeEsser(float threshold, float ratio) {
    m_deEsserThreshold = threshold;
    m_deEsserRatio = ratio;
}

void VocalEnhancer::enableDeEsser(bool enable) {
    m_deEsserEnabled = enable;
}

void VocalEnhancer::setPresence(float amount) {
    m_presence = std::clamp(amount, 0.0f, 1.0f);
}

void VocalEnhancer::setAir(float amount) {
    m_air = std::clamp(amount, 0.0f, 1.0f);
}

void VocalEnhancer::setWarmth(float amount) {
    m_warmth = std::clamp(amount, 0.0f, 1.0f);
}

void VocalEnhancer::process(float* buffer, int32_t numFrames) {
    // Simple implementations - production version would use proper filter design
    
    for (int i = 0; i < numFrames; ++i) {
        float sample = buffer[i];
        
        // Apply warmth (low-shelf boost around 200-400 Hz)
        if (m_warmth > 0.0f) {
            // Simple 1-pole lowpass for warmth estimation
            m_filterStates[0] = m_filterStates[0] * 0.95f + sample * 0.05f;
            sample += m_filterStates[0] * m_warmth * 0.3f;
        }
        
        // Apply presence (boost around 2-5 kHz)
        if (m_presence > 0.0f) {
            // Bandpass approximation
            m_filterStates[1] = m_filterStates[1] * 0.8f + sample * 0.2f;
            m_filterStates[2] = m_filterStates[2] * 0.6f + m_filterStates[1] * 0.4f;
            float presence = m_filterStates[1] - m_filterStates[2];
            sample += presence * m_presence * 0.5f;
        }
        
        // Apply air (high-shelf boost above 8 kHz)
        if (m_air > 0.0f) {
            // Simple highpass for air
            m_filterStates[3] = sample - m_filterStates[4];
            m_filterStates[4] = m_filterStates[4] * 0.9f + sample * 0.1f;
            sample += m_filterStates[3] * m_air * 0.2f;
        }
        
        buffer[i] = sample;
    }
}

} // namespace ultramusic
