package com.example.traindatagenerator.util.audio.file

import com.example.traindatagenerator.model.AudioParams

class WAVHeader(
    private val audioParams: AudioParams,
    private val audioDataBytesCount: Int = 0
) {
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
    }
    var header: ByteArray? = null // the complete header
        private set

    private val bytesPerSample: Int = audioParams.bytesPerFrame
    private val bitsPerSample: Int = audioParams.encoding.bytesPerSample * 8
    private val channelsCount: Int = audioParams.channelsCount
    private val sampleRate: Int = audioParams.sampleRate

    init {
        setHeader()
    }

    private fun setHeader() {
        val header = ByteArray(HEADER_SIZE)
        var offset = 0
        var size: Int

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