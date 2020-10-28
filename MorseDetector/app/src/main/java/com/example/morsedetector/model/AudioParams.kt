package com.example.morsedetector.model

import com.example.morsedetector.util.Constants
import kotlin.math.floor
import kotlin.math.pow

class AudioParams(
    val sampleRate: Int,
    val encoding: Encoding,
    val channelsCount: Int
) {
    companion object {
        fun createDefault(): AudioParams {
            return AudioParams(
                Constants.DEFAULT_SAMPLE_RATE,
                Constants.DEFAULT_ENCODING,
                Constants.DEFAULT_CHANNELS_COUNT
            )
        }
    }

    val bytesPerMs: Int  //bytes per one millisecond

    val bytesPerSample: Int //bytes per one sample

    val samplesAmplitude: Float //amplitude value for samples

    init {
        val bytes = (sampleRate.toFloat() * channelsCount * encoding.byteRate) / 1000f
        this.bytesPerMs =  floor(bytes).toInt()
        this.bytesPerSample = channelsCount * encoding.byteRate
        val maxUnsignedNumber = (2.0.pow(encoding.byteRate.toDouble() * 8.0).toFloat())
        this.samplesAmplitude = maxUnsignedNumber / 2f - 1f
    }
}