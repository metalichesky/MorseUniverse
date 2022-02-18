package com.example.traindatagenerator.util.audio.generator

import com.example.traindatagenerator.model.*
import com.example.traindatagenerator.util.audio.transformer.AudioDataTransformer
import com.example.traindatagenerator.util.audio.transformer.AudioMixer
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.roundToLong
import kotlin.system.measureTimeMillis

class MorseCodeSignalGenerator {
    companion object {
        const val LOG_TAG = "MorseCodeGenerator"
    }

    private val frequencyGenerator =
        FrequencyGenerator()
    private val silenceGenerator =
        SilenceGenerator()
    private val noiseGenerator =
        NoiseGenerator()
    private val channels: MutableList<Channel> = mutableListOf()

    var currentVolume: Volume = Volume.fromRatio(1f)
    var currentNoiseVolume: Volume = Volume.fromRatio(0.5f)
    var currentFrequency: Float = 300f
    var currentWaveformType: WaveformType = WaveformType.SINE
    var currentNoiseType: NoiseType = NoiseType.WHITE
    var currentGroupPerMinute = 12f
    var currentSignalPaddingMs: Long = 300L

    private var startPadding: Boolean = true
    private var endPadding: Boolean = true

    private var currentSymbolIdx: Int = 0

    private var generatorJob: Job? = null

    var paused: Boolean = false
        private set
    val active: Boolean
        get() {
            return generatorJob?.isActive ?: false
        }

    val symbolsStack: Queue<Symbol> = ConcurrentLinkedQueue()//LinkedList<Symbol>()

    fun setText(text: SymbolsText) {
        symbolsStack.clear()
        addText(text)
    }

    fun addText(text: SymbolsText) {
        text.symbols.forEachIndexed { index, symbol ->
            symbolsStack.offer(symbol)
        }
    }

    fun clear() {
        symbolsStack.clear()
    }

    fun start(coroutineScope: CoroutineScope = GlobalScope) {
        resume()
        startPadding = true
        endPadding = true
        generatorJob = coroutineScope.launch(Dispatchers.Default) {
//            Log.d(LOG_TAG, "start generator")
            while (isActive) {
//                Log.d(LOG_TAG, "generator isActive symbols ${symbolsStack.size}")
                val symbol = symbolsStack.poll() ?: continue
                val unit = symbol.alphabet.getUnitMs(currentGroupPerMinute)
                val soundSequence = symbol.morseCode.getSoundSequence()
                val dataTransformer =
                    AudioDataTransformer()

                soundSequence.add(MorsePause.BETWEEN_SYMBOLS)

                val sequence =
                    soundSequence.joinToString { "beep = ${it.silence} pause ${it.unitDuration}" }
//                Log.d(LOG_TAG, "symbol ${symbol} sequence ${sequence}")

                while (paused && isActive) { /*stuck in loop to pause*/
//                    Log.d(LOG_TAG, "paused")
                }

                if (startPadding) {
                    startPadding = false
                    val paddingBuffer = dataTransformer.generateFloatArray(currentSignalPaddingMs.toFloat())
                    noiseGenerator.generateNoise(
                        paddingBuffer,
                        currentNoiseType,
                        currentNoiseVolume.getRatio()
                    )
                    sendDataToAllChannels(dataTransformer.floatArrayToByteArray(paddingBuffer))
                }

                soundSequence.forEachIndexed { idx, sound ->
                    val duration = (sound.unitDuration * unit).roundToLong()

                    val generatorTake = measureTimeMillis {
                        val audioBuffer = dataTransformer.generateFloatArray(duration.toFloat())
                        val noiseBuffer = dataTransformer.generateFloatArray(duration.toFloat())
                        if (!sound.silence) {
                            frequencyGenerator.generate(
                                audioBuffer,
                                currentWaveformType,
                                currentFrequency,
                                currentVolume.getRatio()
                            )
                        } else {
                            silenceGenerator.generate(audioBuffer)
                        }
                        noiseGenerator.generateNoise(
                            noiseBuffer,
                            currentNoiseType,
                            currentNoiseVolume.getRatio()
                        )
                        AudioMixer.mix(audioBuffer, noiseBuffer, audioBuffer)
                        sendDataToAllChannels(dataTransformer.floatArrayToByteArray(audioBuffer))
                    }
                    val delayDuration = duration - generatorTake// - generatorTake - 25
                    if (delayDuration > 0) {
                        delay(delayDuration)
                    }
                }

                if (endPadding) {
                    endPadding = false
                    val paddingBuffer = dataTransformer.generateFloatArray(currentSignalPaddingMs.toFloat())
                    noiseGenerator.generateNoise(
                        paddingBuffer,
                        currentNoiseType,
                        currentNoiseVolume.getRatio()
                    )
                    sendDataToAllChannels(dataTransformer.floatArrayToByteArray(paddingBuffer))
                }
            }
//            Log.d(LOG_TAG, "end generator")
        }
//        generatorJob?.start()
    }

    private suspend fun sendDataToAllChannels(byteArray: ByteArray) {
        channels.forEach {
            it.sendData(byteArray)
        }
    }

    fun pause() {
        paused = true
    }

    fun resume() {
        paused = false
    }

    fun stop() {
        generatorJob?.cancel()
    }

    fun addReceiver(): Channel {
        val newChannel = Channel()
        channels.add(newChannel)
        return newChannel
    }

}

