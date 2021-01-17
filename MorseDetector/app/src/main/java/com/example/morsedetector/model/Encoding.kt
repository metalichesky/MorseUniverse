package com.example.morsedetector.model

import android.media.AudioFormat

enum class Encoding(var bytesPerSample: Int, val androidType: Int) {
    PCM_8BIT(1, AudioFormat.ENCODING_PCM_8BIT),
    PCM_16BIT(2, AudioFormat.ENCODING_PCM_16BIT),
    PCM_24BIT(3, AudioFormat.ENCODING_INVALID),
    PCM_FLOAT(4, AudioFormat.ENCODING_PCM_FLOAT)
}