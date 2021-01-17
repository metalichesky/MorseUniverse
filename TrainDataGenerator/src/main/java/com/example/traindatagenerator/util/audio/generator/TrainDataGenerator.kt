package com.example.traindatagenerator.util.audio.generator

import com.example.traindatagenerator.model.*
import com.example.traindatagenerator.util.Constants
import com.example.traindatagenerator.util.audio.file.WavFileWriter
import com.example.traindatagenerator.util.audio.transformer.AudioDataTransformer
import com.example.traindatagenerator.util.audio.transformer.AudioMixer
import kotlinx.coroutines.*
import java.io.File
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.system.measureTimeMillis

class TrainDataGenerator {
    lateinit var morseGenerator: MorseCodeSignalGeneratorAsync
    lateinit var channel: ChannelAsync
    var audioFileWriter: WavFileWriter = WavFileWriter()
    val alphabet: Alphabet = Constants.russianAlphabet
    val resultDir: File = File("C:\\Users\\Dmitriy\\Desktop\\train")
    val audioParams: AudioParams = AudioParams.createDefault()

    val noiseVolumesRatio = listOf<Float>(0.2f, 0.4f, 0.6f, 0.8f)
    val noiseTypes = listOf<NoiseType>(
        NoiseType.WHITE,
        NoiseType.VIOLET,
        NoiseType.BROWN,
        NoiseType.BLUE,
        NoiseType.PINK
    )
    val signalFrequencies = listOf<Float>(500f, 700f, 900f)
    val volumesRatio = listOf<Float>(0.4f, 0.6f, 0.8f, 1f)
    val signalSpeed = listOf<Float>(8f, 9f, 10f, 11f, 12f, 14f, 16f, 18f, 20f)
    val signalWaveformTypes = listOf<WaveformType>(
        WaveformType.SQUARE,
        WaveformType.SAW_TOOTH,
        WaveformType.SINE,
        WaveformType.TRIANGLE
    )

    fun generate() {
        morseGenerator = MorseCodeSignalGeneratorAsync()
        alphabet.symbols.forEach { symbol ->
            signalSpeed.forEach { groupPerMinute ->
                signalWaveformTypes.forEach { signalWaveformType ->
                    signalFrequencies.forEach { signalFrequency ->
                        volumesRatio.forEach { signalVolumeRatio ->
                            noiseTypes.forEach { noiseType ->
                                noiseVolumesRatio.forEach { noiseVolumeRatio ->
                                    generateSymbol(
                                        symbol,
                                        groupPerMinute,
                                        signalWaveformType,
                                        signalFrequency,
                                        signalVolumeRatio,
                                        noiseType,
                                        noiseVolumeRatio
                                    )
                                    println("Completed.")
                                }
                            }
                        }
                    }
                }
            }
        }
        println("Train data ready!")
    }

    private fun generateSymbol(
        symbol: Symbol,
        groupPerMinute: Float,
        signalWaveformType: WaveformType,
        signalFrequency: Float,
        signalVolumeRatio: Float,
        noiseType: NoiseType,
        noiseVolumeRatio: Float
    ) {

        val text = SymbolsText().apply {
            addAlphabet(alphabet)
            symbols.add(symbol)
        }
        val outputDir = File(resultDir, "symbol_${symbol.symbol}")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val noiseVolumeString = String.format("%.2f", noiseVolumeRatio)
        val signalVolumeString = String.format("%.2f", signalVolumeRatio)
        val outputFileName =
            "s_${symbol.symbol}_gpm_${groupPerMinute.roundToInt()}_swt_${signalWaveformType.name}_sf_${signalFrequency.roundToInt()}_sv_${signalVolumeString}_nt_${noiseType.name}_nv_${noiseVolumeString}"
        val outputFile = File(outputDir, outputFileName)
        audioFileWriter.prepare()

        println("Prepare file: ${outputFileName}")

        morseGenerator.clear()
        morseGenerator.currentFrequency = signalFrequency
        morseGenerator.currentGroupPerMinute = groupPerMinute
        morseGenerator.currentNoiseType = noiseType
        morseGenerator.currentWaveformType = signalWaveformType
        morseGenerator.currentVolume = Volume.fromRatio(signalVolumeRatio)
        morseGenerator.currentNoiseVolume = Volume.fromRatio(noiseVolumeRatio)
        morseGenerator.setText(text)
        channel = morseGenerator.addReceiver()
        morseGenerator.start()
        while (morseGenerator.active || channel.hasData()) {
            var data: ByteArray? = null
            data = channel.receiveData()
            if (data != null) {
//                println("received data size ${data.size}")
                audioFileWriter.write(data)
            }
        }
        audioFileWriter.complete(outputFile, audioParams)
        audioFileWriter.release()
    }
}

