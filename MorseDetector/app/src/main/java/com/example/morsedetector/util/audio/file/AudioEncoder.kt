package com.example.morsedetector.util.audio.file

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import com.example.morsedetector.model.AudioParams
import java.io.IOException
import java.lang.Exception
import java.nio.ByteBuffer


class AudioEncoder {
    companion object {
        private const val DRAIN_STATE_NONE = 0
        private const val DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY = 1
        private const val DRAIN_STATE_CONSUMED = 2
        private const val AUDIO_FORMAT_PREFIX = "audio/"
        private const val ENCODER_TIMEOUT = 5L
        private val SAMPLE_TYPE: MuxRender.SampleType =
            MuxRender.SampleType.AUDIO

        fun createOutputFormatAAC(inputFormat: MediaFormat): MediaFormat {
            return if (MediaFormat.MIMETYPE_AUDIO_AAC == inputFormat.getString(MediaFormat.KEY_MIME)) {
                inputFormat
            } else {
                val outputFormat = MediaFormat()
                outputFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC)
                outputFormat.setInteger(
                    MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectELD
                )
                outputFormat.setInteger(
                    MediaFormat.KEY_SAMPLE_RATE,
                    inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                )
                outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000)
                outputFormat.setInteger(
                    MediaFormat.KEY_CHANNEL_COUNT,
                    inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                )
                outputFormat
            }
        }
    }

    private var encoder: MediaCodec? = null
    private var encoderBufferInfo = MediaCodec.BufferInfo()
    private var muxRender: MuxRender? = null

    private var audioParams: AudioParams? = null
    private var inputFormat: MediaFormat? = null
    private var outputFormat: MediaFormat? = null
    private var actualOutputFormat: MediaFormat? = null

    private var writeEOS: Boolean = false
    private var isEncoderEOS: Boolean = false

    private var dataToEncode: ByteArray? = null
    private var dataToEncodeTimestamp: Long = 0L
    private var dataToEncodeSize: Int = 0
    private var dataToEncodeOffset: Int = 0

    fun setInputFormat(format: MediaFormat) {
        inputFormat = format
    }

    fun setOutputFormat(format: MediaFormat) {
        outputFormat = format
    }

    fun setOutputFilePath(filePath: String) {
        muxRender = MuxRender()
        muxRender?.setOutputFilePath(filePath)
        muxRender?.setup()

    }

    fun setup() {
        if (muxRender == null) {
            throw IllegalStateException("set mux render before call setup()")
        }
        val inputFormat = inputFormat ?: throw IllegalStateException("set input format before call setup()")
        val inputFormatMime = inputFormat.getString(MediaFormat.KEY_MIME) ?: throw IllegalStateException("provided input format is invalid")
        if (!inputFormatMime.startsWith(AUDIO_FORMAT_PREFIX)) {
            throw IllegalStateException("provided output format is not audio format")
        }
        audioParams = inputFormat.getAudioParams()

        val outputFormat = outputFormat ?: throw IllegalStateException("set output format before call setup()")
        val outputFormatMime = outputFormat.getString(MediaFormat.KEY_MIME) ?: throw IllegalStateException("provided output format is invalid")
        if (!outputFormatMime.startsWith(AUDIO_FORMAT_PREFIX)) {
            throw IllegalStateException("provided output format is not audio format")
        }

        encoder = try {
            MediaCodec.createEncoderByType(outputFormatMime)
        } catch (ex: IOException) {
            throw IllegalStateException("can't create codec for provided output format ${outputFormatMime}", ex)
        }
        encoder?.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder?.start()

        println("input format ${inputFormatMime}")
        println("output format ${outputFormatMime}")

        isEncoderEOS = false
    }

    fun canEncode(): Boolean {
        return (!isEncoderEOS && encoder != null && muxRender != null)
    }

    fun encode(inputArray: ByteArray, timestamp: Long, writeEos: Boolean = false) {
        if (encoder == null || muxRender == null) {
            throw IllegalStateException("you should call setup() before encoding")
        }

        dataToEncodeSize = inputArray.size
        dataToEncode = inputArray
        dataToEncodeOffset = 0
        dataToEncodeTimestamp = timestamp
        writeEOS = writeEos

        while(dataToEncode != null && canEncode()) {
            stepEncoder()
        }
    }

    private fun stepEncoder(): Boolean {
        var busy = false
        if (feedEncoder(ENCODER_TIMEOUT) != DRAIN_STATE_NONE) {
            busy = true
        }
        if (drainEncoder(ENCODER_TIMEOUT) != DRAIN_STATE_NONE) {
            busy = true
        }
        return busy
    }

    private fun releaseData() {
        dataToEncodeOffset = 0
        dataToEncodeSize = 0
        this.dataToEncode = null
        dataToEncodeTimestamp = 0L
    }

    private fun feedEncoder(timeoutUs: Long): Int {
        val encoder = encoder ?: return DRAIN_STATE_NONE
        val audioParams = audioParams ?: return DRAIN_STATE_NONE
        val dataToEncode = dataToEncode ?: return DRAIN_STATE_NONE

        val result = encoder.dequeueInputBuffer(timeoutUs)
        return if (result >= 0) {
            val inputBuffer = encoder.getInputBuffer(result) ?: return DRAIN_STATE_NONE
            val presentationTimeUs = calcTimestamp()
            val fillSize = fillInputBuffer(inputBuffer)
            val flags = if (writeEOS) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
            encoder.queueInputBuffer(result, 0, fillSize, presentationTimeUs, flags)


            DRAIN_STATE_CONSUMED
        } else {
            when (result) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
                else -> DRAIN_STATE_NONE
            }
        }
    }

    private fun calcTimestamp(): Long {
        val audioParams = audioParams ?: return 0L
        val timestamp = dataToEncodeTimestamp + (dataToEncodeOffset / audioParams.bytesPerMs) * 1000L
        return timestamp
    }

    private fun fillInputBuffer(byteBuffer: ByteBuffer): Int {
        val dataToEncode = dataToEncode ?: return 0
        val availableCapacity = byteBuffer.capacity()
        val fillSize = Math.min(dataToEncodeSize, availableCapacity)
        byteBuffer.clear()
        byteBuffer.put(dataToEncode, dataToEncodeOffset, fillSize)
        this.dataToEncodeSize -= fillSize
        this.dataToEncodeOffset += fillSize

        if (dataToEncodeSize <= 0) {
            releaseData()
        }
        return fillSize
    }

    private fun drainEncoder(timeoutUs: Long): Int {
        if (isEncoderEOS) return DRAIN_STATE_NONE
        val encoder = encoder ?: return DRAIN_STATE_NONE
        val muxRender = muxRender ?: return DRAIN_STATE_NONE

        val result = encoder.dequeueOutputBuffer(encoderBufferInfo, timeoutUs)
        when (result) {
            MediaCodec.INFO_TRY_AGAIN_LATER -> return DRAIN_STATE_NONE
            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                if (actualOutputFormat != null) {
                    throw RuntimeException("Audio output format changed twice.")
                }
                actualOutputFormat = encoder.outputFormat
                muxRender.setOutputFormat(SAMPLE_TYPE, actualOutputFormat)
                muxRender.onSetOutputFormat()
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
            }
            MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
        }
        if (actualOutputFormat == null) {
            throw RuntimeException("Could not determine actual output format.")
        }
        if (encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
            isEncoderEOS = true
            encoderBufferInfo[0, 0, 0] = encoderBufferInfo.flags
        }
        if (encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
            encoder.releaseOutputBuffer(result, false)
            return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
        }
        encoderBufferInfo.presentationTimeUs = encoderBufferInfo.presentationTimeUs
        val encodedData = encoder.getOutputBuffer(result) ?: return DRAIN_STATE_NONE
        muxRender.writeSampleData(SAMPLE_TYPE, encodedData, encoderBufferInfo)
        encoder.releaseOutputBuffer(result, false)
        return DRAIN_STATE_CONSUMED
    }

    fun release() {
        releaseData()
        try {
            encoder?.flush()
        } catch(ex: Exception) {
            ex.printStackTrace()
        }
        try {
            encoder?.stop()
        } catch(ex: Exception) {
            ex.printStackTrace()
        }
        try {
            encoder?.release()
        } catch(ex: Exception) {
            ex.printStackTrace()
        }
        try {
            muxRender?.release()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}
