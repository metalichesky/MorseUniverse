package com.example.morsedetector.util.audio.transformer

import com.example.morsedetector.model.AudioParams

data class ChannelParams (
    val audioParams: AudioParams,
    val volumeRatio: Float
)

object AudioMixer {
    fun mix(channel1: FloatArray, channel2: FloatArray): FloatArray {
        val resultArraySize = Math.min(channel1.size, channel2.size)
        val resultArray = FloatArray(resultArraySize)
        mix(
            channel1,
            channel2,
            resultArray
        )
        return resultArray
    }

    fun mix(channel1: FloatArray, channel2: FloatArray, resultArray: FloatArray) {
        var resultIdx = 0
        var result = 0f
        val endIdx = Math.min(Math.min(channel1.size, channel2.size), resultArray.size)
        while(resultIdx < endIdx) {
            result = (channel1[resultIdx] + channel2[resultIdx]) * 0.5f
            resultArray[resultIdx] = result
            resultIdx++
        }
    }

    fun mix(channel1: ShortArray, channel2: ShortArray): ShortArray {
        val resultArraySize = Math.min(channel1.size, channel2.size)
        val resultArray = ShortArray(resultArraySize)
        mix(
            channel1,
            channel2,
            resultArray
        )
        return resultArray
    }

    fun mix(channel1: ShortArray, channel2: ShortArray, resultArray: ShortArray) {
        var resultIdx = 0
        var result: Short = 0
        val endIdx = Math.min(Math.min(channel1.size, channel2.size), resultArray.size)
        while(resultIdx < endIdx) {
            result = ((channel1[resultIdx] + channel2[resultIdx]) shr 1).toShort()
            resultArray[resultIdx] = result
            resultIdx++
        }
    }

    fun mix(channels: List<FloatArray>, channelParams: List<ChannelParams>, resultArray: FloatArray) {
        var idx: Int = 0
        var minSize = channels.firstOrNull()?.size ?: return
        var result = 0f
        var channelsCount = channels.size
        channels.forEach {
            if (it.size < minSize) {
                minSize = it.size
            }
        }
        while(idx < minSize) {
            result = 0f
            channels.forEach {
                result += it[idx] / channelsCount
            }
            resultArray[idx] = result
        }
    }

}