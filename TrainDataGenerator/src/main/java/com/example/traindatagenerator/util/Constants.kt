package com.example.traindatagenerator.util

import com.example.traindatagenerator.model.Alphabet
import com.example.traindatagenerator.model.Encoding
import com.example.traindatagenerator.model.MorseCode
import com.example.traindatagenerator.model.Symbol
import com.example.traindatagenerator.repo.AlphabetRepo

object Constants {
    const val DEFAULT_SAMPLE_RATE = 48000
    const val DEFAULT_CHANNELS_COUNT = 1
    val DEFAULT_ENCODING = Encoding.PCM_8BIT


    const val MORSE_GROUP_SIZE = 5

    val englishAlphabet: Alphabet = Alphabet().apply {
        val englishSymbols = listOf<Symbol>(
            Symbol(this, "A", 0.082f, MorseCode(".-")),
            Symbol(this, "B", 0.015f, MorseCode("-...")),
            Symbol(this, "C", 0.028f, MorseCode("-.-.")),
            Symbol(this, "D", 0.043f, MorseCode("-..")),
            Symbol(this, "E", 0.13f, MorseCode(".")),
            Symbol(this, "F", 0.020f, MorseCode("..-.")),
            Symbol(this, "G", 0.02f, MorseCode("--.")),
            Symbol(this, "H", 0.061f, MorseCode("....")),
            Symbol(this, "I", 0.07f, MorseCode("..")),
            Symbol(this, "J", 0.0015f, MorseCode(".---")),
            Symbol(this, "K", 0.0075f, MorseCode("-.-")),
            Symbol(this, "L", 0.04f, MorseCode(".-..")),
            Symbol(this, "M", 0.024f, MorseCode("--")),
            Symbol(this, "N", 0.067f, MorseCode("-.")),
            Symbol(this, "O", 0.075f, MorseCode("---")),
            Symbol(this, "P", 0.019f, MorseCode(".--.")),
            Symbol(this, "Q", 0.00095f, MorseCode("--.-")),
            Symbol(this, "R", 0.06f, MorseCode(".-.")),
            Symbol(this, "S", 0.061f, MorseCode("...")),
            Symbol(this, "T", 0.091f, MorseCode("-")),
            Symbol(this, "U", 0.028f, MorseCode("..-")),
            Symbol(this, "V", 0.0098f, MorseCode("...-")),
            Symbol(this, "W", 0.024f, MorseCode(".--")),
            Symbol(this, "X", 0.0015f, MorseCode("-..-")),
            Symbol(this, "Y", 0.02f, MorseCode("-.--")),
            Symbol(this, "Z", 0.00074f, MorseCode("--.."))
        )
        this.symbols.addAll(englishSymbols)
    }

    val russianAlphabet: Alphabet = Alphabet().apply {
        val russianSymbols = listOf<Symbol>(
            Symbol(this, "А", 0.0801f, MorseCode(".-")),
            Symbol(this, "Б", 0.0159f, MorseCode("-...")),
            Symbol(this, "В", 0.0454f, MorseCode(".--")),
            Symbol(this, "Г", 0.017f, MorseCode("--.")),
            Symbol(this, "Д", 0.0298f, MorseCode("-..")),
            Symbol(this, "Е", 0.0845f, MorseCode(".")),
//        Symbol(RUSSIAN_ALPHABET_ID, "Ё", 0.0004f),
            Symbol(this, "Ж", 0.0094f, MorseCode("...-")),
            Symbol(this, "З", 0.0165f, MorseCode("--..")),
            Symbol(this, "И", 0.0735f, MorseCode("..")),
            Symbol(this, "Й", 0.0121f, MorseCode(".---")),
            Symbol(this, "К", 0.0349f, MorseCode("-.-")),
            Symbol(this, "Л", 0.044f, MorseCode(".-..")),
            Symbol(this, "М", 0.0321f, MorseCode("--")),
            Symbol(this, "Н", 0.067f, MorseCode("-.")),
            Symbol(this, "О", 0.1097f, MorseCode("---")),
            Symbol(this, "П", 0.0281f, MorseCode(".--.")),
            Symbol(this, "Р", 0.0473f, MorseCode(".-.")),
            Symbol(this, "С", 0.0546f, MorseCode("...")),
            Symbol(this, "Т", 0.0626f, MorseCode("-")),
            Symbol(this, "У", 0.0262f, MorseCode("..-")),
            Symbol(this, "Ф", 0.0026f, MorseCode("..-.")),
            Symbol(this, "Х", 0.0097f, MorseCode("....")),
            Symbol(this, "Ц", 0.0048f, MorseCode("-.-.")),
            Symbol(this, "Ч", 0.0144f, MorseCode("---.")),
            Symbol(this, "Ш", 0.0073f, MorseCode("----")),
            Symbol(this, "Щ", 0.0036f, MorseCode("--.-")),
//        Symbol("Ъ", 0.0004f, MorseCode("")),
            Symbol(this, "Ы", 0.019f, MorseCode("-.--")),
            Symbol(this, "Ь", 0.0174f, MorseCode("-..-")),
            Symbol(this, "Э", 0.0032f, MorseCode("..-..")),
            Symbol(this, "Ю", 0.0064f, MorseCode("..--")),
            Symbol(this, "Я", 0.0201f, MorseCode(".-.-"))
        )
        this.symbols.addAll(russianSymbols)
    }


    val digitsAlphabet: Alphabet = Alphabet().apply {
        val digits = listOf<Symbol>(
            Symbol(this, "1", 0.1f, MorseCode(".----")),
            Symbol(this, "2", 0.1f, MorseCode("..---")),
            Symbol(this, "3", 0.1f, MorseCode("...--")),
            Symbol(this, "4", 0.1f, MorseCode("....-")),
            Symbol(this, "5", 0.1f, MorseCode(".....")),
            Symbol(this, "6", 0.1f, MorseCode("-....")),
            Symbol(this, "7", 0.1f, MorseCode("--...")),
            Symbol(this, "8", 0.1f, MorseCode("---..")),
            Symbol(this, "9", 0.1f, MorseCode("----.")),
            Symbol(this, "0", 0.1f, MorseCode("-----"))
        )
        this.symbols.addAll(digits)
    }

}


fun main() {
    println("russian alphabet unit ${Constants.russianAlphabet.getUnitMs(12f)}")
    println("russian alphabet unit ${Constants.digitsAlphabet.getUnitMs(12f)}")
    println("russian alphabet unit ${Constants.englishAlphabet.getUnitMs(12f)}")
}