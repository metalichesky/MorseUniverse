package com.example.morsedetector.util.math

import com.example.morsedetector.model.WaveformType
import com.example.morsedetector.util.audio.generator.FrequencyGenerator
import com.example.morsedetector.util.audio.generator.NoiseGenerator
import com.example.morsedetector.util.audio.transformer.AudioDataTransformer
import com.example.morsedetector.util.audio.transformer.WindowFunctions
import kotlin.math.*
import kotlin.system.measureNanoTime

class FourierTransformer {
    companion object {
        private val TWO_PI = (Math.PI * 2.0).toFloat()
    }

    // simplest implementation of fourier transform
    // too slow, don't use it
    fun completeDFT(
        dataArray: Array<ComplexNumber>,
        forward: Boolean = true
    ): Array<ComplexNumber> {
        val resultArray = Array<ComplexNumber>(dataArray.size) { ComplexNumber() }
        val sign = if (forward) -1f else 1f
        val coefficient = if (forward) ComplexNumber(1f, 0f)
        else ComplexNumber(1f / dataArray.size, 0f)
        for (k in resultArray.indices) {
            var result = ComplexNumber()
            for (n in dataArray.indices) {
                result += dataArray[n] * ComplexNumber(
                    cos(sign * TWO_PI * k * n / dataArray.size).toFloat(),
                    sin(sign * TWO_PI * k * n / dataArray.size).toFloat()
                )
            }
            resultArray[k] = result * coefficient
        }
        return resultArray
    }

    // fast-fourier transform recursion version with decimation by frequency
    // slow but simple, usable
    fun completeBFRFFT(
        dataArray: Array<ComplexNumber>,
        forward: Boolean = true,
        inRecursion: Boolean = false
    ): Array<ComplexNumber> {
        if (dataArray.size == 1) return dataArray
        val dataSize = dataArray.size
        val halfDataSize = dataSize shr 1

        val sign = if (forward) -1f else 1f
        val arg = sign * TWO_PI / dataSize
        val omegaPowBase = ComplexNumber(cos(arg), sin(arg))
        var omega = ComplexNumber(1f, 0f)

        val result = Array(dataSize) {
            var j = it
            if (it < halfDataSize) {
                dataArray[j] + dataArray[j + halfDataSize]
            } else {
                j -= halfDataSize
                val resultValue = omega * (dataArray[j] - dataArray[j + halfDataSize])
                omega *= omegaPowBase
                resultValue
            }
        }

        var yTop = Array(halfDataSize) { result[it] }
        var yBottom = Array(halfDataSize) { result[it + halfDataSize] }

        yTop = completeBFRFFT(yTop, forward, true)
        yBottom = completeBFRFFT(yBottom, forward, true)

        for (i in 0 until halfDataSize) {
            val j = i shl 1 // i = 2*j;
            result[j] = yTop[i]
            result[j + 1] = yBottom[i]
        }

        if (!inRecursion) {
            val coefficient = if (forward) ComplexNumber(1f, 0f)
            else ComplexNumber(1f / dataSize.toFloat(), 0f)
            for (i in result.indices) {
                result[i] *= coefficient
            }
        }

        return result
    }

    // fast-fourier transform recursion version with decimation by frequency, modify input array
    // slow but simple, usable
    fun completeIPBFRFFT(
        dataArray: Array<ComplexNumber>,
        forward: Boolean = true,
        inRecursion: Boolean = false
    ) {
        if (dataArray.size == 1) return
        val dataSize = dataArray.size
        val halfDataSize = dataSize shr 1

        val sign = if (forward) -1f else 1f
        val arg = sign * TWO_PI / dataSize
        val omegaPowBase = ComplexNumber(cos(arg), sin(arg))
        var omega = ComplexNumber(1f, 0f)

        var yTop = Array(halfDataSize) {
            dataArray[it] + dataArray[it + halfDataSize]
        }
        var yBottom = Array(halfDataSize) {
            val resultValue = omega * (dataArray[it] - dataArray[it + halfDataSize])
            omega *= omegaPowBase
            resultValue
        }

        yTop = completeBFRFFT(yTop, forward, true)
        yBottom = completeBFRFFT(yBottom, forward, true)

        for (i in 0 until halfDataSize) {
            val j = i shl 1 // i = 2*j;
            dataArray[j] = yTop[i]
            dataArray[j + 1] = yBottom[i]
        }

        if (!inRecursion) {
            val coefficient = if (forward) ComplexNumber(1f, 0f)
            else ComplexNumber(1f / dataSize.toFloat(), 0f)
            for (i in dataArray.indices) {
                dataArray[i] *= coefficient
            }
        }
    }

