package com.example.traindatagenerator.util.audio.generator

class SilenceGenerator {

    fun generate(dataArray: FloatArray) {
        for (idx in dataArray.indices) {
            dataArray[idx] = 0f
        }
    }
}