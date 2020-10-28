package com.example.morsedetector.model

import java.util.*
import com.example.morsedetector.model.MorseSymbol
import com.example.morsedetector.util.Constants
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

class Alphabet(
    val id: Int = 0,
    var name: String = ""
) {
    var symbols: MutableList<Symbol> = LinkedList()
    private val rand = Random(System.currentTimeMillis())

    fun setupFrequencies() {

    }

    fun findSymbol(string: String): Symbol? {
        return symbols.find {
            println("${it} equals ${string}")
            it.symbol == string
        }
    }

    fun getAvgSymbolUnits(): Float {
        var avgSymbolUnits = 0f
        symbols.forEach {
            val symbolPausesUnits = MorsePause.IN_SYMBOL.unitDuration * (it.morseCode.morseSymbols.size - 1).toFloat()
            val symbolUnits = it.morseCode.morseSymbols.sumByDouble { it.unitDuration.toDouble() }.toFloat()
            avgSymbolUnits += (symbolPausesUnits + symbolUnits) * it.frequency
        }
        return avgSymbolUnits
    }

    fun getUnitMs(groupPerMinute: Float): Float {
        val minute = 60000f

        val groupsCount = groupPerMinute.roundToInt()
        val pausesBetweenGroups = max(groupsCount - 1f, 0f)
        val pausesBetweenGroupsUnits = MorsePause.BETWEEN_GROUPS.unitDuration * pausesBetweenGroups

        val groupSize = Constants.MORSE_GROUP_SIZE

        val pausesBetweenSymbols = (groupSize - 1) * groupPerMinute
        val pausesBetweenSymbolsUnits = MorsePause.BETWEEN_SYMBOLS.unitDuration * pausesBetweenSymbols

        val pausesUnits = pausesBetweenGroupsUnits + pausesBetweenSymbolsUnits

        val symbolsCount = groupPerMinute * groupSize
        val symbolUnits = getAvgSymbolUnits() * symbolsCount

        val allUnits  = pausesUnits + symbolUnits
        val unitDuration = minute / allUnits
        return unitDuration
    }

    fun getRandSymbol(): Symbol {
        var qumulative = 0f
        var previousQumulative = 0f

        val max = symbols.sumByDouble { it.frequency.toDouble() }.toFloat()
        val randFloat = rand.nextFloat() * max
        var randIndex = 0

        for (i in symbols.indices) {
            previousQumulative = qumulative
            qumulative += symbols[i].frequency
            if (randFloat in previousQumulative..qumulative) {
                randIndex = i
                break
            }
        }

        return symbols[randIndex]
    }
}