    // fast-fourier transform recursion version with decimation by time
    // slow but simple, usable
    fun completeBTRFFT(
        dataArray: Array<ComplexNumber>,
        forward: Boolean = true,
        inRecursion: Boolean = false
    ): Array<ComplexNumber> {
        if (dataArray.size == 1) return dataArray
        val dataSize = dataArray.size
        val dataHalfSize = dataArray.size shr 1 // frame.Length/2

        val dataOdd = Array(dataHalfSize) {
            val j = it shl 1
            dataArray[j + 1]
        }
        val dataEven = Array(dataHalfSize) {
            val j = it shl 1
            dataArray[j]
        }

        val resultOdd = completeBTRFFT(dataOdd, forward, true)
        val resultEven = completeBTRFFT(dataEven, forward, true)

        val arg = if (forward) -TWO_PI / dataSize else TWO_PI / dataSize
        val omegaPowBase = ComplexNumber(cos(arg), sin(arg))
        var omega = ComplexNumber(1f, 0f)
        val result = Array<ComplexNumber>(dataSize) {
            ComplexNumber()
        }

        for (j in 0 until dataHalfSize) {
            result[j] = resultEven[j] + omega * resultOdd[j]
            result[j + dataHalfSize] = resultEven[j] - omega * resultOdd[j]
            omega *= omegaPowBase
        }

        if (!inRecursion) {
            val coefficient = if (forward) ComplexNumber(1f, 0f)
            else ComplexNumber(1f / dataSize.toFloat(), 0f)
            for (i in result.indices) {
                result[i] = result[i] * coefficient
            }
        }
        return result
    }

    // fast-fourier transform recursion version with decimation by time, modify input array
    // slow but simple, usable
    fun completeIPBTRFFT(
        dataArray: Array<ComplexNumber>,
        forward: Boolean = true,
        inRecursion: Boolean = false
    ) {
        if (dataArray.size == 1) return
        val dataSize = dataArray.size
        val dataHalfSize = dataArray.size shr 1 // frame.Length/2

        val dataOdd = Array(dataHalfSize) {
            val j = it shl 1
            dataArray[j + 1]
        }
        val dataEven = Array(dataHalfSize) {
            val j = it shl 1
            dataArray[j]
        }

        completeIPBTRFFT(dataOdd, forward, true)
        completeIPBTRFFT(dataEven, forward, true)

        val arg = if (forward) -TWO_PI / dataSize else TWO_PI / dataSize
        val omegaPowBase = ComplexNumber(cos(arg), sin(arg))
        var omega = ComplexNumber(1f, 0f)

        for (j in 0 until dataHalfSize) {
            dataArray[j] = dataEven[j] + omega * dataOdd[j]
            dataArray[j + dataHalfSize] = dataEven[j] - omega * dataOdd[j]
            omega *= omegaPowBase
        }

        if (!inRecursion) {
            val coefficient = if (forward) ComplexNumber(1f, 0f)
            else ComplexNumber(1f / dataSize.toFloat(), 0f)
            for (i in dataArray.indices) {
                dataArray[i] = dataArray[i] * coefficient
            }
        }
    }

    // fast-fourier transform non-recursion version (Cooley-Tukey)
    // best of implemented here, recommended to use
    fun completeFFT(dataArray: Array<ComplexNumber>, forward: Boolean): Array<ComplexNumber> {
        val resultArray = Array<ComplexNumber>(dataArray.size) { dataArray[it] }
        completeIPFFT(resultArray, forward)
        return resultArray
    }

    fun completeIPFFT(
        dataArray: Array<ComplexNumber>,
        forward: Boolean
    ) {
        val dataSize = dataArray.size
        val dataSizeLog = MathUtil.logTwo(dataSize)
        for (idx in 0 until dataSize) {
            val idxReversed = MathUtil.reverseBits(idx, dataSizeLog)
            if (idx < idxReversed) {
                val temp = dataArray[idx]
                dataArray[idx] = dataArray[idxReversed]
                dataArray[idxReversed] = temp
            }
        }
        var partLength = 2
        while (partLength <= dataSize) {
            val halfPartLength = partLength shr 1
            val arg = if (forward) TWO_PI / partLength else -TWO_PI / partLength
            val omegaPowBase = ComplexNumber(cos(arg), sin(arg))
            for (i in 0 until dataSize step partLength) {
                var omega = ComplexNumber(1f, 0f)
                for (j in 0 until halfPartLength) {
                    val u = dataArray[i + j]
                    val v = dataArray[i + j + halfPartLength] * omega
                    dataArray[i + j] = u + v
                    dataArray[i + j + halfPartLength] = u - v
                    omega *= omegaPowBase
                }
            }
            partLength = partLength shl 1
        }

        if (!forward) {
            val coefficient = ComplexNumber(1f / dataSize.toFloat(), 0f)
            for (i in dataArray.indices) {
                dataArray[i] = dataArray[i] * coefficient
            }
        }
    }
}


