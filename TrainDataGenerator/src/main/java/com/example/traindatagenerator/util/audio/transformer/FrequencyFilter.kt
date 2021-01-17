package com.example.traindatagenerator.util.audio.transformer

import com.example.traindatagenerator.model.AudioParams
import com.example.traindatagenerator.model.WaveformType
import com.example.traindatagenerator.model.clamp
import com.example.traindatagenerator.util.audio.file.WavFileWriter
import com.example.traindatagenerator.util.audio.generator.FrequencyGenerator
import com.example.traindatagenerator.util.audio.generator.NoiseGenerator
import com.example.traindatagenerator.util.audio.generator.NoiseType
import com.example.traindatagenerator.util.math.ComplexNumber
import com.example.traindatagenerator.util.math.FourierTransformer
import com.example.traindatagenerator.util.math.Spline
import java.io.File

class FrequencyAudioFilter {
    companion object {
        fun getFrequencyResolution(samplesCount: Int, sampleRate: Int): Float {
            return sampleRate.toFloat() / samplesCount
        }
    }

    var audioParams = AudioParams.createDefault()
    val fourierTransformer = FourierTransformer()
    val audioDataTransformer = AudioDataTransformer()

    fun applyFilter(srcArray: FloatArray, resultArray: FloatArray, filter: FrequencyFilter) {
        val complexArray = audioDataTransformer.floatArrayToComplexArray(srcArray)
        fourierTransformer.completeIPFFT(complexArray, true)

        val samplesCount = complexArray.size / audioParams.channelsCount
        val frequencyResolution = getFrequencyResolution(samplesCount, audioParams.sampleRate)
        var valueIdx = 0
        while (valueIdx < complexArray.size) {
            for (channelIdx in 0 until audioParams.channelsCount) {
                val frequencyIdx = valueIdx / audioParams.channelsCount
                val frequency = frequencyIdx * frequencyResolution
                val filterFrequencyRatio = filter.getValue(frequency)
                complexArray[valueIdx].r *= filterFrequencyRatio
                complexArray[valueIdx].i *= filterFrequencyRatio
                valueIdx++
            }
        }

        fourierTransformer.completeIPFFT(complexArray, false)
        audioDataTransformer.complexArrayToFloatArray(complexArray, resultArray)
    }
}

//60, 170, 310, 600, 1К, 3К, 6К, 12К, 14К, 16К
enum class FrequencyPoint(val frequency: Float) {
    POINT_30(30f),
    POINT_60(60f),
    POINT_120(120f),
    POINT_250(250f),
    POINT_500(500f),
    POINT_1000(1000f),
    POINT_2000(2000f),
    POINT_4000(4000f),
    POINT_8000(8000f),
    POINT_16000(16000f)
}

class FrequencyFilter() {
    var pointValues: MutableMap<FrequencyPoint, Float>
    private val spline = Spline()

    init {
        pointValues = mutableMapOf(
            Pair(FrequencyPoint.POINT_30, 1f),
            Pair(FrequencyPoint.POINT_60, 1f),
            Pair(FrequencyPoint.POINT_120, 1f),
            Pair(FrequencyPoint.POINT_250, 1f),
            Pair(FrequencyPoint.POINT_500, 1f),
            Pair(FrequencyPoint.POINT_1000, 1f),
            Pair(FrequencyPoint.POINT_2000, 1f),
            Pair(FrequencyPoint.POINT_4000, 1f),
            Pair(FrequencyPoint.POINT_8000, 1f),
            Pair(FrequencyPoint.POINT_16000, 1f)
        )
        rebuildSpline()
    }

    private fun rebuildSpline() {
        pointValues.forEach {}
        spline.build(
            pointValues.map { it.key.frequency }.toFloatArray(),
            pointValues.map { it.value }.toFloatArray(),
            pointValues.size
        )
    }

    fun getValue(frequency: Float): Float {
        return spline.calculate(frequency).clamp(0f, 1f)
    }

    fun setPointValue(point: FrequencyPoint, ratio: Float) {
        val validRatio = ratio.clamp(0f, 1f)
        pointValues.put(point, validRatio)
        rebuildSpline()
    }
}