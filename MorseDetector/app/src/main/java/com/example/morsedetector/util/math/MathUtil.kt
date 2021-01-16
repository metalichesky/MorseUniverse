package com.example.morsedetector.util.math

import android.R.bool




object MathUtil {
    private const val INT_BITS_COUNT = 32

    fun reverseBits(num: Int, bitCount: Int): Int {
        var result = 0
        var powerOfTwo = 1
        var reversedPowerOfTwo = 1 shl (bitCount - 1)
        for (i in 0 until bitCount) {
            if (num and powerOfTwo != 0) {
                result = result or reversedPowerOfTwo
            }
            powerOfTwo = powerOfTwo shl 1
            reversedPowerOfTwo = reversedPowerOfTwo shr 1
        }
        return result
    }

    fun logTwo(num: Int): Int {
        var logN = 0
        while ((1 shl logN) < num) {
            logN++
        }
        return logN
    }

    fun isPowerOfTwo(x: Int): Boolean {
        return (x != 0) && (x and (x - 1)) == 0
    }

    fun getNearestPowerOfTwo(number: Int): Int {
        return if (isPowerOfTwo(number)) {
            number
        } else {
            getNextPowerOfTwo(number)
        }
    }

    fun getNextPowerOfTwo(number: Int): Int {
        var maxBit = 1
        var bit = 1
        for(i in 0 until INT_BITS_COUNT) {
            if ((bit and number) != 0) {
                maxBit = bit
            }
            bit = bit shl 1
        }
        maxBit = maxBit shl 1
        return maxBit
    }
}

fun Float.clamp(from: Float, to: Float): Float {
    return Math.min(Math.max(this, from), to)
}