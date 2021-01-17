package com.example.traindatagenerator.model

import com.example.traindatagenerator.repo.AlphabetRepo
import java.util.*
import kotlin.math.absoluteValue

class SymbolsText {
    companion object {
        val SLPIT_REGEX = Regex("")
        private val random = Random(System.currentTimeMillis())
    }

    val symbols: MutableList<Symbol> = mutableListOf()
    val textAlphabets: MutableList<Alphabet> = mutableListOf()

    fun addAlphabet(alphabet: Alphabet) {
        textAlphabets.add(alphabet)
    }

    fun generateRandom(symbolsCount: Int) {
        symbols.clear()
        if (textAlphabets.isEmpty()) return
        while(symbols.size < symbolsCount) {
            val alphabetIdx = random.nextInt().absoluteValue.rem(textAlphabets.size)
            textAlphabets.getOrNull(alphabetIdx)?.let{ alphabet ->
                if (symbols.size < symbolsCount) {
                    symbols.add(alphabet.getRandSymbol())
                }
            }
        }
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
