package com.example.morsedetector.model

abstract class MorseSound(val unitDuration: Float, val silence: Boolean) {}

class MorseSymbol(val notation: String, unitDuration: Float) : MorseSound(unitDuration, false) {
    companion object {
        val DOT = MorseSymbol(".", 1f)
        val DASH = MorseSymbol("-", 3f)
    }
}

class MorsePause(unitDuration: Float) : MorseSound(unitDuration, true) {
    companion object {
        val IN_SYMBOL = MorsePause(1f)
        val BETWEEN_SYMBOLS = MorsePause(3f)
        val BETWEEN_GROUPS = MorsePause(7f)
    }
}

class MorseCode {
    companion object {
        val CHECK_NOTATION_REGEX = Regex("[\\.-]+")
        val SLPIT_REGEX = Regex("")
    }

    var morseSymbols: List<MorseSymbol> = emptyList()

    constructor(notation: String) {
        if (notation.matches(CHECK_NOTATION_REGEX)) {
            morseSymbols = notation.split(SLPIT_REGEX).mapNotNull {
                when (it) {
                    MorseSymbol.DASH.notation -> MorseSymbol.DASH
                    MorseSymbol.DOT.notation -> MorseSymbol.DOT
                    else -> null
                }
            }
        } else {
            // empty or wrong string
        }
    }

    fun getSoundSequence(): MutableList<MorseSound> {
        val soundSequence = mutableListOf<MorseSound>()
        for (i in morseSymbols.indices) {
            soundSequence.add(morseSymbols[i])
            if (i < morseSymbols.size - 1) {
                soundSequence.add(MorsePause.IN_SYMBOL)
            }
        }
        return soundSequence
    }

    override fun toString(): String {
        return morseSymbols.map { it.notation }.joinToString("")
    }
}