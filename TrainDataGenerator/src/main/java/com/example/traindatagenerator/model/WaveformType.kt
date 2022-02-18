package com.example.traindatagenerator.model

enum class WaveformType(val id: Int, val drawableRes: Int = 0, val nameRes: Int = 0, var selected: Boolean = false) {
    SINE(0),
    TRIANGLE(1),
    SAW_TOOTH(2),
    SQUARE(3)
}