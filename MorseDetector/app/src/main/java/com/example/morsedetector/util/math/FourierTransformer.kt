package com.example.morsedetector.util.math

import com.example.morsedetector.model.WaveformType
import com.example.morsedetector.util.AudioDataTransformer
import com.example.morsedetector.util.FrequencyGenerator
import com.example.morsedetector.util.WindowFunction
import com.example.morsedetector.util.WindowFunctions
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

class FourierTransformer {
    //fastFourierTransform without recursy
//    fun FFT(dataArray: Array<ComplexNumber>, r: Int, forward: Boolean): ComplexNumber {
//        val A1 = dataArray
//        val A2 = Array<ComplexNumber>(dataArray.size) { ComplexNumber() }
//        var position = 0
//        var k = 0
//        var degree = 0f
//        val divider = if (forward) 2 else 1
//        /*for (k = 0; k < arraySize; k++){
//        A1[k] = f[k];
//    }
//    */
//        for (s in 1..r) {
//            for (k in 0 until dataArray.size) {
//                degree = 0f
//                for (position in 0 until s) {
//                    if (matrixK[k][position])
//                        degree += pow2(position);
//                }
//                degree *= TWO_PI / pow2(s);
//                if (forward) {
//                    degree *= -1;
//                }
//                A2[k].real = cos(degree);
//                A2[k].img = sin(degree);
//
//                if (matrixK[k][s - 1] == 0) {
//                    A2[k] *= A1[k + pow2(r - s)];
//                    A2[k] += A1[k];
//                } else {
//                    A2[k] *= A1[k];
//                    A2[k] += A1[k - pow2(r - s)];
//                }
//                //complexMultiply++;
//                A2[k].real /= divider;
//                A2[k].img /= divider;
//            }
//            for (int i = 0; i < arraySize; i++){
//                A1[i] = A2[i];
//            }
//        }
//        for (k = 0; k < arraySize; k++) {
//            position = 0;
//            for (s = 0; s < r; s++) {
//            if (matrixK[k][s] == 1)
//                position += pow2(s);
//        }
//            A2[k] = A1[position];
//        }
//        return A2;
//    }


    fun completeDFT(dataArray: Array<ComplexNumber>, forward: Boolean = true): Array<ComplexNumber> {
        val resultArray = Array<ComplexNumber>(dataArray.size) { ComplexNumber() }
        val sign = if (forward) -1f else 1f
        val coefficient = if (forward) ComplexNumber(1f, 0f)
            else ComplexNumber(1f / dataArray.size, 0f)
        val omega = ComplexNumber(
            cos(sign * 2f * PI / dataArray.size).toFloat(),
            sin(sign * 2f * PI / dataArray.size).toFloat()
        )
        for (k in resultArray.indices) {
            var result = ComplexNumber()
            for (n in dataArray.indices) {
                //result += dataArray[n] * omega.pow(k * n)
                result += dataArray[n] * ComplexNumber(
                    cos(sign * 2f * PI * k * n / dataArray.size).toFloat(),
                    sin(sign * 2f * PI * k * n / dataArray.size).toFloat()
                )
            }
            resultArray[k] = result * coefficient
        }
        return resultArray
    }
}

fun main() {
    val frequencyGenerator: FrequencyGenerator = FrequencyGenerator()
    val dataTransformer: AudioDataTransformer = AudioDataTransformer()
    val windowFunctions = WindowFunctions()

    val durationMs = 5f
    val data = dataTransformer.generateFloatArray(durationMs)
    windowFunctions.applyWindow(data, WindowFunction.Type.HAMMING)

    frequencyGenerator.generate(data, WaveformType.SINE, 1000f, 1f)

    val complexData = dataTransformer.floatArrayToComplexArray(data)

    val fourierTransformer = FourierTransformer()

    val forwardMs = measureNanoTime {
        val resultForward = fourierTransformer.completeDFT(complexData, true)
        val resultBackward = fourierTransformer.completeDFT(resultForward, false)
//        println("data: ${data.joinToString()}")
        resultForward.forEach {
            println("${it.module()}".replace(".", ","))
        }
//        println("resultForward: ${resultForward.joinToString()}")
//        println("resultBackward: ${resultBackward.joinToString()}")
    }
    println("measured ${forwardMs} nano")


}
