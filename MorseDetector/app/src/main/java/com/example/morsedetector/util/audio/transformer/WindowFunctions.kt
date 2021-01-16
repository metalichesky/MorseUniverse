package com.example.morsedetector.util.audio.transformer

import com.example.morsedetector.model.AudioParams
import java.lang.Math.cos
import java.lang.Math.exp
import kotlin.math.PI

class WindowFunctions {
    var audioParams: AudioParams = AudioParams.createDefault()

    var windowFunction: WindowFunction =
        GausseWindowFunction()

    fun applyWindow(dataArray: FloatArray) {
        val samplesCount = dataArray.size / audioParams.channelsCount
        val channelsCount = audioParams.channelsCount
        var dataArrayIdx = 0
        val function = windowFunction

        for (sampleIdx in 0 until samplesCount) {
            val w = function.getWindowFactor(sampleIdx, samplesCount)
            for (channelIdx in 0 until channelsCount) {
                dataArray[dataArrayIdx] = dataArray[dataArrayIdx] * w
                dataArrayIdx++
            }
        }

    }

}

abstract class WindowFunction() {
    enum class Type() {
        GAUSSE,
        HENNING,
        HAMMING,
        BLACKMAN
    }

    abstract fun getWindowFactor(n: Int, N: Int): Float
}

class GausseWindowFunction(val q: Float = 0.5f) : WindowFunction() {
    override fun getWindowFactor(n: Int, N: Int): Float {
        val a = (N - 1) * 0.5f
        var t = (n - a) / (q * a)
        t = t * t
        return exp(-t * 0.5).toFloat()
    }
}

class HenningWindowFunction() : WindowFunction() {
    override fun getWindowFactor(n: Int, N: Int): Float {
        return 0.5f * (1.0f - cos(2 * PI * n / (N - 1)).toFloat())
    }
}

class HammingWindowFunction() : WindowFunction() {
    override fun getWindowFactor(n: Int, N: Int): Float {
        return 0.53836f - 0.46164f * cos(2 * PI * n / (N - 1)).toFloat()
    }
}

class BlackmanWindowFunction(alpha: Float = 0.16f) : WindowFunction() {
    private val coefficients: FloatArray = FloatArray(3)

    init {
        coefficients[0] = (1f - alpha) * 0.5f
        coefficients[1] = 0.5f
        coefficients[2] = alpha * 0.5f
    }

    override fun getWindowFactor(n: Int, N: Int): Float {
        return coefficients[0] - coefficients[1] * cos(2.0 * PI * n / (N - 1)).toFloat() + coefficients[2] * cos(
            4.0 * PI * n / (N - 1)
        ).toFloat()
    }
}

class CaiserWindowFunction() : WindowFunction() {
    override fun getWindowFactor(n: Int, N: Int): Float {
        return 0.53836f - 0.46164f * cos(2 * PI * n / (N - 1)).toFloat()
    }
}


fun main() {
    val srcArray = FloatArray(10) { 100f }
    val window =
        WindowFunctions()
    window.windowFunction =
        HammingWindowFunction()
    window.applyWindow(srcArray)
    println(srcArray.joinToString())
}