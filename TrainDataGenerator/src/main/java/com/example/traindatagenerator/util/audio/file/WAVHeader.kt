package com.example.traindatagenerator.util.audio.file

import com.example.traindatagenerator.model.AudioParams
import com.example.traindatagenerator.model.Encoding

class WAVHeader {
    companion object {
        const val HEADER_SIZE = 46

        fun getHeaderForSamples(audioParams: AudioParams, numSamples: Int): ByteArray? {
            val bytesCount = audioParams.bytesPerFrame * numSamples
            return WAVHeader(
                    audioParams,
                    bytesCount
            ).header
        }

        fun getHeaderForBytes(audioParams: AudioParams, bytesCount: Int): ByteArray? {
            return WAVHeader(
                    audioParams,
                    bytesCount
            ).header
        }

        fun getHeaderFromBytes(bytes: ByteArray): WAVHeader {
            return WAVHeader(bytes)
        }
    }

    var header: ByteArray = ByteArray(HEADER_SIZE) // the complete header
        private set
    var audioParams: AudioParams
        private set
    var audioDataBytesCount: Int
        private set

    constructor(audioParams: AudioParams, audioDataBytesCount: Int = 0) {
        this.audioParams = audioParams
        this.audioDataBytesCount = audioDataBytesCount
        writeHeader()
    }

    constructor(headerRawBytes: ByteArray) {
        // initial values
        audioParams = AudioParams.createDefault()
        audioDataBytesCount = 0

        readHeader(headerRawBytes)
    }

    private fun writeHeader() {
        val header = ByteArray(HEADER_SIZE)
        var offset = 0
        var size: Int


        val bytesPerSample: Int = audioParams.bytesPerFrame
        val bitsPerSample: Int = audioParams.encoding.bytesPerSample * 8
        val channelsCount: Int = audioParams.channelsCount
        val sampleRate: Int = audioParams.sampleRate

        // set the RIFF chunk
        System.arraycopy(
                byteArrayOf(
                        'R'.toByte(),
                        'I'.toByte(),
                        'F'.toByte(),
                        'F'.toByte()
                ), 0, header, offset, 4
        )
        offset += 4
        size = 36 + audioDataBytesCount
        header[offset++] = (size and 0xFF).toByte()
        header[offset++] = (size shr 8 and 0xFF).toByte()
        header[offset++] = (size shr 16 and 0xFF).toByte()
        header[offset++] = (size shr 24 and 0xFF).toByte()
        System.arraycopy(
                byteArrayOf(
                        'W'.toByte(),
                        'A'.toByte(),
                        'V'.toByte(),
                        'E'.toByte()
                ), 0, header, offset, 4
        )
        offset += 4

        // set the fmt chunk
        System.arraycopy(
                byteArrayOf(
                        'f'.toByte(),
                        'm'.toByte(),
                        't'.toByte(),
                        ' '.toByte()
                ), 0, header, offset, 4
        )
        offset += 4
        System.arraycopy(
                byteArrayOf(0x10, 0, 0, 0),
                0,
                header,
                offset,
                4
        ) // chunk size = 16
        offset += 4
        System.arraycopy(byteArrayOf(1, 0), 0, header, offset, 2) // format = 1 for PCM
        offset += 2
        header[offset++] = (channelsCount and 0xFF).toByte()
        header[offset++] = (channelsCount shr 8 and 0xFF).toByte()
        header[offset++] = (sampleRate and 0xFF).toByte()
        header[offset++] = (sampleRate shr 8 and 0xFF).toByte()
        header[offset++] = (sampleRate shr 16 and 0xFF).toByte()
        header[offset++] = (sampleRate shr 24 and 0xFF).toByte()
        val byteRate = sampleRate * bytesPerSample
        header[offset++] = (byteRate and 0xFF).toByte()
        header[offset++] = (byteRate shr 8 and 0xFF).toByte()
        header[offset++] = (byteRate shr 16 and 0xFF).toByte()
        header[offset++] = (byteRate shr 24 and 0xFF).toByte()
        header[offset++] = (bytesPerSample and 0xFF).toByte()
        header[offset++] = (bytesPerSample shr 8 and 0xFF).toByte()

        header[offset++] = (bitsPerSample and 0xFF).toByte()
        header[offset++] = (bitsPerSample shr 8 and 0xFF).toByte()
//        System.arraycopy(byteArrayOf(0x10, 0), 0, header, offset, 2)
//        offset += 2

        // set the beginning of the data chunk
        System.arraycopy(
                byteArrayOf(
                        'd'.toByte(),
                        'a'.toByte(),
                        't'.toByte(),
                        'a'.toByte()
                ), 0, header, offset, 4
        )
        offset += 4
        size = audioDataBytesCount
        header[offset++] = (size and 0xFF).toByte()
        header[offset++] = (size shr 8 and 0xFF).toByte()
        header[offset++] = (size shr 16 and 0xFF).toByte()
        header[offset++] = (size shr 24 and 0xFF).toByte()
        this.header = header
    }

