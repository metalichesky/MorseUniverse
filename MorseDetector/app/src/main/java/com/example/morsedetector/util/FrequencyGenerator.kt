package com.example.morsedetector.util

import android.media.AudioFormat
import android.util.Log
import com.example.morsedetector.model.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.lang.Math.pow
import java.lang.Math.sin
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Duration
import kotlin.math.*
import kotlin.random.Random

class FrequencyGenerator() {
    companion object {
        const val LOG_TAG = "FrequencyGenerator"
    }

    private var params: AudioParams = AudioParams.createDefault()

    private var timeOffset: Float = 0f

    fun generate(dataArray: FloatArray, waveType: WaveformType, frequency: Float, volume: Float) {
        getNextValues(dataArray, waveType, frequency, volume)
    }

    private fun getNextValues(dataArray: FloatArray, waveType: WaveformType, frequency: Float, volume: Float) {
        var valueIdx = 0
        val waveGenerator = when (waveType) {
            WaveformType.SQUARE -> SquareWaveGenerator(params)
            WaveformType.TRIANGLE -> TriangleWaveGenerator(params)
            WaveformType.SAW_TOOTH -> SawToothWaveGenerator(params)
            else -> SineWaveGenerator(params)
        }
        val bytesCount = dataArray.size * params.encoding.byteRate
        val durationMs = bytesCount / params.bytesPerMs
        val startTimeMs = timeOffset
        val endTimeMs = timeOffset + durationMs
        val samplesCount = bytesCount / params.bytesPerSample
        val deltaTimeMs = (endTimeMs - startTimeMs) / samplesCount

        var currentTimeMs = startTimeMs

        while (valueIdx < dataArray.size) {
            var value = waveGenerator.getValue(currentTimeMs, frequency, volume)
            val timeRatio = (currentTimeMs - startTimeMs) / durationMs
            value *= getFadeInRatio(timeRatio)
            for (c in 0 until params.channelsCount) {
                dataArray[valueIdx] = value
                valueIdx++
            }
            currentTimeMs += deltaTimeMs
        }
        timeOffset = currentTimeMs.rem(1000f)
    }

//    fun generate(durationMs: Long, waveType: WaveformType, frequency: Float, volume: Float): ByteArray {
//        val needBytes = (durationMs * params.bytesPerMs).toInt()
//        return getNextBytes(needBytes, waveType, frequency, volume)
//    }
//
//    fun fillBuffer(byteBuffer: ByteBuffer, waveType: WaveformType, frequency: Float, volume: Float) {
//        val bytesCount = byteBuffer.limit()
//        byteBuffer.put(getNextBytes(bytesCount, waveType, frequency, volume))
//    }

//    private fun getNextBytes(bytesCount: Int, waveType: WaveformType, frequency: Float, volume: Float): ByteArray {
//        val byteArray = ByteArray(bytesCount)
//        var byteIdx = 0
//        val waveGenerator = when (waveType) {
//            WaveformType.SQUARE -> SquareWaveGenerator(params)
//            WaveformType.TRIANGLE -> TriangleWaveGenerator(params.encoding.bytePerSample)
//            WaveformType.SAW_TOOTH -> SawToothWaveGenerator(params.encoding.bytePerSample)
//            else -> SineWaveGenerator(params.encoding.bytePerSample)
//        }
//
//        val durationMs = bytesCount.toFloat() / params.bytesPerMs
//        val startTimeMs = timeOffset
//        val endTimeMs = timeOffset + durationMs
//        val samplesCount = bytesCount / params.bytesPerSample
//        val deltaTimeMs = (endTimeMs - startTimeMs) / samplesCount
//
//        var currentTimeMs = startTimeMs
//
//        while (byteIdx < bytesCount) {
//            var value = waveGenerator.getValue(currentTimeMs, frequency, volume)
//            val timeRatio = (currentTimeMs - startTimeMs) / durationMs
//            value *= getFadeInRatio(timeRatio)
//            val valueBytes = waveGenerator.toByteArray(value)
//            for (c in 0 until params.channelsCount) {
//                for (b in 0 until params.encoding.bytePerSample) {
//                    if (byteIdx >= bytesCount) {
//                        break
//                    }
//                    byteArray[byteIdx] = valueBytes[b]
//                    byteIdx++
//                }
//            }
//            currentTimeMs += deltaTimeMs
//        }
//        timeOffset = currentTimeMs.rem(1000f)
//        return byteArray
//    }


