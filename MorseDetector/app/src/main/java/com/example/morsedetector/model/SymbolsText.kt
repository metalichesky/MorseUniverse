package com.example.morsedetector.model

import com.example.morsedetector.repo.AlphabetRepo
import java.util.*

class SymbolsText {
    companion object {
        val SLPIT_REGEX = Regex("")
    }

    val symbols: MutableList<Symbol> = mutableListOf()
    val textAlphabets: MutableList<Alphabet> = mutableListOf()

    fun addAlphabet(alphabet: Alphabet) {
        textAlphabets.add(alphabet)
    }

    fun addFromString(string: String) {
        string.split(SLPIT_REGEX).forEach { symbolString ->
            var symbol: Symbol? = null
            for (alphabet in textAlphabets) {
                symbol = alphabet.findSymbol(symbolString)
                if (symbol != null) break
            }
            symbol?.let{
                symbols.add(it)
            }
        }
    }

    override fun toString(): String {
        return symbols.joinToString("")
    }
}
