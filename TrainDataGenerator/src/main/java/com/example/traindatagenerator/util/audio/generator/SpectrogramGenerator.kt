package com.example.traindatagenerator.util.audio.generator

import com.example.traindatagenerator.model.AudioParams
import com.example.traindatagenerator.util.audio.transformer.AudioDataTransformer
import com.example.traindatagenerator.util.audio.transformer.FrequencyAudioFilter
import com.example.traindatagenerator.util.math.FourierTransformer
import kotlin.math.roundToInt


class SpectrogramGenerator() {
    companion object {
        const val LOG_TAG = "FrequencyGenerator"
        val SQUARE_TWO_PI = Math.sqrt(Math.PI * 2).toFloat()
    }

    private val fourierTransformer = FourierTransformer()
    private val audioTransformer = AudioDataTransformer()
    var params: AudioParams = AudioParams.createDefault()
        set(value) {
            field = value
            audioTransformer.audioParams = value
        }


    fun generateSpectrogramMap(audioData: FloatArray, mapSize: Int): Array<Array<UByte>> {
        val dataDurationMs = audioTransformer.getFloatArrayDurationMs(audioData)
        val spectrogramDurationMs = (dataDurationMs / mapSize)
        val map = Array(mapSize) {
            val startMs = spectrogramDurationMs * it
            val endMs = startMs + spectrogramDurationMs
            val cuttedAudioData = audioTransformer.cutFloatArray(audioData, startMs, endMs)
            generateSpectrogram(cuttedAudioData, mapSize)
        }
        return map
    }

    fun generateSpectrogram(audioData: FloatArray, spectrogramSize: Int, maxFrequency: Float = 8000f): Array<UByte> {
        val complexData = if (params.channelsCount == 1) {
            audioTransformer.floatArrayToComplexArray(audioData)
        } else {
            val monoDataArraySize = audioData.size / params.channelsCount
            val monoDataArray = FloatArray(monoDataArraySize)
            var valueIdx = 0
            var avgData = 0f
            for (i in 0 until monoDataArraySize) {
                avgData = 0f
                for (channelIdx in 0 until params.channelsCount) {
                    avgData += audioData[valueIdx] / params.channelsCount
                    valueIdx++
                }
                monoDataArray[i] = avgData
            }
            audioTransformer.floatArrayToComplexArray(monoDataArray)
        }
        fourierTransformer.completeIPFFT(complexData, true)
        val frequencyResolution = FrequencyAudioFilter.getFrequencyResolution(complexData.size, params.sampleRate)
        val maxIndex = (maxFrequency / frequencyResolution).toInt()
        var complexDataSize = complexData.size / 2
        complexDataSize = Math.min(maxIndex, complexDataSize)
        val sizeRatio = complexDataSize.toFloat() / spectrogramSize

        val spectrogram: Array<UByte> = Array<UByte>(spectrogramSize) { idx ->
            val i = spectrogramSize - idx - 1
            val complexDataFirstIdx = (i * sizeRatio).roundToInt()
            val complexDataLastIdx = ((i + 1) * sizeRatio).roundToInt()
            var avgAmplitude = 0f
            val frequency = frequencyResolution * i
            var count = complexDataLastIdx - complexDataFirstIdx
            if (complexDataFirstIdx == complexDataLastIdx) {
                count = 1
                avgAmplitude = complexData[complexDataFirstIdx].module() / complexData.size
            } else {
                for (j in complexDataFirstIdx until complexDataLastIdx) {
                    avgAmplitude += complexData[j].module() / complexData.size
//                    avgAmplitude *= avgAmplitude
                }
            }
            val amplitude = (((avgAmplitude / count) / SQUARE_TWO_PI ) * 255f).roundToInt()
            amplitude.toByte().toUByte()
        }
        return spectrogram
    }
}