class MorseCodeSignalGeneratorAsync {
    companion object {
        const val LOG_TAG = "MorseCodeGenerator"
    }

    private val frequencyGenerator =
        FrequencyGenerator()
    private val silenceGenerator =
        SilenceGenerator()
    private val noiseGenerator =
        NoiseGenerator()
    private val channels: MutableList<ChannelAsync> = mutableListOf()

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

    private var generatorThread: Thread? = null

    var paused: Boolean = false
        private set
    val active: Boolean
        get() {
            return generatorThread?.isAlive ?: false
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

    fun start() {
        resume()
        startPadding = true
        endPadding = true
        generatorThread = Thread {
            println("start generator")
            while (generatorThread?.isInterrupted == false) {
                println("generator isActive symbols ${symbolsStack.size}")
                val symbol = symbolsStack.poll() ?: continue
                val unit = symbol.alphabet.getUnitMs(currentGroupPerMinute)
                val soundSequence = symbol.morseCode.getSoundSequence()
                val dataTransformer =
                    AudioDataTransformer()

                soundSequence.add(MorsePause.BETWEEN_SYMBOLS)

                val sequence =
                    soundSequence.joinToString { "beep = ${it.silence} pause ${it.unitDuration}" }
                println("symbol ${symbol} sequence ${sequence}")

                while (paused && active) { /*stuck in loop to pause*/ println("paused") }

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
                        Thread.sleep(delayDuration)
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
            println("end generator")
        }
        generatorThread?.start()
//        generatorJob?.start()
    }

    private fun sendDataToAllChannels(byteArray: ByteArray) {
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
        generatorThread?.interrupt()
    }

    fun addReceiver(): ChannelAsync {
        val newChannel = ChannelAsync()
        channels.add(newChannel)
        return newChannel
    }
}


class ChannelAsync() {
    companion object {
        const val LOG_TAG = "Channel"
        val MAX_DURATION = 1000 // store max 2 sec
    }

    private var audioParams: AudioParams =
        AudioParams.createDefault()
    private var dataQueue: Queue<Pair<ByteArray, Int>> = ConcurrentLinkedQueue()//LinkedList()
    private var dataDuration: Int = 0

    var lastReadedTime = System.currentTimeMillis()

    fun setAudioParams(audioParams: AudioParams) {
        this.audioParams = audioParams
    }

    fun sendData(data: ByteArray) {
        val dataDuration = data.size / audioParams.bytesPerMs
        var availableDuration = getAvailableDuration()
        var fromLastRead = System.currentTimeMillis() - lastReadedTime
        while (dataDuration > availableDuration) {
            //freeSpaceForDuration(dataDuration)
            availableDuration = getAvailableDuration()
            println("sendData() ${data} duration ${dataDuration} available ${availableDuration} from last read ${fromLastRead}")
            Thread.sleep(10)
            //wait here for data readed
        }
        dataQueue.offer(Pair(data, dataDuration))
    }


    fun hasData(): Boolean {
        return dataQueue.isNotEmpty()
    }

    fun receiveData(): ByteArray? {
        lastReadedTime = System.currentTimeMillis()
        val data = dataQueue.poll()?.first
        updateDataDuration()
        return data
    }

    private fun updateDataDuration() {
        this.dataDuration = getDataDuration()
    }

    private fun getDataDuration(): Int {
        var duration = 0
        dataQueue.forEach {
            duration += it.second
        }
        return dataQueue.sumBy { it.second }
    }

    private fun getAvailableDuration(): Int {
        return MAX_DURATION - dataDuration
    }

    private fun freeSpaceForDuration(neededDuration: Int) {
        if (neededDuration > MAX_DURATION) return
        while(getAvailableDuration() < neededDuration) {
            dataQueue.poll()
        }
    }
}


fun main() {
    val generator = TrainDataGenerator()
    generator.generate()
}

