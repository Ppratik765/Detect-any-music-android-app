package com.priyanshu.aura.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object FFT {

    /**
     * Computes the discrete Fourier transform (DFT) of the given complex vector, returning the result as a
     * pair of FloatArrays: first representing real parts, and second representing imaginary parts.
     * The input sizes must be a power of 2.
     */
    fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        if (n == 0) return
        require(n and (n - 1) == 0) { "n is not a power of 2: $n" }
        require(n == imag.size) { "Arrays must have same length" }

        // Bit-reversal permutation
        val shift = 32 - Integer.numberOfTrailingZeros(n)
        for (i in 0 until n) {
            val j = Integer.reverse(i) ushr shift
            if (j > i) {
                var temp = real[i]
                real[i] = real[j]
                real[j] = temp
                temp = imag[i]
                imag[i] = imag[j]
                imag[j] = temp
            }
        }

        // Cooley-Tukey decimation-in-time radix-2 FFT
        var size = 2
        while (size <= n) {
            val halfSize = size / 2
            val tableStep = n / size
            for (i in 0 until n step size) {
                var k = 0
                for (j in i until i + halfSize) {
                    val angle = -2.0 * PI * k / n
                    val cosA = cos(angle).toFloat()
                    val sinA = sin(angle).toFloat()

                    val tempRe = cosA * real[j + halfSize] - sinA * imag[j + halfSize]
                    val tempIm = sinA * real[j + halfSize] + cosA * imag[j + halfSize]

                    real[j + halfSize] = real[j] - tempRe
                    imag[j + halfSize] = imag[j] - tempIm
                    real[j] += tempRe
                    imag[j] += tempIm
                    k += tableStep
                }
            }
            size *= 2
        }
    }

    /**
     * Helper to compute magnitude of FFT from raw PCM short data.
     * output size will be pcmData.size / 2
     */
    fun computeMagnitudes(pcmData: ShortArray): FloatArray {
        val n = pcmData.size
        // Must be padded or truncated to a power of two
        var pow2 = 1
        while (pow2 * 2 <= n) {
            pow2 *= 2
        }

        val real = FloatArray(pow2)
        val imag = FloatArray(pow2)

        for (i in 0 until pow2) {
            // Normalize audio data nicely
            real[i] = pcmData[i].toFloat() / Short.MAX_VALUE.toFloat()
        }

        fft(real, imag)

        // Only need half the results since it's mirrored
        val halfP2 = pow2 / 2
        val magnitudes = FloatArray(halfP2)
        for (i in 0 until halfP2) {
            val re = real[i]
            val im = imag[i]
            // Magnitude is sqrt(re^2 + im^2)
            magnitudes[i] = sqrt(re * re + im * im)
        }
        return magnitudes
    }
}
