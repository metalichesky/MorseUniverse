package com.example.morsedetector.util

import java.lang.Integer.min

object AudioMixer {
    fun mix(channel1: FloatArray, channel2: FloatArray): FloatArray {
        val resultArraySize = Math.min(channel1.size, channel2.size)
        val resultArray = FloatArray(resultArraySize)
        mix(channel1, channel2, resultArray)
        return resultArray
    }

    fun mix(channel1: FloatArray, channel2: FloatArray, resultArray: FloatArray) {
        var resultIdx = 0
        while(resultIdx < channel1.size && resultIdx < channel2.size && resultIdx < resultArray.size) {
            resultArray[resultIdx] = (channel1[resultIdx] + channel2[resultIdx]) * 0.5f
            resultIdx++
        }
    }
}