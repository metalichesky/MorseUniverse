package com.example.morsedetector.util

import com.example.morsedetector.model.AudioParams
import com.example.morsedetector.util.math.ComplexNumber
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioDataTransformer {
    var audioParams: AudioParams = AudioParams.createDefault()

    fun getFloatArraySize(durationMs: Float): Int {
        return (durationMs * audioParams.bytesPerMs).toInt() / audioParams.encoding.byteRate
    }

    fun generateFloatArray(durationMs: Float): FloatArray {
        val arraySize = getFloatArraySize(durationMs)
        return FloatArray(arraySize)
    }

    fun getByteArraySize(durationMs: Float): Int {
        return (durationMs * audioParams.bytesPerMs).toInt()
    }

    fun generateByteArray(durationMs: Float): ByteArray {
        val arraySize = getByteArraySize(durationMs)
        return ByteArray(arraySize)
    }

    fun floatArrayToByteArray(srcArray: FloatArray): ByteArray {
        val resultArraySize = srcArray.size * audioParams.encoding.byteRate
        val resultArray = ByteArray(resultArraySize)
        floatArrayToByteArray(srcArray, resultArray)
        return resultArray
    }

    fun floatArrayToByteArray(srcArray: FloatArray, resultArray: ByteArray) {
        val channelsCount = audioParams.channelsCount
        val byteRate = audioParams.encoding.byteRate

        var resultIdx = 0
        var srcIdx = 0
        while(srcIdx < srcArray.size) {
            for (channel in 0 until channelsCount) {
                val value = srcArray[srcIdx]
                val resultValues = toByteArray(value, byteRate)
                for (resultValue in resultValues) {
                    if (resultIdx >= resultArray.size) return
                    resultArray[resultIdx] = resultValue
                    resultIdx++
                }
                srcIdx++
            }
        }
    }

    fun byteArrayToFloatArray(srcArray: ByteArray): FloatArray {
        val resultArraySize = srcArray.size / audioParams.encoding.byteRate
        val resultArray = FloatArray(resultArraySize)
        byteArrayToFloatArray(srcArray, resultArray)
        return resultArray
    }

    fun byteArrayToFloatArray(srcArray: ByteArray, resultArray: FloatArray) {
        val channelsCount = audioParams.channelsCount
        val byteRate = audioParams.encoding.byteRate
        var resultIdx = 0
        var srcIdx = 0
        val buffer = ByteArray(byteRate)

        while(srcIdx < srcArray.size) {
            for (channel in 0 until channelsCount) {
                if (resultIdx >= resultArray.size) return
                System.arraycopy(srcArray, srcIdx, buffer, 0, byteRate)
                srcIdx += byteRate
                resultArray[resultIdx] = toFloat(buffer, byteRate)
                resultIdx++
            }
        }
    }

    fun floatArrayToComplexArray(srcArray: FloatArray): Array<ComplexNumber> {
        return Array(srcArray.size) { ComplexNumber(srcArray[it], 0f) }
    }

    fun floatArrayToComplexArray(srcArray: FloatArray, resultArray: Array<ComplexNumber>) {
        var idx = 0
        while(idx < srcArray.size && idx < resultArray.size) {
            resultArray[idx].r = srcArray[idx]
            idx++
        }
    }

    fun complexArrayToFloatArray(srcArray: Array<ComplexNumber>): FloatArray {
        return FloatArray(srcArray.size) { srcArray[it].r }
    }

    fun complexArrayToFloatArray(srcArray: Array<ComplexNumber>, resultArray: FloatArray) {
        var idx = 0
        while (idx < srcArray.size && idx < resultArray.size) {
            resultArray[idx] = srcArray[idx].r
            idx++
        }
    }


    private fun toByteArray(value: Float, byteRate: Int): ByteArray {
        return when (byteRate) {
            4 -> {
                value.toByteArray()
            }
            2 -> {
                value.toShort().toByteArray()
            }
            else -> {
                //make value unsigned here
                (value + audioParams.samplesAmplitude).toByte().toByteArray()
            }
        }
    }

    private fun toFloat(value: ByteArray, byteRate: Int): Float {
        return when (byteRate) {
            4 -> {
                value.toFloat()
            }
            2 -> {
                value.toShort().toFloat()
            }
            else -> {
                //make value unsigned here
                value.toByte().toUByte().toFloat() - audioParams.samplesAmplitude
            }
        }
    }

}

fun main() {
//    val value = 5f
//    println("value ${value} transformed ${value.toByteArray(4).toFloat()}")



    val dataTransformer = AudioDataTransformer()

    val floatArray = FloatArray(10) {
        it.toFloat()
    }

    val byteArray = dataTransformer.floatArrayToByteArray(floatArray)

    val newFloatArray = dataTransformer.byteArrayToFloatArray(byteArray)

    println("${newFloatArray.joinToString()}")
}


fun ByteArray.toFloat(): Float {
    return ByteBuffer.wrap(this)
        .order(ByteOrder.LITTLE_ENDIAN)
        .getFloat()
}

fun ByteArray.toByte(): Byte {
    return this[0]
}

fun ByteArray.toShort(): Short {
    return ByteBuffer.wrap(this)
        .order(ByteOrder.LITTLE_ENDIAN)
        .put(this)
        .getShort()
}

fun Float.toByteArray(bytesCount: Int = 4): ByteArray {
    return ByteBuffer.allocate(bytesCount)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putFloat(this)
        .array()
}

fun Int.toByteArray(): ByteArray {
    return ByteBuffer.allocate(4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(this)
        .array()
}

fun Short.toByteArray(): ByteArray {
    return ByteBuffer.allocate(2)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putShort(this)
        .array()
}

fun Byte.toByteArray(): ByteArray {
    val array = ByteArray(1)
    array[0] = this
    return array
}