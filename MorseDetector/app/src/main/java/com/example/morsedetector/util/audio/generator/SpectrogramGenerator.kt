package com.example.morsedetector.util.audio.generator

import com.example.morsedetector.model.AudioParams
import com.example.morsedetector.util.audio.transformer.AudioDataTransformer
import com.example.morsedetector.util.math.FourierTransformer
import kotlin.math.roundToInt

class SpectrogramGenerator() {
    companion object {
        const val LOG_TAG = "FrequencyGenerator"
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

    fun generateSpectrogram(audioData: FloatArray, spectrogramSize: Int): Array<UByte> {
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
        val sizeRatio = complexData.size.toFloat() / spectrogramSize

        val spectrogram: Array<UByte> = Array<UByte>(spectrogramSize) { i ->
            val complexDataFirstIdx = (i * sizeRatio).roundToInt()
            val complexDataLastIdx = ((i + 1) * sizeRatio).roundToInt()
            var avgAmplitude = 0f
            val count = complexDataLastIdx - complexDataFirstIdx
            for(j in complexDataFirstIdx until complexDataLastIdx) {
                avgAmplitude += complexData[j].module() / complexData.size
            }
            var amplitude = (avgAmplitude / count)
            amplitude *= 256
            amplitude.toByte().toUByte()
        }
        return spectrogram
    }
}