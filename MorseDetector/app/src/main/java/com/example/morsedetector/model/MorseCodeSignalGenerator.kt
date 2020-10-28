package com.example.morsedetector.model

import android.util.Log
import androidx.core.math.MathUtils.clamp
import com.example.morsedetector.util.Constants
import com.example.morsedetector.util.FrequencyGenerator
import com.example.morsedetector.util.SilenceGenerator
import kotlinx.coroutines.*
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class MorseCodeSignalGenerator {
    companion object {
        const val LOG_TAG = "MorseCodeGenerator"
    }

    private val frequencyGenerator = FrequencyGenerator()
    private val silenceGenerator = SilenceGenerator()
    private val channels: MutableList<Channel> = mutableListOf()

    var currentVolume: Volume = Volume.fromRatio(1f)
    var currentFrequency: Float = 300f
    var currentWaveformType: WaveformType = WaveformType.SINE
    var currentGroupPerMinute = 12f


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
        generatorJob = coroutineScope.launch(Dispatchers.Default) {
            Log.d(LOG_TAG, "start generator")
            while (isActive) {
                Log.d(LOG_TAG, "generator isActive symbols ${symbolsStack.size}")
                val symbol = symbolsStack.poll() ?: continue
                val unit = symbol.alphabet.getUnitMs(currentGroupPerMinute)
                val soundSequence = symbol.morseCode.getSoundSequence()

                soundSequence.add(MorsePause.BETWEEN_SYMBOLS)

                val sequence =
                    soundSequence.joinToString { "beep = ${it.silence} pause ${it.unitDuration}" }
                Log.d(LOG_TAG, "symbol ${symbol} sequence ${sequence}")

                while (paused && isActive) { /*stuck in loop to pause*/ Log.d(LOG_TAG, "paused")
                }

                soundSequence.forEachIndexed { idx, sound ->
                    val duration = (sound.unitDuration * unit).roundToLong()
//                    val data = if (!sound.silence) {
//                        generateSound(duration)
//                    } else {
//                        generateSilence(duration)
//                    }
//                    sendDataToAllChannels(data)
//                    val delayDuration = duration - generatorTake - 25
//                    if (delayDuration > 0) {
//                        delay(delayDuration)
//                    }
                }
            }
            Log.d(LOG_TAG, "end generator")
        }
    }

    private fun generateSound(floatArray: FloatArray) {
        frequencyGenerator.generate(
            floatArray,
            currentWaveformType,
            currentFrequency,
            currentVolume.getRatio()
        )
    }

    private fun generateSilence(floatArray: FloatArray) {
        silenceGenerator.generate(floatArray)
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