    private fun readHeader(header: ByteArray) {
        var offset = 0
        offset += 4
        var size: Int = 0
        size = size or (header[offset++].toUByte().toInt())
        size = size or (header[offset++].toUByte().toInt() shl 8)
        size = size or (header[offset++].toUByte().toInt() shl 16)
        size = size or (header[offset++].toUByte().toInt() shl 24)
        audioDataBytesCount = size - 36
        offset += 4
        offset += 4
        offset += 4
        offset += 2

        var channelsCount: Int = 0
        channelsCount = channelsCount or (header[offset++].toUByte().toInt())
        channelsCount = channelsCount or (header[offset++].toUByte().toInt() shl 8)
        // audioParams.channelsCount = channelsCount

        var sampleRate: Int = 0
        sampleRate = sampleRate or (header[offset++].toUByte().toInt())
        sampleRate = sampleRate or (header[offset++].toUByte().toInt() shl 8)
        sampleRate = sampleRate or (header[offset++].toUByte().toInt() shl 16)
        sampleRate = sampleRate or (header[offset++].toUByte().toInt() shl 24)
        // audioParams.sampleRate = sampleRate

        var byteRate = 0//sampleRate * bytesPerSample (bytes per second)
        byteRate = byteRate or (header[offset++].toUByte().toInt())
        byteRate = byteRate or (header[offset++].toUByte().toInt() shl 8)
        byteRate = byteRate or (header[offset++].toUByte().toInt() shl 16)
        byteRate = byteRate or (header[offset++].toUByte().toInt() shl 24)


        var bytesPerFrame = 0
        bytesPerFrame = bytesPerFrame or (header[offset++].toUByte().toInt())
        bytesPerFrame = bytesPerFrame or (header[offset++].toUByte().toInt() shl 8)
        // audioParams.bytesPerFrame = bytesPerFrame

        var bitsPerSample = 0
        bitsPerSample = bitsPerSample or (header[offset++].toUByte().toInt())
        bitsPerSample = bitsPerSample or (header[offset++].toUByte().toInt() shl 8)
        val bytesPerSample = bitsPerSample / 8
        // audioParams.encoding.bytesPerSample = bitsPerSample / 8

        // set the beginning of the data chunk
        offset += 4
        var audioDataSize = 0
        audioDataSize = audioDataSize or (header[offset++].toUByte().toInt())
        audioDataSize = audioDataSize or (header[offset++].toUByte().toInt() shl 8)
        audioDataSize = audioDataSize or (header[offset++].toUByte().toInt() shl 16)
        audioDataSize = audioDataSize or (header[offset++].toUByte().toInt() shl 24)
        audioDataBytesCount = audioDataSize
        this.header = header
        audioParams = AudioParams(
                sampleRate = sampleRate,
                channelsCount = channelsCount,
                encoding = when(bytesPerSample) {
                    1 -> {
                        Encoding.PCM_8BIT
                    }
                    2 -> {
                        Encoding.PCM_16BIT
                    }
                    3 -> {
                        Encoding.PCM_24BIT
                    }
                    else -> {
                        Encoding.PCM_FLOAT
                    }
                }
        )
    }

    override fun toString(): String {
        var str = ""
        val header = header ?: return str
        val num_32bits_per_lines = 8
        var count = 0
        for (b in header) {
            val breakLine = count > 0 && count % (num_32bits_per_lines * 4) == 0
            val insertSpace = count > 0 && count % 4 == 0 && !breakLine
            if (breakLine) {
                str += '\n'
            }
            if (insertSpace) {
                str += ' '
            }
            str += String.format("%02X", b)
            count++
        }
        return str
    }
}