fun main() {
    var fourierTransformer = FourierTransformer()
    val frequencyGenerator: FrequencyGenerator =
        FrequencyGenerator()
    val noiseGenerator =
        NoiseGenerator()
    val dataTransformer: AudioDataTransformer =
        AudioDataTransformer()
    val windowFunctions =
        WindowFunctions()

    val durationMs = 10f
    val data = dataTransformer.generateFloatArray(durationMs)
    windowFunctions.applyWindow(data)

//    noiseGenerator.generateNoise(data, NoiseType.WHITE, 1f)
    frequencyGenerator.generate(data, WaveformType.SINE, 1000f, 1f)

    val complexDataSize = MathUtil.getNearestPowerOfTwo(data.size)
    println("float array size ${data.size} complex data size ${complexDataSize}")
    val complexData = Array<ComplexNumber>(complexDataSize){ ComplexNumber(0f, 0f) }
    dataTransformer.floatArrayToComplexArray(data, complexData)

    var byTimeAvg = 0.0
    var byTimeInPlaceAvg = 0.0
    var byFrequencyAvg = 0.0
    var byFrequencyInPlaceAvg = 0.0
    var fftAvg = 0.0
    var fftInPlaceAvg = 0.0
    val N = 100
    for (i in 0 until N) {
        println("measure ${i+1} of ${N}")
        var complexDataCopy = dataTransformer.copyComplexArray(complexData)
        val byTime = measureNanoTime {
            val resultForward = fourierTransformer.completeBTRFFT(complexData, true)
            val resultBackward = fourierTransformer.completeBTRFFT(resultForward, false)
        }
        complexDataCopy = dataTransformer.copyComplexArray(complexData)
        val byTimeInPlace = measureNanoTime {
            fourierTransformer.completeIPBTRFFT(complexDataCopy, true)
            fourierTransformer.completeIPBTRFFT(complexDataCopy, false)
        }
        complexDataCopy = dataTransformer.copyComplexArray(complexData)
        val byFrequency = measureNanoTime {
            val resultForward = fourierTransformer.completeBFRFFT(complexData, true)
            val resultBackward = fourierTransformer.completeBFRFFT(resultForward, false)
        }
        complexDataCopy = dataTransformer.copyComplexArray(complexData)
        val byFrequencyInPlace = measureNanoTime {
            fourierTransformer.completeIPBFRFFT(complexDataCopy, true)
            fourierTransformer.completeIPBFRFFT(complexDataCopy, false)
        }
        complexDataCopy = dataTransformer.copyComplexArray(complexData)
        val fft = measureNanoTime {
            val resultForward = fourierTransformer.completeFFT(complexData, true)
            val resultBackward = fourierTransformer.completeFFT(complexData, false)
        }
        complexDataCopy = dataTransformer.copyComplexArray(complexData)
        val fftInPlace = measureNanoTime {
            fourierTransformer.completeIPFFT(complexData, true)
            fourierTransformer.completeIPFFT(complexData, false)
        }
        byTimeAvg += byTime
        byTimeInPlaceAvg += byTimeInPlace
        byFrequencyAvg += byFrequency
        byFrequencyInPlaceAvg += byFrequencyInPlace
        fftAvg += fft
        fftInPlaceAvg += fftInPlace
    }

    val coefficient = 1000000f
    byTimeAvg /= N * coefficient
    byTimeInPlaceAvg /= N * coefficient
    byFrequencyAvg /= N * coefficient
    byFrequencyInPlaceAvg /= N * coefficient
    fftAvg /= N * coefficient
    fftInPlaceAvg /= N * coefficient

    println("by time ${byTimeAvg} ms")

    println("by frequency ${byFrequencyAvg} ms")

    println("by time in place ${byTimeInPlaceAvg} ms")

    println("by frequency in place ${byFrequencyInPlaceAvg} ms")

    println("no recursion fft ${fftAvg} ms")

    println("no recursion fft in place ${fftInPlaceAvg} ms")

}
