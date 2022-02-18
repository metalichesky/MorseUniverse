package com.example.morsedetector.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.example.morsedetector.R
import com.example.morsedetector.repo.AssetRepo
import kotlinx.coroutines.launch
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException


class DecoderFragment() : BaseFragment() {
    companion object {
        const val LOG_TAG = "DecoderFragment"
    }

    private var interpreter: Interpreter? = null
    var probabilityBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 1001), DataType.UINT8)
    var tImage = TensorImage(DataType.UINT8)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_decoder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadModel()
        AssetRepo.getAssetBitmap("А1.jpg")?.let {
            classify(it)
        }
    }

    fun loadModel() {
        val activity = activity ?: return
        try{
            val tfliteModel = AssetRepo.copyToInternalFile("model.tflite", "model.tflite") ?: return
            interpreter = Interpreter(tfliteModel)
        } catch (e: IOException){
            Log.e("tfliteSupport", "Error reading model", e);
        }

    }

    fun classify(bitmap: Bitmap) {
//        val processor = ImageProcessor.Builder().build()
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(180, 180, ResizeOp.ResizeMethod.BILINEAR))
            .build()
        lifecycleScope.launch {
            tImage.load(bitmap)
            tImage = imageProcessor.process(tImage)
            interpreter?.run(tImage.buffer, probabilityBuffer.buffer)
            Log.d(LOG_TAG , "classify() ${probabilityBuffer.floatArray.joinToString()}")
        }
    }


}

