package com.example.morsedetector.ui

import android.media.MediaFormat
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.example.morsedetector.R
import com.example.morsedetector.model.AudioParams
import com.example.morsedetector.util.audio.file.AudioDecoder
import com.example.morsedetector.util.audio.file.AudioEncoder
import com.example.morsedetector.util.audio.file.getAudioParams
import com.example.morsedetector.util.audio.player.SamplesPlayer
import com.example.morsedetector.util.audio.transformer.AudioDataTransformer
import com.example.morsedetector.util.audio.transformer.AudioMixer
import kotlinx.android.synthetic.main.fragment_decoder.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.system.measureTimeMillis

class DecoderFragment() : BaseFragment() {
    companion object {
        const val LOG_TAG = "DecoderFragment"
    }

    private var samplesPlayer: SamplesPlayer =
        SamplesPlayer()
    private var audioDecoder: AudioDecoder =
        AudioDecoder()
    private var audioDecoder2: AudioDecoder =
        AudioDecoder()
    private var audioEncoder: AudioEncoder =
        AudioEncoder()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_decoder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnStart.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val time = measureTimeMillis {
                    decode()
                }
                Log.d(LOG_TAG, "decoding take ${time / 1000} sec")
            }
        }
    }


    private fun decode() {
        val videoDir = context?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return
        //Environment.getRootDirectory()
        if (!videoDir.exists()) {
            videoDir.mkdirs()
        }
        println("files dir ${videoDir.absolutePath}")
        val filePaths = videoDir.list()?.toList()?.filter{
            !it.contains("result")
        }?.map {
            File(videoDir, it).absolutePath
        } ?: return

        println("files ${filePaths.joinToString()}")
        val resultFile = File(videoDir, "result_video.mp4")
        println("result file ${resultFile}")
        audioDecoder.setInputFilePath(filePaths[0])
        audioDecoder2.setInputFilePath(filePaths[1])
        val bufferSize = 1024 * 16
        val buffer = ByteArray(bufferSize)
        val buffer2 = ByteArray(bufferSize)
        var shortBuffer = ShortArray(1)
        var shortBuffer2 = ShortArray(1)
        audioDecoder.setup()
        audioDecoder2.setup()
//        val audioWriter = WavFileWriter()
//        audioWriter.prepare()

        var audioParams: AudioParams? = null
        var outputFormat: MediaFormat? = null
        var audioDataTransformer: AudioDataTransformer? = null
        var playerInitialised = false
        var timestamp = 0L

        while (audioDecoder.hasNext() || audioDecoder2.hasNext()) {
            audioDecoder.decodeNextInto(buffer)
            audioDecoder2.decodeNextInto(buffer2)

            Log.d(LOG_TAG, "decoding ${audioDecoder.hasNext()} || ${audioDecoder2.hasNext()}")

            if (!playerInitialised) {
                Log.d(LOG_TAG, "init audio params")
                audioDecoder.outputFormat?.let {
                    audioParams = it.getAudioParams()
                    Log.d(
                        LOG_TAG,
                        "audio params ${audioParams?.sampleRate} ${audioParams?.channelsCount} ${audioParams?.encoding}"
                    )
                    audioEncoder.setInputFormat(it)
                    outputFormat = AudioEncoder.createOutputFormatAAC(it)
                    audioEncoder.setOutputFormat(outputFormat!!)
                    audioEncoder.setOutputFilePath(resultFile.absolutePath)
                    audioEncoder.setup()

                    audioDataTransformer = AudioDataTransformer()
                        .apply {
                        this.audioParams = audioParams!!
                    }
                    shortBuffer = audioDataTransformer!!.byteArrayToShortArray(buffer)
                    shortBuffer2 = audioDataTransformer!!.byteArrayToShortArray(buffer2)
                    playerInitialised = true
                }
            }
            audioDataTransformer?.let { dataTransformer ->
                dataTransformer.byteArrayToShortArray(buffer, shortBuffer)
                dataTransformer.byteArrayToShortArray(buffer2, shortBuffer2)
                AudioMixer.mix(shortBuffer, shortBuffer2, shortBuffer)
                dataTransformer.shortArrayToByteArray(shortBuffer, buffer)
                Log.d(LOG_TAG, "encoding ${timestamp}")
                audioEncoder.encode(buffer, timestamp)
                timestamp += (bufferSize / audioParams!!.bytesPerMs) * 1000
            }
        }
        audioEncoder.encode(ByteArray(0), timestamp, true)
        audioEncoder.release()
        audioDecoder.release()
        audioDecoder2.release()
    }
}

