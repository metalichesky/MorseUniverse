package com.example.morsedetector.model

import com.example.morsedetector.R

enum class WaveformType(val id: Int, val drawableRes: Int, val nameRes: Int, var selected: Boolean = false) {
    SINE(0, R.drawable.ic_waveform_sine, R.string.waveform_sine),
    TRIANGLE(1, R.drawable.ic_waveform_triangle, R.string.waveform_triangle),
    SAW_TOOTH(2, R.drawable.ic_waveform_sawtooth, R.string.waveform_saw_tooth),
    SQUARE(3, R.drawable.ic_waveform_square, R.string.waveform_square)
}