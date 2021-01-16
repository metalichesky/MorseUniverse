package com.example.morsedetector.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.FileInputStream
import java.io.IOException
import java.lang.Exception

class AudioDecoder {
    companion object {
        private const val DRAIN_STATE_NONE = 0
        private const val DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY = 1
        private const val DRAIN_STATE_CONSUMED = 2
        private const val AUDIO_FORMAT_PREFIX = "audio/"
        private const val DECODER_TIMEOUT = 5L
    }

    private var inputFilePath: String? = null
    private var extractor: MediaExtractor? = null
    private var decoder: MediaCodec? = null
    private var decoderBufferInfo = MediaCodec.BufferInfo()

    private var audioTrackIdx: Int = -1
    private var inputFormat: MediaFormat? = null
    var outputFormat: MediaFormat? = null

    private var isExtractorEOS: Boolean = false
    private var isDecoderEOS: Boolean = false


    private var decodedDataArray: ByteArray? = null
    private var decodedDataSize: Int = 0
    private var decodedDataOffset: Int = 0

    fun setInputFilePath(path: String) {
        inputFilePath = path
    }

    fun setup() {
        if (inputFilePath == null) {
            throw IllegalStateException("set input file path before call setup()")
        }
        extractor = try {
            MediaExtractor().apply {
                val videoFis = FileInputStream(inputFilePath)
                setDataSource(videoFis.fd)
                videoFis.close()
            }
        } catch (ex: Exception) {
            throw IllegalStateException("can't initialize media extractor for provided input file", ex)
        }

        audioTrackIdx = extractor?.findFirstTrack(AUDIO_FORMAT_PREFIX) ?: -1
        if (audioTrackIdx < 0) {
            throw IllegalStateException("input file doesn't contain any audio track")
        }
        inputFormat = extractor?.getTrackFormat(audioTrackIdx)
        val inputFormatMime = inputFormat?.getString(MediaFormat.KEY_MIME) ?:
            throw IllegalStateException("input file doesn't contain any audio track with valid format")

        extractor?.selectTrack(audioTrackIdx)

        println("input format ${inputFormat}")

        decoder = try {
            MediaCodec.createDecoderByType(inputFormatMime)
        } catch (ex: IOException) {
            throw IllegalStateException("can't create codec for provided input format ${inputFormatMime}", ex)
        }
        decoder?.configure(inputFormat, null, null, 0)
        decoder?.start()
        isDecoderEOS = false
        isExtractorEOS = false
        outputFormat = decoder?.outputFormat
    }

    fun hasNext(): Boolean {
        return !isDecoderEOS || decodedDataArray != null
    }

    fun decodeNextInto(resultArray: ByteArray) {
        if (decoder == null || extractor == null) {
            throw IllegalStateException("you should call setup() before decoding")
        }

        var resultDataOffset = 0
        var neededDataSize = resultArray.size

        if (decodedDataSize > 0 && decodedDataArray != null) {
            val filledSize = fillResultArray(resultArray, neededDataSize, resultDataOffset)
            resultDataOffset += filledSize
            neededDataSize -= filledSize
        }

        while(neededDataSize > 0 && hasNext()) {
            if (decodedDataArray == null) {
                stepDecoder()
            }
            val filledSize = fillResultArray(resultArray, neededDataSize, resultDataOffset)
            resultDataOffset += filledSize
            neededDataSize -= filledSize
        }
    }

    private fun stepDecoder(): Boolean {
        var busy = false
        if (drainExtractor(DECODER_TIMEOUT) != DRAIN_STATE_NONE) {
            busy = true
        }
        if (drainDecoder(DECODER_TIMEOUT) != DRAIN_STATE_NONE) {
            busy = true
        }
        return busy
    }

    private fun fillResultArray(resultArray: ByteArray, neededDataSize: Int, resultDataOffset: Int): Int {
        val decodedDataArray = decodedDataArray ?: return 0
        val copySize = Math.min(decodedDataSize, neededDataSize)
        System.arraycopy(decodedDataArray, decodedDataOffset, resultArray, resultDataOffset, copySize)
        decodedDataOffset += copySize
        decodedDataSize -= copySize
        if (decodedDataSize <= 0) {
            releaseData()
        }
        return copySize
    }

