package com.example.morsedetector.util

import com.example.morsedetector.model.AudioParams

class SilenceGenerator {

    fun generate(dataArray: FloatArray) {
        for (idx in dataArray.indices) {
            dataArray[idx] = 0f
        }
    }
}