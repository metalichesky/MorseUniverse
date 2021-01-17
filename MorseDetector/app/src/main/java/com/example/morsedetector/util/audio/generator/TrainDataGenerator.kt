package com.example.morsedetector.util.audio.generator

import android.graphics.Bitmap
import android.graphics.Color
import com.example.morsedetector.model.*
import com.example.morsedetector.util.Constants
import com.example.morsedetector.util.audio.file.WavFileReader
import com.example.morsedetector.util.audio.file.WavFileWriter
import com.example.morsedetector.util.audio.transformer.AudioDataTransformer
import com.example.morsedetector.util.audio.transformer.AudioMixer
import com.example.morsedetector.util.math.clamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.imageio.ImageIO
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.random.Random
import kotlin.system.measureTimeMillis


class TrainDataGenerator {
    lateinit var morseGenerator: MorseCodeAudioFileGenerator
    val alphabet: Alphabet = Constants.russianAlphabet
    val resultDir: File = File("C:\\Users\\Dmitriy\\Desktop\\train")

    val noiseVolumesRatio = listOf<Float>(0.1f, 0.4f, 0.8f)
    val noiseTypes = listOf<NoiseType>(
        NoiseType.WHITE,
        NoiseType.VIOLET,
        NoiseType.BROWN
    )
    val signalFrequencies = listOf<Float>(500f, 700f, 900f)
    val volumesRatio = listOf<Float>(0.2f, 0.5f, 1f)
    val signalSpeed = listOf<Float>(8f, 10f, 12f, 14f, 16f, 18f)
    val signalWaveformTypes = listOf<WaveformType>(
        WaveformType.SQUARE,
        WaveformType.SAW_TOOTH,
        WaveformType.TRIANGLE
    )

    fun generate() {
        morseGenerator = MorseCodeAudioFileGenerator()
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

    private fun generateNoise() {
        val rand = Random(System.currentTimeMillis())
        val audioFileWriter = WavFileWriter()
        val audioParams = AudioParams.createDefault()
        val durationMinMs = 500L
        val durationMaxMs = 1500L
        val resultDir: File = File(resultDir, "none")
        val transformer = AudioDataTransformer()
        val noiseGenerator = NoiseGenerator()
        val noiseTypes = listOf<NoiseType>(
            NoiseType.WHITE,
            NoiseType.VIOLET,
            NoiseType.BROWN
        )
        val filesCount = 1000
        for (i in 0 until filesCount) {
            val durationMs = rand.nextInt().rem(durationMaxMs).clamp(durationMinMs, durationMaxMs)
            val fileName = "s_none_idx_${i}_d_${durationMs}.wav"
            val resultFile = File(resultDir, fileName)
            val noiseBuffer = transformer.generateFloatArray(durationMs.toFloat())
            val noiseType = noiseTypes.random(rand)
            val noiseVolumeRatio = rand.nextFloat().absoluteValue.clamp(0f, 1f)
            noiseGenerator.generateNoise(noiseBuffer, noiseType, noiseVolumeRatio)
            audioFileWriter.prepare()
            audioFileWriter.write(transformer.floatArrayToByteArray(noiseBuffer))
            audioFileWriter.complete(resultFile, audioParams)
            audioFileWriter.release()
            println("complete file ${fileName}")
        }
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
            "s_${symbol.symbol}_gpm_${groupPerMinute.roundToInt()}_swt_${signalWaveformType.name}_sf_${signalFrequency.roundToInt()}_sv_${signalVolumeString}_nt_${noiseType.name}_nv_${noiseVolumeString}.wav"
        val outputFile = File(outputDir, outputFileName)
        println("Prepare file: ${outputFileName}")

        if (outputFile.exists()) {
            println("exists!")
            return
        }


        morseGenerator.clear()
        morseGenerator.currentFrequency = signalFrequency
        morseGenerator.currentGroupPerMinute = groupPerMinute
        morseGenerator.currentNoiseType = noiseType
        morseGenerator.currentWaveformType = signalWaveformType
        morseGenerator.currentVolume = Volume.fromRatio(signalVolumeRatio)
        morseGenerator.currentNoiseVolume = Volume.fromRatio(noiseVolumeRatio)
        morseGenerator.setText(text)
        morseGenerator.setOutputFile(outputFile)
        morseGenerator.start()
    }

    fun generateSymbolsSpectrograms() {
        alphabet.symbols.forEach { symbol ->
            val symbolDir = File(resultDir, "symbol_${symbol.symbol}")
            symbolDir.list()?.forEach {
                val audioFile: File = File(symbolDir, it)
                generateSymbolSpectrogram(audioFile)
            }
        }
    }


    fun generateSymbolSpectrogram(file: File) {
        val transformer = AudioDataTransformer()
        val spectrogramGenerator = SpectrogramGenerator()
        val durationMs = 3000L

        val audioFileReader = WavFileReader()
        audioFileReader.prepare(file)
        val audioParams = audioFileReader.wavHeader?.audioParams ?: AudioParams.createDefault()
        spectrogramGenerator.params = audioParams
        transformer.audioParams = audioParams
        println("audioParams = ${audioParams}")

        val spectrogramSize = 100
        if (audioFileReader.hasMore()) {
            val rawAudioBuffer = transformer.generateByteArray(durationMs.toFloat())
            val readedSize = audioFileReader.read(rawAudioBuffer)
            val audioData = transformer.byteArrayToFloatArray(rawAudioBuffer)
            val spectrogramMap = spectrogramGenerator.generateSpectrogramMap(
                audioData,
                spectrogramSize
            )
            saveAudioSpectrogram(spectrogramMap)
        }
        audioFileReader.release()

    }

    fun saveAudioSpectrogram(spectrogramMap: Array<Array<UByte>>, file: File) {

//        val width = 250
//        val height = 250
//
//        // Constructs a BufferedImage of one of the predefined image types.
//        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
//        // Create a graphics which can be used to draw into the buffered image
//        val g2d: Graphics2D = bufferedImage.createGraphics()
//        // fill all the image with white
//        g2d.setColor(Color.white)
//        g2d.fillRect(0, 0, width, height)
//
//        // create a circle with black
//
//        // create a circle with black
//        g2d.setColor(Color.black)
//        g2d.fillOval(0, 0, width, height)
//
//        // create a string with yellow
//
//        // create a string with yellow
//        g2d.setColor(Color.yellow)
//        g2d.drawString("Java Code Geeks", 50, 120)
//
//        // Disposes of this graphics context and releases any system resources that it is using.
//
//        // Disposes of this graphics context and releases any system resources that it is using.
//        g2d.dispose()
//
//        // Save as PNG
//
//        // Save as PNG
//        var file = File("myimage.png")
//        ImageIO.write(bufferedImage, "png", file)
//
//        // Save as JPEG
//
//        // Save as JPEG
//        file = File("myimage.jpg")
//        ImageIO.write(bufferedImage, "jpg", file)
    }
}


class MorseCodeAudioFileGenerator {
    companion object {
        const val LOG_TAG = "MorseCodeGenerator"
    }

