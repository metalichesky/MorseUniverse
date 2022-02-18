package com.example.traindatagenerator.util.audio.file

import java.io.BufferedInputStream
import java.io.File


class WavFileReader {
    private var fileStream: BufferedInputStream? = null
    private var inputFile: File? = null
    private var inputFileSize: Int = 0
    var wavHeader: WAVHeader? = null
        private set

    fun hasMore(): Boolean {
        return (fileStream?.available() ?: 0) > 0
    }

    fun prepare(inputFile: File?) {
        this.inputFile = inputFile
        inputFile?.let {
            fileStream = BufferedInputStream(it.inputStream())
            inputFileSize = it.length().toInt()
        }
        val inputFileStream = fileStream ?: return
        val headerBytes = ByteArray(WAVHeader.HEADER_SIZE)
        inputFileStream.read(headerBytes)
        wavHeader = WAVHeader.getHeaderFromBytes(headerBytes)
    }

    fun read(byteArray: ByteArray): Int {
        val inputFileStream = fileStream ?: return 0
        val size = Math.min(byteArray.size, inputFileStream.available())
        val readed = inputFileStream.read(byteArray, 0, size)
        inputFileSize -= readed
        return readed
    }

    fun release() {
        fileStream?.close()
        fileStream = null
        inputFile = null
        inputFileSize = 0
        wavHeader = null
    }
}