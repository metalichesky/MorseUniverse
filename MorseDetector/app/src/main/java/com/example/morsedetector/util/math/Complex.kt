package com.example.morsedetector.util.math

import kotlin.math.*

class ComplexNumber() {
    companion object {
        const val DELTA = 0.000001
        var ROOT_NUMBER = 0
        var ROOT_DEGREE = 2
        var precision = 4
        private val HALF_PI = (Math.PI / 2.0).toFloat()

    }

    var r = 0f
    var i = 0f

    constructor(real: Float, image: Float) : this() {
        this.r = real
        this.i = image
    }

    private fun set(real: Float, image: Float) {
        this.r = real
        this.i = image
    }


    fun copy(): ComplexNumber = ComplexNumber(this.r, this.i)

    operator fun plus(addition: ComplexNumber): ComplexNumber =
        ComplexNumber(
            this.r + addition.r,
            this.i + addition.i
        )

    operator fun minus(subtrahend: ComplexNumber): ComplexNumber =
        ComplexNumber(
            this.r - subtrahend.r,
            this.i - subtrahend.i
        )

    operator fun times(multiplier: ComplexNumber): ComplexNumber =
        ComplexNumber(
            this.r * multiplier.r - this.i * multiplier.i,
            this.r * multiplier.i + this.i * multiplier.r
        )

    private fun isZero(value: Float): Boolean {
        return value.absoluteValue < DELTA
    }

    private fun isEquals(value1: Float, value2: Float): Boolean {
        return (value1 - value2).absoluteValue < DELTA
    }

    //    @Throws(IllegalArgumentException::class)
    operator fun div(divider: ComplexNumber): ComplexNumber {
        return if (!isZero(divider.r) && !isZero(divider.i)) {
            ComplexNumber(
                (this.r * divider.r + this.i * divider.i) / (divider.r * divider.r + divider.i * divider.i),
                (this.i * divider.r - this.r * divider.i) / (divider.r * divider.r + divider.i * divider.i)
            )
        } else {
            throw IllegalArgumentException("dividing by zero")
        }
    }

    fun square(): ComplexNumber =
        ComplexNumber(this.r * this.r - this.i * this.i, 2.0f * this.r * this.i)

    fun pow(degree: Int): ComplexNumber {
        val module = this.module().pow(degree)
        val argument = this.argument()
        return ComplexNumber(
            (module * cos(degree * argument)).toFloat(),
            (module * sin(degree * argument)).toFloat()
        )
    }

    fun reverse(): ComplexNumber {
        val a = r
        val b = i
        val newReal = a / ((a * a) + (b * b))
        val newImage = -b / ((a * a) + (b * b))
        return ComplexNumber(newReal, newImage)
    }

    fun argument(): Float {
        return when {
            this.r > DELTA / 2.0 -> atan(this.i / this.r)
            this.r < -DELTA / 2.0 -> atan(this.i / this.r) + PI.toFloat()
            this.i > DELTA / 2.0 -> HALF_PI
            this.i < -DELTA / 2.0 -> -HALF_PI
            else -> -1.0f
        }
    }

    fun angle(): Float {
        return argument() * 180.0f / PI.toFloat()
    }

    fun module(): Float {
        return sqrt(this.r * this.r + this.i * this.i)
    }

    fun root(degree: Int = ROOT_DEGREE, rootNumber: Int = ROOT_NUMBER): ComplexNumber {
        val k = rootNumber.rem(degree)
        val module = this.module().pow(1.0f / degree)
        val argument = this.argument()
        return ComplexNumber(
            module * cos((argument + 2.0 * PI * k) / degree).toFloat(),
            module * sin((argument + 2.0 * PI * k) / degree).toFloat()
        )
    }

    override fun equals(other: Any?): Boolean {
        return (other is ComplexNumber) && isEquals(this.i, other.i) && isEquals(this.r, other.r)
    }


    override fun toString(): String {
        return (String.format("%.5f + i*%.5f", r, i).replace(",", "."))
    }


    fun add(number: ComplexNumber): ComplexNumber {
        return this + number
    }

    fun sub(number: ComplexNumber): ComplexNumber {
        return this - number
    }

    fun mul(number: ComplexNumber): ComplexNumber {
        return this * number
    }

    fun pow(number: ComplexNumber): ComplexNumber {
        val n = 0
        val x1 = this.r
        val y1 = this.i
        val x2 = number.r
        val y2 = number.i

        val tempReal1 = exp(x2 * ln(sqrt(x1 * x1 + y1 * y1)))
        val tempReal2 = exp(-y2 * (atan(y1 / x1) + 2 * PI * n))
        val tempReal3 = cos(x2 * (atan(y1 / x1) + 2 * PI * n))
        val tempReal4 = cos(y2 * ln(sqrt(x1 * x1 + y1 * y1)))

        val tempImage1 = sin(x2 * (atan(y1 / x1) + 2 * PI * n))
        val tempImage2 = sin(y2 * ln(sqrt(x1 * x1 + y1 * y1)))

        val real = tempReal1 * tempReal2 * (tempReal3 * tempReal4 - tempImage1 * tempImage2)
        val image = tempReal1 * tempReal2 * (tempImage1 * tempReal4 + tempImage2 * tempReal3)

        return ComplexNumber(real.toFloat(), image.toFloat())
    }

    fun sqrt(rootNumber: Int = ROOT_NUMBER): ComplexNumber {
        return root(2, rootNumber)
    }
}
