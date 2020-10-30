package com.example.morsedetector.util

import com.example.morsedetector.model.AudioParams
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
    val fourierTransformer = FourierTransformer()

    private var timeOffset: Float = 0f

    fun generateNoise(dataArray: FloatArray, noiseType: NoiseType, volume: Float) {
        when(noiseType) {
            NoiseType.WHITE -> generateWhiteNoise(dataArray, volume)
            NoiseType.RED -> generateRedNoise(dataArray, volume)
        }
    }

    fun generateWhiteNoise(dataArray: FloatArray, volume: Float) {
        val amplitude = audioParams.samplesAmplitude
        val bytesCount = dataArray.size * audioParams.encoding.byteRate
        val durationMs = bytesCount / audioParams.bytesPerMs
        val startTimeMs = timeOffset
        val currentTimeMs = startTimeMs
        val endTimeMs = startTimeMs + durationMs

        for (i in dataArray.indices) {
            val value = (rand.nextFloat() * 2 * amplitude) - amplitude
            dataArray[i] = applyVolume(value, volume)
        }
        timeOffset = currentTimeMs.rem(1000f)
    }
    val rand = Random(System.currentTimeMillis())

    fun generateRedNoise(dataArray: FloatArray, volume: Float) {
        generateWhiteNoise(dataArray, volume)

        val audioDataTransformer = AudioDataTransformer()
        //filter noise
        val complexDataSize = MathUtil.getNearestPowerOfTwo(dataArray.size)
        val complexData = Array<ComplexNumber>(complexDataSize) { ComplexNumber() }
        audioDataTransformer.floatArrayToComplexArray(dataArray, complexData)
        fourierTransformer.completeIPFFT(complexData, true)
        for (i in 0 until complexDataSize) {
            val maxFrequency = complexDataSize / 8

            val coefficient = ((maxFrequency - i.toFloat()) / maxFrequency).clamp(0f, 1f)
            complexData[i].r = complexData[i].r * coefficient
            complexData[i].i = complexData[i].i * coefficient
        }
        fourierTransformer.completeIPFFT(complexData, false)
        for (i in dataArray.indices){
            dataArray[i] = complexData[i].r
        }
//        generateNoise(dataArray, volume,  300f)
    }

    fun generateNoise(dataArray: FloatArray, volume: Float, frequency: Float) {
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
}

enum class NoiseType() {
    WHITE,
    PINK,
    RED,
    PERLIN
}

fun main() {
    val audioParams = AudioParams.createDefault()
    val dataTransformer = AudioDataTransformer()
    val fourierTransformer = FourierTransformer()
    val fileWriter = AudioFileWriter()


    val noiseGenerator = NoiseGenerator()
    var bigger = 0
    var smaller = 0
    for (i in 0 until 100) {
        val value = noiseGenerator.randFloat()
        if (value > 0.5) {
            bigger++
        } else {
            smaller++
        }
        println("value ${value}")
    }
    println("bigger ${bigger} smaller ${smaller}")

    val noises = listOf<Pair<NoiseType, String>>(
        Pair(NoiseType.WHITE, "C:\\Users\\Dmitriy\\Desktop\\white_noise.wav"),
        Pair(NoiseType.RED, "C:\\Users\\Dmitriy\\Desktop\\red_noise.wav")
    )

    val dataFloatArray = dataTransformer.generateFloatArray(100f)
    noises.forEach {
        val resultFile = File(it.second)
        for (i in 0 until 100) {
            noiseGenerator.generateNoise(dataFloatArray, it.first, 1f)
            fileWriter.write(dataTransformer.floatArrayToByteArray(dataFloatArray))
        }
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