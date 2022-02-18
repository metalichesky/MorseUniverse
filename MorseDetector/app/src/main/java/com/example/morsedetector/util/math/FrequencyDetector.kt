package com.example.morsedetector.util.math

import com.example.morsedetector.model.AudioParams
import com.example.morsedetector.util.audio.transformer.FrequencyAudioFilter
import com.example.morsedetector.util.audio.transformer.FrequencyFilter

class FrequencyDetector {
    companion object {

    }

    val audioParams = AudioParams.createDefault()

    fun getFrequenciesFromSpectrum(data: Array<ComplexNumber>) {
        val samplesCount = data.size
        val frequencyResolution = FrequencyAudioFilter.getFrequencyResolution(samplesCount, audioParams.sampleRate)
        println("getFrequenciesFromSpectrum() frequency resolution = ${frequencyResolution} samplesCount = ${samplesCount}")

        var maxModule = 0f
        var index = 0
        val halfSize = data.size / 2
        data.forEachIndexed {idx, value ->
            if (value.module() > maxModule && idx < halfSize) {
                maxModule = value.module()
                index = idx
            }
        }
        val frequency = index * frequencyResolution
        println("getFrequenciesFromSpectrum() frequency pike at ${frequency} Hz")

    }

}