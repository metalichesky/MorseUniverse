package com.example.traindatagenerator.model

enum class Encoding(var bytesPerSample: Int, val androidType: Int) {
    PCM_8BIT(1, 8),
    PCM_16BIT(2, 16),
    PCM_24BIT(3, 24),
    PCM_FLOAT(4, 32)
}