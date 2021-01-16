package com.example.morsedetector.util.audio.recorder

import android.media.*
import android.util.Log
import com.example.morsedetector.util.audio.transformer.SimpleResampler
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min
import kotlin.math.roundToInt


class AudioRecorder{
    companion object {
        val rates = intArrayOf(8000, 11025, 22050, 44100, 48000)
        val channels = intArrayOf(AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO)
        val encodings = intArrayOf(AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT)
        const val ENCODER_TYPE = "audio/mp4a-latm"
        const val LOG_TAG = "AudioRecorder"
    }

    private var enabledChannels = AudioFormat.CHANNEL_IN_MONO
    private var enabledSampleRate = 8000
    private var recordingSampleRate = 8000
    private var encodingSampleRate = 8000
    private var enabledEncoding = AudioFormat.ENCODING_PCM_16BIT
    private var channelsCount = 1
    private var outputFormat: MediaFormat = MediaFormat()
    private var minBufferSize = 0
    private var transitionBufferSize = minBufferSize*2
    private var bytesPerSecond = 0
    private var speed = 1f

    private var recorder: AudioRecord? = null
    private var encoder: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var resampler: SimpleResampler? = null

    private var recording = false
    private var recordingStarted = false
    private var resamplingFinished = false

    private var audioFile: File? = null
    private var tempFile: File? = null

    private var recorderThread: Thread? = null
    private var resamplerThread: Thread? = null
    private var converterThread: Thread? = null

    private lateinit var recordedQueue: ConcurrentLinkedQueue<ByteArray>
    private lateinit var resampledQueue: ConcurrentLinkedQueue<ByteArray>

    var onAudioRecorded: ()->Unit = {}
    var onAudioSaved: (filePath: String)->Unit = {}

    constructor(){
        getBestAudioSettings()
    }

    fun getBestAudioSettings(){
        for (enc in encodings) {
            for (ch in channels) {
                for (rate in rates) {
                    val t = AudioRecord.getMinBufferSize(rate, ch, enc)
                    if (t != AudioRecord.ERROR && t != AudioRecord.ERROR_BAD_VALUE) {
                        enabledEncoding = enc
                        enabledChannels = ch
                        enabledSampleRate = rate
                        minBufferSize = t
                    }
                }
            }
        }
        channelsCount =  if (enabledChannels == AudioFormat.CHANNEL_IN_MONO) 1 else 2
        Log.d(
            LOG_TAG,"Best settings: " +
                "sample rate = ${enabledSampleRate}, " +
                "encoding = ${if (enabledEncoding == AudioFormat.ENCODING_PCM_8BIT) "8bit" else "16bit"}, " +
                "channels = ${if (enabledChannels == AudioFormat.CHANNEL_IN_MONO) "mono" else "stereo"}, " +
                "min buffer size = $minBufferSize")
        transitionBufferSize = minBufferSize*2
        bytesPerSecond = enabledSampleRate*channelsCount
        bytesPerSecond *= if (enabledEncoding == AudioFormat.ENCODING_PCM_8BIT) 1 else 2
    }

    fun setOutputFile(filePath: String){
        audioFile = File(filePath)
    }

    fun setAudioSpeed(newSpeed: Float){
        this.speed = newSpeed
        encodingSampleRate = enabledSampleRate
        recordingSampleRate = enabledSampleRate
    }

