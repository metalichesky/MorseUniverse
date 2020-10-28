package com.example.morsedetector.repo

import com.example.morsedetector.model.Alphabet
import com.example.morsedetector.util.Constants.digitsAlphabet
import com.example.morsedetector.util.Constants.englishAlphabet
import com.example.morsedetector.util.Constants.russianAlphabet

object AlphabetRepo {

    fun getAllAlphabets(): List<Alphabet> {
        return listOf(
            englishAlphabet,
            russianAlphabet,
            digitsAlphabet
        )
    }
}

