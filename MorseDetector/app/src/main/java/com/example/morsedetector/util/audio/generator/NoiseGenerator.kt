package com.example.morsedetector.util.audio.generator

import com.example.morsedetector.model.AudioParams
import com.example.morsedetector.util.audio.transformer.AudioDataTransformer
import com.example.morsedetector.util.audio.file.WavFileWriter
import com.example.morsedetector.util.audio.transformer.FrequencyAudioFilter
import com.example.morsedetector.util.audio.transformer.FrequencyFilter
import com.example.morsedetector.util.audio.transformer.FrequencyPoint
import com.example.morsedetector.util.math.ComplexNumber
import com.example.morsedetector.util.math.FourierTransformer
import com.example.morsedetector.util.math.MathUtil
import java.io.File
import java.lang.Math.pow
import kotlin.math.*
import kotlin.random.Random

class NoiseGenerator {
    var audioParams: AudioParams = AudioParams.createDefault()
//    private var rand = Random(System.currentTimeMillis())
    private val rand = Random(System.currentTimeMillis())
    private val audioFilter = FrequencyAudioFilter()

    private var timeOffset: Float = 0f

    fun generateNoise(dataArray: FloatArray, noiseType: NoiseType, volume: Float) {
        val amplitude = audioParams.samplesAmplitude
//        val bytesCount = dataArray.size * audioParams.encoding.byteRate
//        val durationMs = bytesCount / audioParams.bytesPerMs
//        val startTimeMs = timeOffset
//        val currentTimeMs = startTimeMs
//        val endTimeMs = startTimeMs + durationMs

        for (i in dataArray.indices) {
            val value = (rand.nextFloat().absoluteValue * 2f * amplitude) - amplitude
            dataArray[i] = applyVolume(value, volume)
        }
        val filter = NoiseFilter.createNoiseFilter(noiseType)
        audioFilter.applyFilter(dataArray, dataArray, filter)

//        timeOffset = currentTimeMs.rem(1000f)
    }

//    fun generateNoise(dataArray: FloatArray, volume: Float) {
//        generateWhiteNoise(dataArray, volume)
//
//        val audioDataTransformer =
//            AudioDataTransformer()
//        //filter noise
//        val complexDataSize = MathUtil.getNearestPowerOfTwo(dataArray.size)
//        val complexData = Array<ComplexNumber>(complexDataSize) { ComplexNumber() }
//        audioDataTransformer.floatArrayToComplexArray(dataArray, complexData)
//        fourierTransformer.completeIPFFT(complexData, true)
//        for (i in 0 until complexDataSize) {
//            val maxFrequency = complexDataSize / 8
//
//            val coefficient = ((maxFrequency - i.toFloat()) / maxFrequency).clamp(0f, 1f)
//            complexData[i].r = complexData[i].r * coefficient
//            complexData[i].i = complexData[i].i * coefficient
//        }
//        fourierTransformer.completeIPFFT(complexData, false)
//        for (i in dataArray.indices){
//            dataArray[i] = complexData[i].r
//        }
////        generateNoise(dataArray, volume,  300f)
//    }

    private fun generateRedNoise(dataArray: FloatArray, volume: Float) {
        val amplitude = audioParams.samplesAmplitude
        val bytesCount = dataArray.size * audioParams.encoding.byteRate
        val samplesCount = (bytesCount.toFloat() / audioParams.bytesPerSample).roundToInt()
        val durationMs = bytesCount.toFloat() / audioParams.bytesPerMs
        val deltaMs = durationMs / samplesCount
        val startTimeMs = timeOffset
        var currentTimeMs = startTimeMs
        val endTimeMs = startTimeMs + durationMs

        var dataArrayIdx = 0

        var previousValue = 0f
        while(dataArrayIdx < dataArray.size) {
//            val random = randFloat() // amplitude
//            val argument = (currentTimeMs * random * frequency * 2f * PI) / 1000f
//            var value = amplitude * sin(argument).toFloat()
            val random = (rand.nextFloat() - 0.5f) * amplitude / 5f
            var value = previousValue + random
            value = value.clamp(-amplitude, amplitude)
            previousValue = value
            value = applyVolume(value, volume)
            for (c in 0 until audioParams.channelsCount) {
                dataArray[dataArrayIdx] = value
                dataArrayIdx++
            }
            currentTimeMs += deltaMs
        }
        timeOffset = currentTimeMs.rem(1000f)
    }

    fun randFloat(): Float {
        val randomNumber = rand.nextInt().rem(audioParams.samplesAmplitude)
        var randFloat = pow(randomNumber.toDouble(), 2.0) / audioParams.samplesAmplitude
        randFloat = randFloat / audioParams.samplesAmplitude
//        val differentValues = (audioParams.samplesAmplitude / 20f).roundToInt()
//        return (rand.nextInt().rem(differentValues)).toFloat() / differentValues
        return randFloat.toFloat()
    }

    private fun applyVolume(value: Float, volume: Float): Float {
        return value * volume
    }

