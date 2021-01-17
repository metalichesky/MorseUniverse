package com.example.traindatagenerator.util.audio.file

import com.example.traindatagenerator.model.AudioParams
import java.io.*
import java.util.*

class WavFileWriter {
    private var fileStream: BufferedOutputStream? = null
    private var tempFile: File? = null
    private var tempFileSize: Int = 0

    fun prepare() {
        tempFileSize = 0
        tempFile = createTempFile()
        fileStream = BufferedOutputStream(tempFile?.outputStream())
    }

    private fun getCacheDir(): File {
        return File("C:\\Users\\Dmitriy\\Desktop\\cache").apply {
            if (!exists()) {
                mkdirs()
            }
        }
//        return App.instance.externalCacheDir!!
    }

    private fun createTempFile(): File {
        val name = UUID.randomUUID().toString()
        return File(getCacheDir(), name)
    }

    fun write(byteArray: ByteArray) {
        if (tempFile == null) {
            prepare()
        }
        fileStream?.write(byteArray)
        tempFileSize += byteArray.size
    }

    fun complete(file: File, audioParams: AudioParams) {
        val tempFile = tempFile ?: return
        val sourceFileStream = BufferedInputStream(tempFile.inputStream())
        val destinationFileStream = BufferedOutputStream(file.outputStream())
        val byteArray = ByteArray(8 * 1024)
        var readed = sourceFileStream.read(byteArray)
        val fileSize = tempFile.length().toInt()
        println("complete() writed size ${tempFileSize} file size ${fileSize}")
        if (readed > 0) {
            destinationFileStream.write(
                WAVHeader(
                    audioParams,
                    fileSize
                ).header)
        }
        while(readed > 0) {
            destinationFileStream.write(byteArray, 0, readed)
            readed = sourceFileStream.read(byteArray)
        }
        destinationFileStream.flush()
        destinationFileStream.close()
    }

    fun release() {
        fileStream?.flush()
        fileStream?.close()
        tempFile?.delete()
        tempFile = null
        fileStream = null
        tempFileSize = 0
    }
}