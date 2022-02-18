package com.example.traindatagenerator.util.audio.generator

import com.example.traindatagenerator.model.*
import com.example.traindatagenerator.util.Constants
import com.example.traindatagenerator.util.audio.file.WavFileReader
import com.example.traindatagenerator.util.audio.file.WavFileWriter
import com.example.traindatagenerator.util.audio.transformer.AudioDataTransformer
import com.example.traindatagenerator.util.audio.transformer.AudioMixer
import com.example.traindatagenerator.util.math.clamp
import kotlinx.coroutines.*
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import javax.imageio.ImageIO
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.random.Random
import kotlin.system.measureTimeMillis


class TrainDataGenerator {
    var morseGenerator = MorseCodeAudioFileGenerator()
    val alphabet: Alphabet = Constants.russianAlphabet
    val resultDir: File = File("D:\\train")

    val noiseVolumesRatio = listOf<Float>(0.1f, 0.4f, 0.7f)
    val noiseTypes = listOf<NoiseType>(
            NoiseType.NONE,
            NoiseType.WHITE,
            NoiseType.VIOLET,
            NoiseType.BROWN,
            NoiseType.PINK,
            NoiseType.BLUE
    )
    val signalFrequencies = listOf<Float>(500f, 700f, 900f)
    val volumesRatio = listOf<Float>(0.2f, 0.6f, 1f)
    val signalSpeed = listOf<Float>(8f, 10f, 12f, 14f, 16f, 18f)
    val signalWaveformTypes = listOf<WaveformType>(
            WaveformType.SQUARE,
            WaveformType.SAW_TOOTH,
            WaveformType.TRIANGLE,
            WaveformType.SINE
    )