    object NoiseFilter {
        private val whiteNoiseFilter = FrequencyFilter()
        private val blueNoiseFilter = FrequencyFilter().apply {
            setPointValue(FrequencyPoint.POINT_30, 0.1f)
            setPointValue(FrequencyPoint.POINT_60, 0.2f)
            setPointValue(FrequencyPoint.POINT_120, 0.3f)
            setPointValue(FrequencyPoint.POINT_250, 0.4f)
            setPointValue(FrequencyPoint.POINT_500, 0.5f)
            setPointValue(FrequencyPoint.POINT_1000, 0.6f)
            setPointValue(FrequencyPoint.POINT_2000, 0.7f)
            setPointValue(FrequencyPoint.POINT_4000, 0.8f)
            setPointValue(FrequencyPoint.POINT_8000, 0.9f)
            setPointValue(FrequencyPoint.POINT_16000, 1f)
        }
        private val violetNoiseFilter = FrequencyFilter().apply {
            setPointValue(FrequencyPoint.POINT_30, 0.01f)
            setPointValue(FrequencyPoint.POINT_60, 0.01f)
            setPointValue(FrequencyPoint.POINT_120, 0.05f)
            setPointValue(FrequencyPoint.POINT_250, 0.1f)
            setPointValue(FrequencyPoint.POINT_500, 0.2f)
            setPointValue(FrequencyPoint.POINT_1000, 0.4f)
            setPointValue(FrequencyPoint.POINT_2000, 0.6f)
            setPointValue(FrequencyPoint.POINT_4000, 0.8f)
            setPointValue(FrequencyPoint.POINT_8000, 0.9f)
            setPointValue(FrequencyPoint.POINT_16000, 1f)
        }
        private val pinkNoiseFilter = FrequencyFilter().apply {
            setPointValue(FrequencyPoint.POINT_30, 1f)
            setPointValue(FrequencyPoint.POINT_60, 0.9f)
            setPointValue(FrequencyPoint.POINT_120, 0.8f)
            setPointValue(FrequencyPoint.POINT_250, 0.7f)
            setPointValue(FrequencyPoint.POINT_500, 0.6f)
            setPointValue(FrequencyPoint.POINT_1000, 0.5f)
            setPointValue(FrequencyPoint.POINT_2000, 0.4f)
            setPointValue(FrequencyPoint.POINT_4000, 0.3f)
            setPointValue(FrequencyPoint.POINT_8000, 0.2f)
            setPointValue(FrequencyPoint.POINT_16000, 0.1f)
        }
        private val brownNoiseFilter = FrequencyFilter().apply {
            setPointValue(FrequencyPoint.POINT_30, 1f)
            setPointValue(FrequencyPoint.POINT_60, 0.9f)
            setPointValue(FrequencyPoint.POINT_120, 0.8f)
            setPointValue(FrequencyPoint.POINT_250, 0.6f)
            setPointValue(FrequencyPoint.POINT_500, 0.4f)
            setPointValue(FrequencyPoint.POINT_1000, 0.2f)
            setPointValue(FrequencyPoint.POINT_2000, 0.1f)
            setPointValue(FrequencyPoint.POINT_4000, 0.05f)
            setPointValue(FrequencyPoint.POINT_8000, 0.01f)
            setPointValue(FrequencyPoint.POINT_16000, 0.01f)
        }

        fun createNoiseFilter(type: NoiseType): FrequencyFilter {
            return when (type) {
                NoiseType.VIOLET -> violetNoiseFilter
                NoiseType.BLUE -> blueNoiseFilter
                NoiseType.WHITE -> whiteNoiseFilter
                NoiseType.PINK -> pinkNoiseFilter
                NoiseType.BROWN -> brownNoiseFilter
            }
        }
    }
}

enum class NoiseType() {
    WHITE,
    PINK,
    BROWN,
    BLUE,
    VIOLET
}

fun main() {
    val audioParams = AudioParams.createDefault()
    val dataTransformer = AudioDataTransformer()
    val fileWriter = WavFileWriter()

    val noiseGenerator = NoiseGenerator()


    val noises = listOf<Pair<NoiseType, String>>(
        Pair(NoiseType.WHITE, "C:\\Users\\Dmitriy\\Desktop\\white_noise.wav"),
        Pair(NoiseType.BROWN, "C:\\Users\\Dmitriy\\Desktop\\brown_noise.wav"),
        Pair(NoiseType.PINK, "C:\\Users\\Dmitriy\\Desktop\\pink_noise.wav"),
        Pair(NoiseType.BLUE, "C:\\Users\\Dmitriy\\Desktop\\blue_noise.wav"),
        Pair(NoiseType.VIOLET, "C:\\Users\\Dmitriy\\Desktop\\violet_noise.wav")
    )
    val dataFloatArray = dataTransformer.generateFloatArray(5000f)

    noises.forEach {
        val resultFile = File(it.second)
        noiseGenerator.generateNoise(dataFloatArray, it.first, 1f)
        fileWriter.write(dataTransformer.floatArrayToByteArray(dataFloatArray))
        fileWriter.complete(resultFile, audioParams)
        fileWriter.release()
    }
}

fun Float.clamp(min: Float, max: Float): Float {
    return if (this > max) {
        max
    } else if (this < min) {
        min
    } else {
        this
    }
}
