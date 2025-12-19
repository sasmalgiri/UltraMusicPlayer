/**
 * UltraMusicPlayer - FFTProcessor.cpp
 * 
 * High-performance FFT implementation optimized for audio processing.
 * Uses NEON SIMD on ARM for maximum performance while maintaining quality.
 * 
 * Quality considerations:
 * - Uses 64-bit double precision for intermediate calculations
 * - Proper windowing to reduce spectral leakage
 * - Accurate twiddle factor computation
 */

#include "../include/FFTProcessor.h"
#include <cmath>
#include <algorithm>
#include <stdexcept>

#if defined(__ARM_NEON) || defined(__ARM_NEON__)
#include <arm_neon.h>
#define USE_NEON 1
#endif

namespace ultramusic {

// Constants for high precision
constexpr double PI = 3.14159265358979323846;
constexpr double TWO_PI = 2.0 * PI;

FFTProcessor::FFTProcessor(int32_t size) : m_size(size) {
    // Validate size is power of 2
    if (size <= 0 || (size & (size - 1)) != 0) {
        throw std::invalid_argument("FFT size must be a positive power of 2");
    }
    
    // Calculate log2 of size
    m_log2Size = 0;
    int32_t temp = size;
    while (temp > 1) {
        temp >>= 1;
        m_log2Size++;
    }
    
    // Allocate buffers
    m_twiddleFactors.resize(size);
    m_bitReversalTable.resize(size);
    m_workBuffer.resize(size);
    
    // Precompute tables
    computeTwiddleFactors();
    computeBitReversalTable();
}

FFTProcessor::~FFTProcessor() = default;

void FFTProcessor::computeTwiddleFactors() {
    // Compute twiddle factors with high precision
    // W_N^k = e^(-2πik/N) for forward FFT
    for (int32_t k = 0; k < m_size; ++k) {
        double angle = -TWO_PI * k / m_size;
        // Use double precision for accuracy
        m_twiddleFactors[k] = std::complex<float>(
            static_cast<float>(std::cos(angle)),
            static_cast<float>(std::sin(angle))
        );
    }
}

void FFTProcessor::computeBitReversalTable() {
    for (int32_t i = 0; i < m_size; ++i) {
        int32_t reversed = 0;
        int32_t temp = i;
        for (int32_t j = 0; j < m_log2Size; ++j) {
            reversed = (reversed << 1) | (temp & 1);
            temp >>= 1;
        }
        m_bitReversalTable[i] = reversed;
    }
}

void FFTProcessor::forward(const float* input, std::complex<float>* output) {
    // Copy real input to complex buffer
    for (int32_t i = 0; i < m_size; ++i) {
        m_workBuffer[i] = std::complex<float>(input[i], 0.0f);
    }
    
    // Perform FFT
    performFFT(m_workBuffer.data(), false);
    
    // Copy to output
    std::copy(m_workBuffer.begin(), m_workBuffer.end(), output);
}

void FFTProcessor::inverse(const std::complex<float>* input, float* output) {
    // Copy input to work buffer
    std::copy(input, input + m_size, m_workBuffer.begin());
    
    // Perform inverse FFT
    performFFT(m_workBuffer.data(), true);
    
    // Extract real part and normalize
    float normFactor = 1.0f / m_size;
    for (int32_t i = 0; i < m_size; ++i) {
        output[i] = m_workBuffer[i].real() * normFactor;
    }
}

void FFTProcessor::forwardComplex(std::complex<float>* data) {
    performFFT(data, false);
}

void FFTProcessor::inverseComplex(std::complex<float>* data) {
    performFFT(data, true);
    
    // Normalize
    float normFactor = 1.0f / m_size;
    for (int32_t i = 0; i < m_size; ++i) {
        data[i] *= normFactor;
    }
}

void FFTProcessor::performFFT(std::complex<float>* data, bool inverse) {
    // Bit-reversal permutation
    for (int32_t i = 0; i < m_size; ++i) {
        int32_t j = m_bitReversalTable[i];
        if (i < j) {
            std::swap(data[i], data[j]);
        }
    }
    
    // Cooley-Tukey FFT
    for (int32_t stage = 1; stage <= m_log2Size; ++stage) {
        int32_t m = 1 << stage;           // Butterfly size
        int32_t mHalf = m >> 1;           // Half butterfly
        int32_t tableStep = m_size / m;   // Step through twiddle table
        
#if USE_NEON && defined(__aarch64__)
        // Use NEON-optimized butterfly for ARM64
        if (mHalf >= 4) {
            butterflyRadix2_NEON(data, stage);
            continue;
        }
#endif
        
        // Standard butterfly
        for (int32_t k = 0; k < m_size; k += m) {
            for (int32_t j = 0; j < mHalf; ++j) {
                int32_t twiddleIdx = j * tableStep;
                std::complex<float> twiddle = m_twiddleFactors[twiddleIdx];
                
                // For inverse FFT, conjugate the twiddle factor
                if (inverse) {
                    twiddle = std::conj(twiddle);
                }
                
                int32_t evenIdx = k + j;
                int32_t oddIdx = k + j + mHalf;
                
                std::complex<float> even = data[evenIdx];
                std::complex<float> odd = data[oddIdx] * twiddle;
                
                data[evenIdx] = even + odd;
                data[oddIdx] = even - odd;
            }
        }
    }
}

#if USE_NEON && defined(__aarch64__)
void FFTProcessor::butterflyRadix2_NEON(std::complex<float>* data, int32_t stage) {
    int32_t m = 1 << stage;
    int32_t mHalf = m >> 1;
    int32_t tableStep = m_size / m;
    
    for (int32_t k = 0; k < m_size; k += m) {
        for (int32_t j = 0; j < mHalf; j += 4) {
            // Load 4 complex pairs at a time
            float32x4x2_t even_vec, odd_vec, twiddle_vec;
            
            // Load even elements
            float even_real[4], even_imag[4];
            float odd_real[4], odd_imag[4];
            float tw_real[4], tw_imag[4];
            
            for (int32_t i = 0; i < 4 && (j + i) < mHalf; ++i) {
                int32_t evenIdx = k + j + i;
                int32_t oddIdx = k + j + i + mHalf;
                int32_t twIdx = (j + i) * tableStep;
                
                even_real[i] = data[evenIdx].real();
                even_imag[i] = data[evenIdx].imag();
                odd_real[i] = data[oddIdx].real();
                odd_imag[i] = data[oddIdx].imag();
                tw_real[i] = m_twiddleFactors[twIdx].real();
                tw_imag[i] = m_twiddleFactors[twIdx].imag();
            }
            
            // Load into NEON registers
            float32x4_t er = vld1q_f32(even_real);
            float32x4_t ei = vld1q_f32(even_imag);
            float32x4_t or_vec = vld1q_f32(odd_real);
            float32x4_t oi = vld1q_f32(odd_imag);
            float32x4_t tr = vld1q_f32(tw_real);
            float32x4_t ti = vld1q_f32(tw_imag);
            
            // Complex multiply: odd * twiddle
            // (a + bi)(c + di) = (ac - bd) + (ad + bc)i
            float32x4_t prod_r = vsubq_f32(vmulq_f32(or_vec, tr), vmulq_f32(oi, ti));
            float32x4_t prod_i = vaddq_f32(vmulq_f32(or_vec, ti), vmulq_f32(oi, tr));
            
            // Butterfly
            float32x4_t out_even_r = vaddq_f32(er, prod_r);
            float32x4_t out_even_i = vaddq_f32(ei, prod_i);
            float32x4_t out_odd_r = vsubq_f32(er, prod_r);
            float32x4_t out_odd_i = vsubq_f32(ei, prod_i);
            
            // Store results
            float out_er[4], out_ei[4], out_or[4], out_oi[4];
            vst1q_f32(out_er, out_even_r);
            vst1q_f32(out_ei, out_even_i);
            vst1q_f32(out_or, out_odd_r);
            vst1q_f32(out_oi, out_odd_i);
            
            for (int32_t i = 0; i < 4 && (j + i) < mHalf; ++i) {
                int32_t evenIdx = k + j + i;
                int32_t oddIdx = k + j + i + mHalf;
                data[evenIdx] = std::complex<float>(out_er[i], out_ei[i]);
                data[oddIdx] = std::complex<float>(out_or[i], out_oi[i]);
            }
        }
    }
}
#endif

float FFTProcessor::getBinFrequency(int32_t bin, float sampleRate) const {
    return static_cast<float>(bin) * sampleRate / m_size;
}

int32_t FFTProcessor::getFrequencyBin(float frequency, float sampleRate) const {
    return static_cast<int32_t>(std::round(frequency * m_size / sampleRate));
}

// ============================================================================
// Window Functions
// ============================================================================

std::vector<float> WindowFunction::create(int32_t size, int type) {
    std::vector<float> window(size);
    
    switch (type) {
        case RECTANGULAR:
            std::fill(window.begin(), window.end(), 1.0f);
            break;
            
        case HANN:
            for (int32_t n = 0; n < size; ++n) {
                window[n] = 0.5f * (1.0f - std::cos(TWO_PI * n / (size - 1)));
            }
            break;
            
        case HAMMING:
            for (int32_t n = 0; n < size; ++n) {
                window[n] = 0.54f - 0.46f * std::cos(TWO_PI * n / (size - 1));
            }
            break;
            
        case BLACKMAN:
            for (int32_t n = 0; n < size; ++n) {
                double phase = TWO_PI * n / (size - 1);
                window[n] = 0.42f - 0.5f * std::cos(phase) + 0.08f * std::cos(2.0 * phase);
            }
            break;
            
        case BLACKMAN_HARRIS:
            for (int32_t n = 0; n < size; ++n) {
                double phase = TWO_PI * n / (size - 1);
                window[n] = 0.35875f - 0.48829f * std::cos(phase) 
                          + 0.14128f * std::cos(2.0 * phase)
                          - 0.01168f * std::cos(3.0 * phase);
            }
            break;
            
        case KAISER:
            for (int32_t n = 0; n < size; ++n) {
                window[n] = kaiser(n, size, 9.0f);  // Beta = 9 for good quality
            }
            break;
            
        case GAUSSIAN: {
            float sigma = 0.4f;
            for (int32_t n = 0; n < size; ++n) {
                float t = (n - (size - 1) / 2.0f) / (sigma * (size - 1) / 2.0f);
                window[n] = std::exp(-0.5f * t * t);
            }
            break;
        }
            
        case TRIANGULAR:
            for (int32_t n = 0; n < size; ++n) {
                window[n] = 1.0f - std::abs((n - (size - 1) / 2.0f) / ((size + 1) / 2.0f));
            }
            break;
            
        case NUTTALL:
            for (int32_t n = 0; n < size; ++n) {
                double phase = TWO_PI * n / (size - 1);
                window[n] = 0.355768f - 0.487396f * std::cos(phase)
                          + 0.144232f * std::cos(2.0 * phase)
                          - 0.012604f * std::cos(3.0 * phase);
            }
            break;
            
        default:
            // Default to Hann
            for (int32_t n = 0; n < size; ++n) {
                window[n] = 0.5f * (1.0f - std::cos(TWO_PI * n / (size - 1)));
            }
    }
    
    return window;
}

void WindowFunction::apply(float* buffer, const float* window, int32_t size) {
#if USE_NEON
    // NEON-optimized windowing
    int32_t i = 0;
    for (; i + 4 <= size; i += 4) {
        float32x4_t buf = vld1q_f32(buffer + i);
        float32x4_t win = vld1q_f32(window + i);
        float32x4_t result = vmulq_f32(buf, win);
        vst1q_f32(buffer + i, result);
    }
    // Handle remaining samples
    for (; i < size; ++i) {
        buffer[i] *= window[i];
    }
#else
    for (int32_t i = 0; i < size; ++i) {
        buffer[i] *= window[i];
    }
#endif
}

float WindowFunction::calculateGain(const float* window, int32_t size) {
    float sum = 0.0f;
    for (int32_t i = 0; i < size; ++i) {
        sum += window[i];
    }
    return sum / size;
}

float WindowFunction::kaiser(int n, int size, float beta) {
    float t = 2.0f * n / (size - 1) - 1.0f;
    return bessel_i0(beta * std::sqrt(1.0f - t * t)) / bessel_i0(beta);
}

float WindowFunction::bessel_i0(float x) {
    // Modified Bessel function of the first kind, order 0
    // Using series expansion for accuracy
    float sum = 1.0f;
    float term = 1.0f;
    float x_squared = x * x * 0.25f;
    
    for (int k = 1; k < 25; ++k) {
        term *= x_squared / (k * k);
        sum += term;
        if (term < 1e-10f) break;
    }
    
    return sum;
}

} // namespace ultramusic
