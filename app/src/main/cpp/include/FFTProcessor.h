/**
 * UltraMusicPlayer - FFTProcessor.h
 * 
 * Optimized FFT implementation using NEON SIMD on ARM
 * and SSE on x86 for maximum performance.
 * 
 * Uses Cooley-Tukey radix-2 algorithm with optimizations.
 */

#ifndef ULTRA_MUSIC_FFT_PROCESSOR_H
#define ULTRA_MUSIC_FFT_PROCESSOR_H

#include <vector>
#include <complex>
#include <cstdint>

namespace ultramusic {

/**
 * FFTProcessor class
 * 
 * High-performance FFT implementation optimized for real-time audio.
 * Supports power-of-2 sizes from 256 to 16384.
 */
class FFTProcessor {
public:
    explicit FFTProcessor(int32_t size);
    ~FFTProcessor();
    
    // Forward FFT: real input -> complex output
    void forward(const float* input, std::complex<float>* output);
    
    // Inverse FFT: complex input -> real output
    void inverse(const std::complex<float>* input, float* output);
    
    // In-place complex FFT
    void forwardComplex(std::complex<float>* data);
    void inverseComplex(std::complex<float>* data);
    
    // Get FFT size
    int32_t getSize() const { return m_size; }
    
    // Get frequency bin for a given index
    float getBinFrequency(int32_t bin, float sampleRate) const;
    
    // Get index for a given frequency
    int32_t getFrequencyBin(float frequency, float sampleRate) const;
    
private:
    int32_t m_size;
    int32_t m_log2Size;
    
    // Precomputed twiddle factors
    std::vector<std::complex<float>> m_twiddleFactors;
    
    // Bit-reversal lookup table
    std::vector<int32_t> m_bitReversalTable;
    
    // Work buffers
    std::vector<std::complex<float>> m_workBuffer;
    
    // Initialization
    void computeTwiddleFactors();
    void computeBitReversalTable();
    
    // Core FFT algorithm
    void performFFT(std::complex<float>* data, bool inverse);
    
    // SIMD-optimized butterfly operations
    void butterflyRadix2(std::complex<float>* data, int32_t stage);
    
#if defined(__ARM_NEON) || defined(__ARM_NEON__)
    void butterflyRadix2_NEON(std::complex<float>* data, int32_t stage);
#endif
    
#if defined(__SSE2__)
    void butterflyRadix2_SSE(std::complex<float>* data, int32_t stage);
#endif
};

/**
 * Windowing functions for spectral analysis
 */
class WindowFunction {
public:
    // Generate window of given type and size
    static std::vector<float> create(int32_t size, int type);
    
    // Window types
    enum Type {
        RECTANGULAR = 0,
        HANN = 1,
        HAMMING = 2,
        BLACKMAN = 3,
        BLACKMAN_HARRIS = 4,
        KAISER = 5,
        GAUSSIAN = 6,
        TRIANGULAR = 7,
        NUTTALL = 8
    };
    
    // Apply window to buffer (in-place)
    static void apply(float* buffer, const float* window, int32_t size);
    
    // Calculate window gain for normalization
    static float calculateGain(const float* window, int32_t size);
    
private:
    static float kaiser(int n, int size, float beta);
    static float bessel_i0(float x);
};

} // namespace ultramusic

#endif // ULTRA_MUSIC_FFT_PROCESSOR_H
