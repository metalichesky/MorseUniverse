package com.example.morsedetector.model

enum class NoiseType(val id: Int, val drawableRes: Int, val colorRes: Int, val nameRes: Int, var selected: Boolean = false) {
    WHITE(0, ),
    BROWN(1, ),
    PINK(2, ),
    BLUE(3, ),
    VIOLET(4, )
}