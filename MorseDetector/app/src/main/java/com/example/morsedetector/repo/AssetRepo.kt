package com.example.morsedetector.repo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.example.morsedetector.App
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.Exception


object AssetRepo {

    fun getAssetDrawable(assetPath: String): Drawable? {
        var imageAssetPath = assetPath
        var fileStream: InputStream? = null
        try {
            fileStream = App.instance.assets.open(imageAssetPath)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        fileStream ?: return null
        return Drawable.createFromStream(fileStream, null)
    }

    fun getAssetBitmap(assetPath: String): Bitmap? {
        var fileStream: InputStream? = null
        try {
            fileStream = App.instance.assets.open(assetPath)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        fileStream ?: return null
        return BitmapFactory.decodeStream(fileStream)
    }

    fun getAssetStream(assetPath: String): InputStream? {
        var fileStream: InputStream? = null
        try {
            fileStream = App.instance.assets.open(assetPath)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return fileStream
    }

    fun copyToInternalFile(assetPath: String, name: String): File? {
        var fileStream: InputStream? = null
        try {
            fileStream = App.instance.assets.open(assetPath)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        fileStream ?: return null
        val externalRoot = App.instance.getExternalFilesDir(null)
        val resultFile = File(externalRoot, "model.tflite")
        copyStreamToFile(fileStream, resultFile)
        return resultFile
    }

    private fun copyStreamToFile(inputStream: InputStream, outputFile: File) {
        inputStream.use { input ->
            val outputStream = FileOutputStream(outputFile)
            outputStream.use { output ->
                val buffer = ByteArray(4 * 1024) // buffer size
                while (true) {
                    val byteCount = input.read(buffer)
                    if (byteCount < 0) break
                    output.write(buffer, 0, byteCount)
                }
                output.flush()
            }
        }
    }
}