    val audioParams: AudioParams = AudioParams.createDefault()
    val dataTransformer = AudioDataTransformer()
    var audioFileWriter: WavFileWriter = WavFileWriter()
    private val frequencyGenerator =
        FrequencyGenerator()
    private val silenceGenerator =
        SilenceGenerator()
    private val noiseGenerator =
        NoiseGenerator()

    var currentVolume: Volume = Volume.fromRatio(1f)
    var currentNoiseVolume: Volume = Volume.fromRatio(0.5f)
    var currentFrequency: Float = 300f
    var currentWaveformType: WaveformType = WaveformType.SINE
    var currentNoiseType: NoiseType = NoiseType.WHITE
    var currentGroupPerMinute = 12f

    private var currentSymbolIdx: Int = 0
    private var outputFile: File? = null

    var paused: Boolean = false
        private set

    val symbolsStack: Queue<Symbol> = ConcurrentLinkedQueue()//LinkedList<Symbol>()

    fun setOutputFile(outputFile: File) {
        this.outputFile = outputFile
    }

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
//            Log.d(LOG_TAG, "start generator")
        val stackIsEmpty = symbolsStack.isEmpty()
        while (!stackIsEmpty) {
//                Log.d(LOG_TAG, "generator isActive symbols ${symbolsStack.size}")
            val symbol = symbolsStack.poll() ?: break
            val unit = symbol.alphabet.getUnitMs(currentGroupPerMinute)
            val soundSequence = mutableListOf<MorseSound>()
            soundSequence.add(MorsePause.BETWEEN_SYMBOLS)
            soundSequence.addAll(symbol.morseCode.getSoundSequence())
            soundSequence.add(MorsePause.BETWEEN_SYMBOLS)


//            val sequence =
//                soundSequence.joinToString { "beep = ${it.silence} pause ${it.unitDuration}" }
//                Log.d(LOG_TAG, "symbol ${symbol} sequence ${sequence}")

//            while (paused && !stackIsEmpty) { /*stuck in loop to pause*/
//                    Log.d(LOG_TAG, "paused")
//            }

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
//                val delayDuration = duration - generatorTake// - generatorTake - 25
//                if (delayDuration > 0) {
//                    delay(delayDuration)
//                }
            }
        }
        outputFile?.let {
            audioFileWriter.complete(it, audioParams)
        }
        audioFileWriter.release()
    }

    private fun sendDataToAllChannels(byteArray: ByteArray) {
        audioFileWriter.write(byteArray)
    }

    fun pause() {
        paused = true
    }

    fun resume() {
        paused = false
    }

}


fun main() {




}

