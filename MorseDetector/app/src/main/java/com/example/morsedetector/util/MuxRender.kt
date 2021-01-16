package com.example.morsedetector.util

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import java.io.FileOutputStream
import java.lang.Exception
import java.lang.IllegalStateException
import java.nio.ByteBuffer
import java.nio.ByteOrder


class MuxRender(
) {
    private val BUFFER_SIZE = 64 * 1024

    private var videoFormat: MediaFormat? = null
    private var audioFormat: MediaFormat? = null
    private var videoTrackIndex = 0
    private var audioTrackIndex = 0
    private var byteBuffer: ByteBuffer? = null
    private var sampleInfoList: MutableList<SampleInfo>? = mutableListOf()
    private var started = false
    private var skipNewFormatSetup = false

    private var muxer: MediaMuxer? = null
    private var outputFilePath: String? = null

    fun setOutputFilePath(filePath: String) {
        outputFilePath = filePath
    }

    fun setup() {
        val outputFilePath = outputFilePath ?: throw IllegalStateException("set valid output file path before call setup()")

        muxer = try {
            if (Build.VERSION.SDK_INT >= 26) {
                val outputFileStream = FileOutputStream(outputFilePath)
                MediaMuxer(
                    outputFileStream.fd,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                )
            } else {
                MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            }
        } catch (ex: Exception) {
            throw IllegalStateException("can't create media muxer for provided output file path")
        }
    }

    fun release() {
        try {
            muxer?.stop()
        } catch(ex: Exception) {
            ex.printStackTrace()
        }
        try {
            muxer?.release()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun setOutputFormat(sampleType: SampleType?, format: MediaFormat?) {
        when (sampleType) {
            SampleType.VIDEO -> videoFormat = format
            SampleType.AUDIO -> audioFormat = format
            else -> throw AssertionError()
        }
    }

    fun onSetOutputFormat() {
        val muxer = muxer ?: throw IllegalStateException("media muxer not initialized")

        if (skipNewFormatSetup) {
            skipNewFormatSetup = false
            return
        }
        if (videoFormat != null && audioFormat != null) {
            videoTrackIndex = muxer.addTrack(videoFormat!!)
            audioTrackIndex = muxer.addTrack(audioFormat!!)
        } else if (videoFormat != null) {
            videoTrackIndex = muxer.addTrack(videoFormat!!)
        } else if (audioFormat != null) {
            audioTrackIndex = muxer.addTrack(audioFormat!!)
        }
        muxer.start()
        started = true
        if (byteBuffer == null) {
            byteBuffer = ByteBuffer.allocate(0)
        }
        byteBuffer!!.flip()
        val bufferInfo = MediaCodec.BufferInfo()
        var offset = 0
        for (sampleInfo in sampleInfoList!!) {
            sampleInfo.writeToBufferInfo(bufferInfo, offset)
            muxer.writeSampleData(
                getTrackIndexForSampleType(sampleInfo.sampleType),
                byteBuffer!!,
                bufferInfo
            )
            offset += sampleInfo.size
        }
        sampleInfoList!!.clear()
        byteBuffer = null
    }

    fun writeSampleData(
        sampleType: SampleType,
        byteBuf: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo
    ) {
        val muxer = muxer ?: throw IllegalStateException("media muxer not initialized")
        if (started) {
            muxer.writeSampleData(getTrackIndexForSampleType(sampleType), byteBuf, bufferInfo)
            return
        }
        if (skipNewFormatSetup) return
        byteBuf.limit(bufferInfo.offset + bufferInfo.size)
        byteBuf.position(bufferInfo.offset)
        if (byteBuffer == null) {
            byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE)
                .order(ByteOrder.nativeOrder())
        }
        byteBuffer!!.put(byteBuf)
        sampleInfoList!!.add(SampleInfo(sampleType, bufferInfo.size, bufferInfo))
    }

    private fun getTrackIndexForSampleType(sampleType: SampleType): Int {
        return when (sampleType) {
            SampleType.VIDEO -> videoTrackIndex
            SampleType.AUDIO -> audioTrackIndex
        }
    }

    fun skipNewFormatSetup() {
        skipNewFormatSetup = true
    }

    enum class SampleType {
        VIDEO, AUDIO
    }

    class SampleInfo(
        val sampleType: SampleType,
        val size: Int,
        bufferInfo: MediaCodec.BufferInfo
    ) {
        private val presentationTimeUs: Long = bufferInfo.presentationTimeUs
        private val flags: Int = bufferInfo.flags

        fun writeToBufferInfo(
            bufferInfo: MediaCodec.BufferInfo,
            offset: Int
        ) {
            bufferInfo[offset, size, presentationTimeUs] = flags
        }
    }
}