    private fun releaseData() {
        decodedDataSize = 0
        decodedDataOffset = 0
        this.decodedDataArray = null
    }

    fun stepPipeline(): Boolean {
        var busy = false
        var status: Int
        do {
            status = drainDecoder(DECODER_TIMEOUT)
            if (status != DRAIN_STATE_NONE) busy = true
        } while (status == DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY)
        while (drainExtractor(DECODER_TIMEOUT) != DRAIN_STATE_NONE) {
            busy = true
        }
        return busy
    }

    private fun drainExtractor(timeoutUs: Long): Int {
        val extractor = extractor ?: return DRAIN_STATE_NONE
        val decoder = decoder ?: return DRAIN_STATE_NONE

        if (isExtractorEOS) return DRAIN_STATE_NONE

        val trackIndex = extractor.sampleTrackIndex
        if (trackIndex >= 0 && trackIndex != audioTrackIdx) {
            return DRAIN_STATE_NONE
        }

        val result = decoder.dequeueInputBuffer(timeoutUs)
        if (result < 0) return DRAIN_STATE_NONE
        if (trackIndex < 0) {
            isExtractorEOS = true
            decoder.queueInputBuffer(result, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            return DRAIN_STATE_NONE
        }

        val inputBuffer = decoder.getInputBuffer(result) ?: return DRAIN_STATE_NONE
        val sampleSize = extractor.readSampleData(inputBuffer, 0)
        val isKeyFrame = extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0
        decoder.queueInputBuffer(
            result,
            0,
            sampleSize,
            extractor.sampleTime,
            if (isKeyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
        )
        extractor.advance()
        return DRAIN_STATE_CONSUMED
    }

    private fun drainDecoder(timeoutUs: Long): Int {
        val decoder = decoder ?: return DRAIN_STATE_NONE

        if (isDecoderEOS) return DRAIN_STATE_NONE

        val result = decoder.dequeueOutputBuffer(decoderBufferInfo, timeoutUs)
        when (result) {
            MediaCodec.INFO_TRY_AGAIN_LATER -> {
                return DRAIN_STATE_NONE
            }
            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                outputFormat = decoder.outputFormat
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
            }
            MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                return DRAIN_STATE_SHOULD_RETRY_IMMEDIATELY
            }
        }
        if (decoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
            isDecoderEOS = true
        } else if (decoderBufferInfo.size > 0) {
            val outputBuffer = decoder.getOutputBuffer(result) ?: return DRAIN_STATE_NONE
            val outputBufferSize = outputBuffer.remaining()
            if (decodedDataArray == null) {
                decodedDataArray = ByteArray(outputBufferSize)
            }
            if (decodedDataArray!!.size < outputBufferSize) {
                decodedDataArray = ByteArray(outputBufferSize)
            }
            outputBuffer.get(decodedDataArray!!)
            decodedDataSize = outputBufferSize
            decoder.releaseOutputBuffer(result, false)
        }
        return DRAIN_STATE_CONSUMED
    }

    fun release() {
        releaseData()
        try {
            extractor?.release()
        } catch(ex: Exception) {
            ex.printStackTrace()
        }
        try {
            decoder?.flush()
        } catch(ex: Exception) {
            ex.printStackTrace()
        }
        try {
            decoder?.stop()
        } catch(ex: Exception) {
            ex.printStackTrace()
        }
        try {
            decoder?.release()
        } catch(ex: Exception) {
            ex.printStackTrace()
        }
    }
}

fun MediaExtractor.findFirstTrack(trackType: String): Int {
    var trackIdx = -1
    for (i in 0 until trackCount) {
        val mediaFormat = getTrackFormat(i)
        val mimeType = mediaFormat.getString(MediaFormat.KEY_MIME) ?: continue
        if (mimeType.startsWith(trackType)) {
            trackIdx = i
        }
    }
    return trackIdx
}