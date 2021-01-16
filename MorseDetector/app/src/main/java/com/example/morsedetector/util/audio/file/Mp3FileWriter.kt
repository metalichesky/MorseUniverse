package com.example.morsedetector.util.audio.file

import java.io.ByteArrayOutputStream


class Mp3FileWriter {

//    fun encodePcmToMp3(pcm: ByteArray): ByteArray? {
//
//        val encoder = LameEncoder(inputFormat, 256, MPEGMode.STEREO, Lame.QUALITY_HIGHEST, false)
//        val mp3 = ByteArrayOutputStream()
//        val buffer = ByteArray(encoder.getPCMBufferSize())
//        var bytesToTransfer = Math.min(buffer.size, pcm.size)
//        var bytesWritten: Int
//        var currentPcmPosition = 0
//        while (0 < encoder.encodeBuffer(pcm, currentPcmPosition, bytesToTransfer, buffer)
//                .also({ bytesWritten = it })
//        ) {
//            currentPcmPosition += bytesToTransfer
//            bytesToTransfer = Math.min(buffer.size, pcm.size - currentPcmPosition)
//            mp3.write(buffer, 0, bytesWritten)
//        }
//        encoder.close()
//        return mp3.toByteArray()
//    }

}