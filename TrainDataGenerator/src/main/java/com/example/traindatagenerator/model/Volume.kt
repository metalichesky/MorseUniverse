package com.example.traindatagenerator.model

import kotlin.math.log
import kotlin.math.pow

class Volume {
    companion object {
        const val VOLUME_MIN_RATIO = 0.001f
        const val VOLUME_MAX_RATIO = 1f
        val VOLUME_MAX_DB = fromRatio(VOLUME_MAX_RATIO).getDb()

        fun fromDb(db: Float): Volume {
            return Volume().apply {
                val ratio = 10f.pow(db / 10f) * VOLUME_MIN_RATIO
                this.internal = ratio.clamp(0f, VOLUME_MAX_RATIO)
            }
        }

        fun fromRatio(ratio: Float): Volume {
            return Volume().apply {
                this.internal = ratio.clamp(0f, VOLUME_MAX_RATIO)
            }
        }
    }
    private var internal = 0f

    private constructor()

    fun getDb(): Float {
        return log(internal / VOLUME_MIN_RATIO, 10f) * 10f
    }

    fun getRatio(): Float {
        return internal
    }
}

fun Float.clamp(min: Float, max: Float): Float {
    return if (this < min) {
        min
    } else if (this > max) {
        max
    } else {
        this
    }
}