package com.example.morsedetector.model

class Symbol(
    val alphabet: Alphabet,
    val symbol: String = "",
    var frequency: Float = 0f,
    var morseCode: MorseCode = MorseCode("")
) {
    override fun toString(): String {
        return symbol
    }
}