package com.example.traindatagenerator.repo

import com.example.traindatagenerator.model.Alphabet
import com.example.traindatagenerator.util.Constants.digitsAlphabet
import com.example.traindatagenerator.util.Constants.englishAlphabet
import com.example.traindatagenerator.util.Constants.russianAlphabet

object AlphabetRepo {

    fun getAllAlphabets(): List<Alphabet> {
        return listOf(
            englishAlphabet,
            russianAlphabet,
            digitsAlphabet
        )
    }
}