    fun getFadeInRatio(currentTimeRatio: Float): Float {
        val fadeDurationRatio = 0.05f
        return if (currentTimeRatio < 0f || currentTimeRatio > 1f) {
            0f
        } else if (currentTimeRatio < fadeDurationRatio) {
            currentTimeRatio / fadeDurationRatio
        } else if (currentTimeRatio > (1f - fadeDurationRatio)) {
            (1f - currentTimeRatio) / fadeDurationRatio
        } else {
            1f
        }
    }

}

const val SQUARE_PI = (PI * PI)

abstract class WaveGenerator(val audioParams: AudioParams) {

    abstract fun getValue(
        currentTimeMs: Float,
        frequency: Float,
        volume: Float
    ): Float

    protected fun applyVolume(result: Float, volume: Float): Float {
        return result * volume
    }

}

class SineWaveGenerator(audioParams: AudioParams) : WaveGenerator(audioParams) {
    override fun getValue(
        currentTimeMs: Float,
        frequency: Float,
        volume: Float
    ): Float {
        val argument = 2f * PI * frequency * currentTimeMs / 1000f
        val currentValue = audioParams.samplesAmplitude * sin(argument).toFloat()
        return applyVolume(currentValue, volume)
    }
}

class SawToothWaveGenerator(audioParams: AudioParams) : WaveGenerator(audioParams) {
    companion object {
        private val K = listOf(1, 2, 3, 4)
    }

    override fun getValue(
        currentTimeMs: Float,
        frequency: Float,
        volume: Float
    ): Float {
        var result = 0f
        val time = currentTimeMs / 1000f
        val argument = 2f * frequency * time * floor(0.5f + frequency * time)
        result = audioParams.samplesAmplitude * argument
        return applyVolume(result, volume)
    }

//    override fun getValue(
//        currentTimeMs: Float,
//        frequency: Float,
//        volume: Float
//    ): Float {
//        var result = 0f
//        val argument = 2f * PI * frequency * currentTimeMs / 1000f
//        for (k in K) {
//            result += sin(k * argument).toFloat() / k
//        }
//        result *= audioParams.samplesAmplitude
//        result /= PI.toFloat()
//        result = audioParams.samplesAmplitude / 2f - result
//        return applyVolume(result, volume)
//    }
}

class TriangleWaveGenerator(audioParams: AudioParams) : WaveGenerator(audioParams) {
    companion object {
        private val K = listOf(1, 3, 5, 7)
    }

    override fun getValue(
        currentTimeMs: Float,
        frequency: Float,
        volume: Float
    ): Float {
        var result = 0f
        val argument = 2f * PI * frequency * currentTimeMs / 1000f
        result = sin(argument).toFloat()
        result = asin(result) * 2f / PI.toFloat()
        result = result * audioParams.samplesAmplitude
        return applyVolume(result, volume)
    }

//    override fun getValue(
//        currentTimeMs: Float,
//        frequency: Float,
//        volume: Float
//    ): Float {
//        var result = 0f
//        val argument = 2f * PI * frequency * currentTimeMs / 1000f
//        for (k in K) {
//            result += pow(-1.0, (k - 1.0) / 2.0).toFloat() * sin(k * argument).toFloat() / (k * k)
//        }
//        result *= 8f * audioParams.samplesAmplitude
//        result /= SQUARE_PI.toFloat()
//        return applyVolume(result, volume)
//    }
}

class SquareWaveGenerator(audioParams: AudioParams) : WaveGenerator(audioParams) {
    companion object {
        private val K = listOf(1, 3, 5, 7)
    }

