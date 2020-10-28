package com.example.morsedetector.util

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.roundToInt


class SimpleResampler{
    var channelsCount = 1
    var srcSampleRate = 48000
    var dstSampleRate = 48000
    var bitrate = 16
    private var sampleRatio = 1
    private var resultSize = 0

    fun prepare(){
        var ratio = dstSampleRate.toDouble() / srcSampleRate.toDouble()
        if (ratio < 1f && ratio > 0f){
            ratio = srcSampleRate.toDouble() / dstSampleRate.toDouble()
            ratio *= -1
        }
        sampleRatio = ratio.roundToInt()
    }

    fun resample(inputSamples: ByteArray): ByteArray{
        return when {
            sampleRatio == 1 -> inputSamples
            bitrate == 16 -> resampleShortArray(inputSamples)
            else -> resampleByteArray(inputSamples)
        }
    }

    private fun resampleShortArray(inputSamples: ByteArray): ByteArray{
        resultSize = if (sampleRatio > 0){
            inputSamples.size * sampleRatio
        }
        else{
            inputSamples.size / abs(sampleRatio)
        }
        resultSize /= 2
        val preparedArray = byteArrayToShortArray(inputSamples)

        val resultSamples = interpolate(preparedArray)

        return shortArrayToByteArray(resultSamples)
    }

    fun interpolate(inputArray: ShortArray): ShortArray{
        //Log.d("Interpolate","SampleRatio ${sampleRatio} input size ${inputArray.size} result size ${resultSize}")
        val resultArray = ShortArray(resultSize)
        //slow down (upsampling)
        if (sampleRatio > 0) {
            for (i in 0 until inputArray.size / channelsCount) {
                for (channel in 0 until channelsCount) {
                    val currentIndex = i*channelsCount + channel
                    val nextIndex = (i+1)*channelsCount + channel
                    val Y = inputArray[currentIndex]
                    val dY = if (nextIndex < inputArray.size - 1) {
                        inputArray[nextIndex] - inputArray[currentIndex]
                    } else {
                        0
                    }
                    for (j in 0 until sampleRatio){
                        val position = (i*channelsCount*sampleRatio + channel + j*channelsCount)
                        if (position > resultArray.size - 1) break
                        resultArray[position] =
                            (Y + (dY.toDouble() / sampleRatio.toDouble()) * j).toShort()
                    }
                }
            }
        }
        //speed up (downsampling)
        else{
            val absRatio = abs(sampleRatio)
            for (i in 0 until resultArray.size / channelsCount) {
                for (channel in 0 until channelsCount) {
                    val inputIndex = i * absRatio * channelsCount + channel
                    val resultIndex = i * channelsCount + channel
                    if (inputIndex > inputArray.size - 1) {
                        resultArray[resultIndex] = resultArray[resultIndex - channelsCount]
                    } else {
                        resultArray[resultIndex] = inputArray[inputIndex]
                    }
                }
            }
        }
        return resultArray
    }

    fun resampleByteArray(inputSamples: ByteArray): ByteArray{
        resultSize = if (sampleRatio > 0){
            inputSamples.size * sampleRatio
        }
        else{
            inputSamples.size / abs(sampleRatio)
        }
        return interpolate(inputSamples)
    }

    fun interpolate(inputArray: ByteArray): ByteArray{
        //Log.d("Interpolate","SampleRatio ${sampleRatio} input size ${inputArray.size} result size ${resultSize}")
        val resultArray = ByteArray(resultSize)
        //slow down (upsampling)
        if (sampleRatio > 0) {
            for (i in 0 until inputArray.size / channelsCount) {
                for (channel in 0 until channelsCount) {
                    val currentIndex = i*channelsCount+channel
                    val nextIndex = (i+1)*channelsCount+channel
                    val Y = inputArray[currentIndex]
                    val dY = if (nextIndex < inputArray.size - 1) {
                        inputArray[nextIndex] - inputArray[currentIndex]
                    } else {
                        0
                    }
                    for (j in 0 until sampleRatio){
                        val position = currentIndex * sampleRatio + j*channelsCount
                        if (position > resultArray.size - 1) break
                        resultArray[currentIndex * sampleRatio + j*channelsCount] =
                            (Y + (dY.toDouble() / sampleRatio.toDouble()) * j).toByte()
                    }
                }
            }
        }
        //speed up (downsampling)
        else{
            val absRatio = abs(sampleRatio)
            for (i in 0 until resultArray.size / channelsCount) {
                for (channel in 0 until channelsCount) {
                    val inputIndex = i * absRatio * channelsCount + channel
                    val resultIndex = i * channelsCount + channel
                    if (inputIndex > inputArray.size - 1) {
                        resultArray[resultIndex] = resultArray[resultIndex - channelsCount]
                    } else {
                        resultArray[resultIndex] = inputArray[inputIndex]
                    }
                }
            }
        }
        return resultArray
    }

    fun byteArrayToShortArray(byteArray: ByteArray):ShortArray{
        val shortArray = ShortArray(byteArray.size/2)
        val byteBuffer = ByteBuffer.allocate(2)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        for(i in shortArray.indices){
            byteBuffer.put(byteArray[i*2])
            byteBuffer.put(byteArray[i*2+1])
            shortArray[i] = byteBuffer.getShort(0)
            byteBuffer.clear()
        }
        return shortArray
    }

    fun shortArrayToByteArray(shortArray: ShortArray): ByteArray{
        val byteArray = ByteArray(shortArray.size*2)
        val byteBuffer = ByteBuffer.allocate(2)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        for(i in shortArray.indices){
            byteBuffer.putShort(shortArray[i])
            byteArray[i*2] = byteBuffer.get(0)
            byteArray[i*2+1] = byteBuffer.get(1)
            byteBuffer.clear()
        }
        return byteArray
    }
}