    fun generate() {
        alphabet.symbols.forEach { symbol ->
            signalSpeed.forEach { groupPerMinute ->
                signalWaveformTypes.forEach { signalWaveformType ->
                    signalFrequencies.forEach { signalFrequency ->
                        volumesRatio.forEach { signalVolumeRatio ->
                            noiseTypes.forEach { noiseType ->
                                noiseVolumesRatio.forEach { noiseVolumeRatio ->
                                    var needGenerate = true
                                    needGenerate = needGenerate && signalVolumeRatio > noiseVolumeRatio
                                    if (needGenerate) {
                                        generateSymbol(
                                                symbol,
                                                groupPerMinute,
                                                signalWaveformType,
                                                signalFrequency,
                                                signalVolumeRatio,
                                                noiseType,
                                                noiseVolumeRatio
                                        )
                                    }
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

    fun generateNoise() {
        val rand = Random(System.currentTimeMillis())
        val audioFileWriter = WavFileWriter()
        val audioParams = AudioParams.createDefault()
        val durationMs = 3000L
        val resultDir: File = File(resultDir, "none")
        if (!resultDir.exists()) {
            resultDir.mkdirs()
        }
        val transformer = AudioDataTransformer()
        val noiseGenerator = NoiseGenerator()
        val noiseTypes = listOf<NoiseType>(
                NoiseType.WHITE,
                NoiseType.VIOLET,
                NoiseType.BROWN,
                NoiseType.PINK,
                NoiseType.BLUE
        )
        val filesCount = 1500
        for (i in 0 until filesCount) {
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

    fun generateSymbol(
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
        morseGenerator.start(3000L)
    }

    fun generateSymbolsSpectrograms() {
        alphabet.symbols.forEach { symbol ->
            val symbolDir = File(resultDir, "symbol_${symbol.symbol}")
            symbolDir.list()?.filter {
                it.endsWith(".wav")
            }?.forEach {
                val audioFile: File = File(symbolDir, it)
                generateFileSpectrogram(audioFile)
            }
        }
    }

    fun generateNoiseSpectrograms() {
        val noiseDir = File(resultDir, "none")
        noiseDir.list()?.filter {
            it.endsWith(".wav")
        }?.forEach {
            val audioFile: File = File(noiseDir, it)
            generateFileSpectrogram(audioFile)
        }
    }

    fun generateFileSpectrogram(file: File, spectrogramSize: Int = 180) {
        val transformer = AudioDataTransformer()
        val spectrogramGenerator = SpectrogramGenerator()
        val durationMs = 3000L
        val resultFileName = file.name.replace(file.extension, "jpg")
        println("result file ${resultFileName}")
        val resultFile = File(file.parent, resultFileName)
//        if (resultFile.exists()) {
//            println("exists")
//            return
//        }

        val audioFileReader = WavFileReader()
        audioFileReader.prepare(file)
        val audioParams = audioFileReader.wavHeader?.audioParams ?: AudioParams.createDefault()
        spectrogramGenerator.params = audioParams
        transformer.audioParams = audioParams

        if (audioFileReader.hasMore()) {
            val rawAudioBuffer = transformer.generateByteArray(durationMs.toFloat())
            transformer.fillByteArrayWithZeros(rawAudioBuffer)
            val readedSize = audioFileReader.read(rawAudioBuffer)
            val audioData = transformer.byteArrayToFloatArray(rawAudioBuffer)
            val spectrogramMap = spectrogramGenerator.generateSpectrogramMap(
                    audioData,
                    spectrogramSize
            )
            saveAudioSpectrogram(spectrogramMap, resultFile)
        }
        audioFileReader.release()

    }

    fun saveAudioSpectrogramAndroid(spectrogramMap: Array<Array<UByte>>, resultFile: File) {
//        val imageSize = spectrogramMap.size
//        val bitmapResult = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888)
//        val canvas = Canvas(bitmapResult)
//        val paint = Paint()
//        paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.CLEAR))
//
//        spectrogramMap.forEachIndexed { idx, spectrogram ->
//            val x = idx.toFloat()
//            spectrogram.forEachIndexed { y, amplitude ->
//                paint.color = Color.rgb(amplitude.toInt(), 0, 0)
//                canvas.drawPoint(x, y.toFloat(), paint)
//            }
//        }
//        val os = FileOutputStream(resultFile)
//        bitmapResult.compress(Bitmap.CompressFormat.JPEG, 80, os)
    }

    fun saveAudioSpectrogram(spectrogramMap: Array<Array<UByte>>, resultFile: File) {
        val imageSize = spectrogramMap.size
        // Constructs a BufferedImage of one of the predefined image types.
        val bufferedImage = BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_RGB)
        // Create a graphics which can be used to draw into the buffered image
        val g2d: Graphics2D = bufferedImage.createGraphics()
        // fill all the image with white

        spectrogramMap.forEachIndexed { x, spectrogram ->
            val avgAmplitude = (spectrogram.sumBy { it.toInt() } / spectrogram.size)
            spectrogram.forEachIndexed { y, amplitude ->
                val red = (amplitude.toInt() - avgAmplitude).clamp(0, 255)
                val green = 0
                val blue = (amplitude.toInt() - red).clamp(0, 255)
                g2d.setColor(Color(red, green, blue))
                g2d.drawRect(x, y, 1, 1)
            }
        }
        g2d.dispose()
        ImageIO.write(bufferedImage, "jpg", resultFile)
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

    fun start(maxDurationMs: Long, coroutineScope: CoroutineScope = GlobalScope) {
        resume()
        var allDurationMs = 0L
        val stackIsEmpty = symbolsStack.isEmpty()
        while (!stackIsEmpty) {
            val symbol = symbolsStack.poll() ?: break
            val unit = symbol.alphabet.getUnitMs(currentGroupPerMinute)
            val soundSequence = mutableListOf<MorseSound>()
            soundSequence.add(MorsePause.BETWEEN_SYMBOLS)
            soundSequence.addAll(symbol.morseCode.getSoundSequence())
            soundSequence.add(MorsePause.BETWEEN_SYMBOLS)

//            val sequence =
//                soundSequence.joinToString { "beep = ${it.silence} pause ${it.unitDuration}" }
//                Log.d(LOG_TAG, "symbol ${symbol} sequence ${sequence}")
            soundSequence.forEachIndexed { idx, sound ->
                val duration = (sound.unitDuration * unit).roundToLong()
                allDurationMs += duration
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
        }

        val additionalSilenceMs = maxDurationMs - allDurationMs
        if (additionalSilenceMs > 0) {
            val silenceBuffer = dataTransformer.generateFloatArray(additionalSilenceMs.toFloat())
            val noiseBuffer = dataTransformer.generateFloatArray(additionalSilenceMs.toFloat())
            noiseGenerator.generateNoise(
                    noiseBuffer,
                    currentNoiseType,
                    currentNoiseVolume.getRatio()
            )
            AudioMixer.mix(silenceBuffer, noiseBuffer, silenceBuffer)
            sendDataToAllChannels(dataTransformer.floatArrayToByteArray(silenceBuffer))
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
    val generator = TrainDataGenerator()
//    generator.generate()
//    generator.generateSymbolsSpectrograms()
//    generator.generateNoise()
//    generator.generateNoiseSpectrograms()

    generator.generateFileSpectrogram(File("C:\\Users\\Dmitriy\\Desktop\\morse.wav"))
    generator.generateFileSpectrogram(File("C:\\Users\\Dmitriy\\Desktop\\morse2.wav"))
    generator.generateFileSpectrogram(File("C:\\Users\\Dmitriy\\Desktop\\morse3.wav"))
    generator.generateFileSpectrogram(File("C:\\Users\\Dmitriy\\Desktop\\morse4.wav"))

//    generator.generateSymbol(
//            Constants.russianAlphabet.findSymbol("Б")!!,
//            8f,
//            WaveformType.SINE,
//            800f,
//            1f,
//            NoiseType.WHITE,
//            0f
//    )

}