    override fun getValue(
        currentTimeMs: Float,
        frequency: Float,
        volume: Float
    ): Float {
        var result = 0f
        val argument = 2f * PI * frequency * currentTimeMs / 1000f
        result = sign(sin(argument)).toFloat()
        result = result * audioParams.samplesAmplitude
        return applyVolume(result, volume)
    }
}

fun main() {
    val audioParams = AudioParams.createDefault()
    val dataTransformer = AudioDataTransformer()
    val fileWriter = AudioFileWriter()
    val resultFile = File("C:\\Users\\Dmitriy\\Desktop\\morse.wav")
    resultFile.createNewFile()
    val frequencyGenerator = FrequencyGenerator()
    val silenceGenerator = SilenceGenerator()
    val noiseGenerator = NoiseGenerator()

    val rand = Random(System.currentTimeMillis())
    val textSize = 60 * 5
    val text = mutableListOf<Symbol>()
    for (i in 0 until textSize) {
        text.add(Constants.russianAlphabet.getRandSymbol())
    }
    println("generated text: ${text.joinToString()}")
    val map = mutableMapOf<Symbol, Int>()
    for(symbol in Constants.russianAlphabet.symbols) {
        if (map[symbol] == null) {
            map.put(symbol, 0)
        }
        text.forEach {
            if (symbol.toString() == it.toString()) {
                val prev = map[symbol] ?: 0
                map[symbol] = prev + 1
            }
        }
    }
    map.forEach {
        println("symbol ${it.key.symbol} freq ${it.key.frequency} count ${it.value}")
    }

    var index = 0
    val groupsPerMinute = 12f
    val frequency = 800f
    val volume = 1.0f
    val noiseVolume = 0.5f
    val waveformType = WaveformType.SINE

    val unit = Constants.russianAlphabet.getUnitMs(groupsPerMinute)
    println("unit duration ${unit}")
    val sequence = mutableListOf<MorseSound>()
    text.forEachIndexed { idx, symbol ->
        sequence.addAll(symbol.morseCode.getSoundSequence())
        if (idx > 0 && idx % Constants.MORSE_GROUP_SIZE == 0) {
            sequence.add(MorsePause.BETWEEN_GROUPS)
        } else {
            sequence.add(MorsePause.BETWEEN_SYMBOLS)
        }
    }

    sequence.forEach {
        val duration = it.unitDuration * unit
        val dataFloatArray = dataTransformer.generateFloatArray(duration)
        val noiseFloatArray = dataTransformer.generateFloatArray(duration)
        val noiseFloatArray2 = dataTransformer.generateFloatArray(duration)

        noiseGenerator.generateNoise(noiseFloatArray, NoiseType.RED, 1f)
//        noiseGenerator.generateNoise(noiseFloatArray2, NoiseType.WHITE, 0.2f)
        if (!it.silence) {
            frequencyGenerator.generate(dataFloatArray, waveformType, frequency, volume)
        } else {
            silenceGenerator.generate(dataFloatArray)
        }

        AudioMixer.mix(dataFloatArray, noiseFloatArray, dataFloatArray)
        AudioMixer.mix(dataFloatArray, noiseFloatArray2, dataFloatArray)

        fileWriter.write(dataTransformer.floatArrayToByteArray(dataFloatArray))
    }
    fileWriter.complete(resultFile, audioParams)
    fileWriter.release()

//    val noises = listOf<Pair<NoiseType, String>>(
//        Pair(NoiseType.WHITE, "C:\\Users\\Dmitriy\\Desktop\\white_noise.wav"),
//        Pair(NoiseType.RED, "C:\\Users\\Dmitriy\\Desktop\\red_noise.wav")
//    )
//
//    val dataFloatArray = dataTransformer.generateFloatArray(5000f)
//    noises.forEach {
//        val resultFile = File(it.second)
//        noiseGenerator.generateNoise(dataFloatArray, it.first, noiseVolume)
//        fileWriter.write(dataTransformer.floatArrayToByteArray(dataFloatArray))
//        fileWriter.complete(resultFile, audioParams)
//        fileWriter.release()
//    }

}