    fun prepare(){
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            recordingSampleRate,
            enabledChannels,
            enabledEncoding,
            minBufferSize*10
        )
        resampler =
            SimpleResampler()
        resampler.let {
            it!!.bitrate = if (enabledEncoding == AudioFormat.ENCODING_PCM_8BIT) 8 else 16
            it.channelsCount = channelsCount
            it.srcSampleRate = enabledSampleRate
            it.dstSampleRate = (enabledSampleRate/speed).roundToInt()
            it.prepare()
        }
        outputFormat = getFormat(encodingSampleRate)
        recordingStarted = false
        recording = false
        resamplingFinished = false
        if (!audioFile!!.exists()){
            audioFile!!.createNewFile()
        }
    }

    fun isRecording(): Boolean{
        return (recorderThread != null && recorderThread!!.isAlive)
                || (resamplerThread != null && resamplerThread!!.isAlive)
                || (converterThread != null && converterThread!!.isAlive)
    }

    fun startRecording(){
        if (recorder == null){
            Log.d(LOG_TAG,"Recorder can't started: need call prepare() first")
            return
        }

        if (recorder!!.recordingState != AudioRecord.STATE_INITIALIZED){
            Log.d(LOG_TAG,"Recorder can't started: need call prepare() first")
            return
        }

        if (tempFile == null && audioFile == null){
            Log.d(LOG_TAG,"Recorder can't started: need set output file first")
            return
        }

        recordedQueue = ConcurrentLinkedQueue()
        resampledQueue = ConcurrentLinkedQueue()

        recorderThread = Thread{
            record()
        }
        resamplerThread = Thread{
            resample()
        }
        converterThread = Thread{
            convert()
        }

        recorderThread?.start()
        resamplerThread?.start()
        converterThread?.start()
    }

    private fun record(){
        val buffer = ByteArray(transitionBufferSize)
        try{
            recorder!!.startRecording()
            recording = true
            Log.d(LOG_TAG,"Recorder started")
        }
        catch(ex: IllegalStateException){
            Log.e(LOG_TAG,"Cannot start recorder!")
            ex.printStackTrace()
        }
        while(recording){
            val readResult = recorder!!.read(buffer, 0, buffer.size)
            if (readResult == AudioRecord.ERROR_INVALID_OPERATION){
                Log.d(LOG_TAG,"Error reading: invalid operation")
            }
            else if (readResult == AudioRecord.ERROR_BAD_VALUE){
                Log.d(LOG_TAG,"Error reading: bad value")
            }
            else if (readResult == AudioRecord.ERROR_DEAD_OBJECT){
                Log.d(LOG_TAG, "Error reading: dead object")
            }
            else if (readResult != AudioRecord.ERROR){
                //put recorded data to first queue
                recordedQueue.add(buffer.clone())
                recordingStarted = true
            }
        }
        recordingStarted = true
        try{
            try{
                recorder!!.stop()
                recorder!!.release()
            }
            catch (ex: IllegalStateException){
                Log.d(LOG_TAG,"Recorder can't stop")
                ex.printStackTrace()
            }
        }
        catch(ex: Exception){
            ex.printStackTrace()
        }
        finally{
            recorder = null
            onAudioRecorded()
        }
    }

    private fun resample(){
        resamplingFinished = false
        while((recorderThread!= null && recorderThread!!.isAlive) || recordedQueue.isNotEmpty()) {
            //getting recorded data from first queue and resample it
            val recordedBytes = recordedQueue.poll()
            recordedBytes?.apply{
                val resampledBytes = resampler!!.resample(recordedBytes)
                //after resampling, put resampled bytes to another queue
                resampledQueue.add(resampledBytes)
            }
        }
        resamplingFinished = true
    }

    private fun convert(){
        try {
            MediaCodecList(MediaCodecList.ALL_CODECS).findEncoderForFormat(outputFormat)
            muxer = MediaMuxer(audioFile!!.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            encoder = MediaCodec.createEncoderByType(ENCODER_TYPE)
            try {
                encoder!!.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            }
            catch(ex: java.lang.Exception){
                ex.printStackTrace()
            }
            try {
                encoder!!.start()
            }
            catch(ex: java.lang.Exception){
                ex.printStackTrace()
            }

            val outBuffInfo = MediaCodec.BufferInfo()
            var hasMoreData = true
            var presentationTimeUs = 0.0
            var audioTrackIdx = 0
            var totalBytesRead = 0

            do {
                //getting resampled bytes from queue
                var resampledBytes = resampledQueue.poll()
                while(!resamplingFinished && resampledBytes == null){
                    resampledBytes = resampledQueue.poll()
                }
                val resampledBytesCount = if (resampledBytes != null) resampledBytes.size else 0
                var convertedBytesCount = 0
                do {
                    var inputBufIndex = 0
                    if((inputBufIndex != -1) && hasMoreData) {
                        inputBufIndex = encoder!!.dequeueInputBuffer(-1)
                        if (inputBufIndex >= 0) {
                            val dstBuf = encoder!!.getInputBuffer(inputBufIndex)
                            dstBuf!!.clear()
                            val bytesRead = min(resampledBytesCount - convertedBytesCount, dstBuf.limit())
                            if (resampledBytes == null && resamplingFinished) {
                                hasMoreData = false
                                //put EOS flag to end converting
                                encoder!!.queueInputBuffer(
                                    inputBufIndex,
                                    0,
                                    0,
                                    presentationTimeUs.toLong(),
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                            }

                            if (resampledBytes != null && bytesRead > 0) {
                                val tempBuffer = resampledBytes.copyOfRange(convertedBytesCount, convertedBytesCount + bytesRead)
                                totalBytesRead += bytesRead
                                //put and enqueue data to media codec
                                dstBuf.put(tempBuffer, 0, bytesRead)
                                presentationTimeUs = (1000000.0 * totalBytesRead) / (bytesPerSecond.toDouble())
                                encoder!!.queueInputBuffer(
                                    inputBufIndex,
                                    0,
                                    bytesRead,
                                    presentationTimeUs.toLong(),
                                    0
                                )
                                convertedBytesCount += bytesRead
                            }
                        }
                    }

                    var outputBufIndex = encoder!!.dequeueOutputBuffer(outBuffInfo, 100)
                    while (outputBufIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                        if (outputBufIndex >= 0) {
                            val encodedData = encoder!!.getOutputBuffer(outputBufIndex)
                            //getting encoded data from media codec
                            encodedData!!.position(outBuffInfo.offset)
                            encodedData.limit(outBuffInfo.offset + outBuffInfo.size)
                            if (outBuffInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0 && outBuffInfo.size != 0) {
                                //input codec specific data before writing sample data
                                encoder!!.releaseOutputBuffer(outputBufIndex, false)
                            } else {
                                //put encoded audio data to muxer
                                muxer!!.writeSampleData(
                                    audioTrackIdx,
                                    encodedData,
                                    outBuffInfo
                                )
                                encoder!!.releaseOutputBuffer(outputBufIndex, false)
                            }
                        } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            outputFormat = encoder!!.outputFormat
                            audioTrackIdx = muxer!!.addTrack(outputFormat)
                            muxer!!.start()
                        } else if (outputBufIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                            Log.e(
                                LOG_TAG,
                                "Unknown return code from dequeueOutputBuffer - $outputBufIndex"
                            )
                        }
                        outputBufIndex = encoder!!.dequeueOutputBuffer(outBuffInfo, 0)
                    }
                } while (convertedBytesCount < resampledBytesCount)
            } while (outBuffInfo.flags != MediaCodec.BUFFER_FLAG_END_OF_STREAM)

            try {
                muxer!!.stop()
            }
            catch(ex: java.lang.Exception){
                ex.printStackTrace()
            }
            try{
                muxer!!.release()
            }
            catch(ex: java.lang.Exception){
                ex.printStackTrace()
            }
        }
        catch(ex: java.lang.Exception){
            ex.printStackTrace()
        }
        finally{
            onAudioSaved(audioFile!!.absolutePath)
        }
    }

    private fun getFormat(rate: Int): MediaFormat {
        val format = MediaFormat.createAudioFormat("audio/mp4a-latm", rate, channelsCount)
        format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm")
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelsCount)
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, rate)
        format.setInteger(MediaFormat.KEY_BIT_RATE, 128 * 1024)
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        return format
    }

    fun stopRecording(){
        recording = false
    }

    private fun resampleAudio(){
        val lastName = tempFile!!.absolutePath
        var tempAudioFile = File(tempFile!!.absolutePath+"_temp")
        tempAudioFile.createNewFile()
        val inputFileStream = FileInputStream(tempFile)
        val outputFileStream = FileOutputStream(tempAudioFile)

        val buffer = ByteArray(enabledSampleRate*channelsCount)

        var readedCount = inputFileStream.read(buffer, 0, buffer.size)
        while(readedCount != -1) {
            val output = resampler?.resample(buffer)
            output?.let{
                outputFileStream.write(it)
            }
            readedCount = inputFileStream.read(buffer)
        }
        try {
            inputFileStream.close()
        }
        catch(exception: Exception){
            Log.d(LOG_TAG,exception.toString())
            exception.printStackTrace()
        }
        try {
            outputFileStream.flush()
            outputFileStream.close()
        }
        catch(exception: Exception){
            Log.d(LOG_TAG,exception.toString())
            exception.printStackTrace()
        }

        val currentFile = File(lastName)
        currentFile.delete()
        tempAudioFile = File(lastName+"_temp")
        tempAudioFile.renameTo(File(lastName))
        tempFile = File(lastName)
    }

    private fun putAudioToContainer(){
        if (!audioFile!!.exists()){
            audioFile!!.createNewFile()
        }
        try {
            val fis = FileInputStream(tempFile!!)
            var format = getFormat(encodingSampleRate)


            muxer = MediaMuxer(audioFile!!.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            encoder = MediaCodec.createEncoderByType("audio/mp4a-latm")
            try {
                encoder!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            }
            catch(ex: java.lang.Exception){
                ex.printStackTrace()
            }
            try {
                encoder!!.start()
            }
            catch(ex: java.lang.Exception){
                ex.printStackTrace()
            }

            val outBuffInfo = MediaCodec.BufferInfo()
            val tempBuffer = ByteArray(minBufferSize)
            var hasMoreData = true
            var presentationTimeUs = 0.0
            var audioTrackIdx = 0
            var totalBytesRead = 0

            do {
                var inputBufIndex = 0
                if (inputBufIndex != -1 && hasMoreData) {
                    inputBufIndex = encoder!!.dequeueInputBuffer(-1)
                    if (inputBufIndex >= 0) {
                        val dstBuf = encoder!!.getInputBuffer(inputBufIndex)
                        dstBuf!!.clear()
                        val bytesRead = fis.read(tempBuffer, 0, dstBuf.limit())

                        if (bytesRead == -1) { // -1 implies EOS
                            hasMoreData = false
                            encoder!!.queueInputBuffer(
                                inputBufIndex,
                                0,
                                0,
                                presentationTimeUs.toLong(),
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                        } else {
                            totalBytesRead += bytesRead
                            dstBuf.put(tempBuffer, 0, bytesRead)
                            presentationTimeUs =
                                (1000000.0 * totalBytesRead / 2.0) / (encodingSampleRate * channelsCount.toDouble())
                            encoder!!.queueInputBuffer(
                                inputBufIndex,
                                0,
                                bytesRead,
                                presentationTimeUs.toLong(),
                                0
                            )
                        }
                    }
                }

                var outputBufIndex = encoder!!.dequeueOutputBuffer(outBuffInfo, 100000)
                while (outputBufIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (outputBufIndex >= 0) {
                        val encodedData = encoder!!.getOutputBuffer(outputBufIndex)
                        encodedData!!.position(outBuffInfo.offset)

                        encodedData.limit(outBuffInfo.offset + outBuffInfo.size)
                        if (outBuffInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0 && outBuffInfo.size != 0) {
                            encoder!!.releaseOutputBuffer(outputBufIndex, false)
                        } else {
                            muxer!!.writeSampleData(
                                audioTrackIdx,
                                encodedData,
                                outBuffInfo
                            )
                            encoder!!.releaseOutputBuffer(outputBufIndex, false)
                        }
                    } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        format = encoder!!.outputFormat
                        //Log.v(LOG_TAG, "Output format changed - $format")
                        audioTrackIdx = muxer!!.addTrack(format)
                        muxer!!.start()
                    } else if (outputBufIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                        Log.e(
                            LOG_TAG,
                            "Unknown return code from dequeueOutputBuffer - $outputBufIndex")
                    }
                    outputBufIndex = encoder!!.dequeueOutputBuffer(outBuffInfo, 0)
                }
            } while (outBuffInfo.flags != MediaCodec.BUFFER_FLAG_END_OF_STREAM)

            fis.close()
            try {
                muxer!!.stop()
            }
            catch(ex: java.lang.Exception){
                ex.printStackTrace()
            }
            try{
                muxer!!.release()
            }
            catch(ex: java.lang.Exception){
                ex.printStackTrace()
            }
        }
        catch(ex: java.lang.Exception){
            ex.printStackTrace()
        }
        finally{
            onAudioSaved(audioFile!!.absolutePath)
        }
    }
}
