package com.example.traindatagenerator.util.math

import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

class Spline() {
    class SplineTuple {
        var a: Float = 0f
        var b: Float = 0f
        var c: Float = 0f
        var d: Float = 0f
        var x: Float = 0f
    }

    var splines: Array<SplineTuple> =
        emptyArray() //= Array(pointsCount) { SplineTuple() }  // Сплайн

    // Построение сплайна
    // x - узлы сетки, должны быть упорядочены по возрастанию, кратные узлы запрещены
    // y - значения функции в узлах сетки
    // pointsCount - количество узлов сетки
    fun build(x: FloatArray, y: FloatArray, pointsCount: Int) {
        // Инициализация массива сплайнов
        splines = Array<SplineTuple>(pointsCount) { SplineTuple() }
        for (i in 0 until pointsCount) {
            splines[i].x = x[i]
            splines[i].a = y[i]
        }
        splines.sortBy {
            it.x
        }
        splines[0].c = 0f
        splines[pointsCount - 1].c = 0f
        val alpha = FloatArray(pointsCount - 1)
        val beta = FloatArray(pointsCount - 1)
        alpha[0] = 0f
        beta[0] = 0f
        // Решение СЛАУ относительно коэффициентов сплайнов c[i] методом прогонки для трехдиагональных матриц
        // Вычисление прогоночных коэффициентов - прямой ход метода прогонки
        for (i in 1 until (pointsCount - 1)) {
            val h_i = splines[i].x - splines[i - 1].x
            val h_i1 = splines[i + 1].x - splines[i].x
            val A = h_i
            val C = 2f * (h_i + h_i1)
            val B = h_i1
            val F = 6f * ((splines[i + 1].a - splines[i].a) / h_i1 - (splines[i].a - splines[i-1].a) / h_i)
            val z = (A * alpha[i - 1] + C)
            alpha[i] = -B / z
            beta[i] = (F - A * beta[i - 1]) / z
        }

        // Нахождение решения - обратный ход метода прогонки
        for (i in (pointsCount - 2) downTo 1) {
            splines[i].c = alpha[i] * splines[i + 1].c + beta[i]
            // По известным коэффициентам c[i] находим значения b[i] и d[i]<br />
        }
        for (i in (pointsCount - 1) downTo 1) {
            val h_i = splines[i].x - splines[i - 1].x
            splines[i].d = (splines[i].c - splines[i - 1].c) / h_i
            splines[i].b =
                h_i * (2f * splines[i].c + splines[i - 1].c) / 6f + (splines[i].a - splines[i - 1].a) / h_i
        }
    }

    // Вычисление значения интерполированной функции в произвольной точке<br />
    fun calculate(x: Float): Float {
        val pointsCount = splines.size
        val s = if (x <= splines[0].x) {
            // Если x меньше точки сетки x[0] - пользуемся первым эл-тов массива
            splines[0]
        } else if (x >= splines[pointsCount - 1].x) {
            // Если x больше точки сетки x[n - 1] - пользуемся последним эл-том массива
            splines[pointsCount - 1]
        } else {
            // Иначе x лежит между граничными точками сетки - производим бинарный поиск нужного эл-та массива
            var i = 0
            var j = pointsCount - 1
            while (i + 1 < j) {
                val k = i + (j - i) / 2
                if (x <= splines[k].x) {
                    j = k
                } else {
                    i = k
                }
            }
            splines[j]
        }
        val dx = (x - s.x)
        // Вычисляем значение сплайна в заданной точке по схеме Горнера (в принципе, "умный" компилятор применил бы схему Горнера сам, но ведь не все так умны, как кажутся)
        return s.a + (s.b + (s.c / 2f + s.d * dx / 6f) * dx) * dx
    }
}

fun main() {
    val spline = Spline()
    val pointsCount = 10
    val random = Random(System.currentTimeMillis())

    val points = Array<Float>(pointsCount) {
        it.toFloat()
    }
    val values = points.map {
        random.nextInt().absoluteValue.rem(100).toFloat()
    }

    val buildTime = measureNanoTime {
        spline.build(points.toFloatArray(), values.toFloatArray(), pointsCount)
    }
    println("build time ${buildTime}}")

    for (i in 0 until pointsCount) {
        println("point x = ${points[i]} y = ${values[i]}")

    }

    for (i in 0 until 100) {
        val point = (i - 40f)
        val value = spline.calculate(point)
        println("${point};${value}")
    }
}