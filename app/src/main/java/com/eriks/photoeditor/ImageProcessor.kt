package com.eriks.photoeditor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import java.io.IOException
import kotlin.math.pow

class ImageProcessor(private val context: Context, private var loadedImage: Bitmap) {


    fun recalculate(slidersState: SlidersState): Bitmap {

        val pixelsToSet =
            loadImagePixels()
                .brightness(slidersState.brightness)
                .contrast(slidersState.contrast)
                .saturation(slidersState.saturation)
                .gamma(slidersState.gamma)

        val editedImage =
            Bitmap.createBitmap(loadedImage.width, loadedImage.height, Bitmap.Config.ARGB_8888)
        editedImage.setPixels(
            pixelsToSet,
            0,
            loadedImage.width,
            0,
            0,
            loadedImage.width,
            loadedImage.height
        )
        return editedImage
    }


    fun createBitmap(uri: Uri): Bitmap {
        loadedImage = BitmapFactory
            .decodeStream(context.contentResolver.openInputStream(uri))
            ?: throw IOException("Neni")
        return loadedImage
    }

    private fun calculateAlpha(valueChange: Int) =
        (255 + valueChange.toDouble()) / (255 - valueChange)

    private fun limitToRgb(color: Int): Int {
        return if (color < 0) 0
        else if (color > 255) 255
        else color
    }

    private fun loadImagePixels(): IntArray {
        val height = loadedImage.height
        val width = loadedImage.width
        @ColorInt val pixels = IntArray(height * width)
        loadedImage.getPixels(pixels, 0, width, 0, 0, width, height)
        return pixels
    }

    /** brightness = R+G+B/3, brightness is changed by adding or subtracting the same amount from each RGB color.
     */
    private fun IntArray.brightness(brightnessChange: Int): IntArray {
        this.indices.forEach {
            val red = limitToRgb(Color.red(this[it]) + brightnessChange)
            val green = limitToRgb(Color.green(this[it]) + brightnessChange)
            val blue = limitToRgb(Color.blue(this[it]) + brightnessChange)
            this[it] = Color.rgb(red, green, blue)
        }
        return this
    }

    /** contrast adjusted R equals: alpha * (R - avgBrightness) + avgBrightness
     *  where
     *  alpha = (255 + contrastChange)/(255 - contrastChange)
     *  avgBrightness = totalBrightness / all pixels * 3 (R,G,B)
     *  totalBrightness = R + G + B for every pixel
     */
    private fun IntArray.contrast(contrastChange: Int): IntArray {
        var totalBrightness: Long = 0
        val alpha = calculateAlpha(contrastChange)
        this.forEach {
            totalBrightness += (it.red + it.blue + it.green)
        }
        val avgBrightness: Int =
            (totalBrightness / (loadedImage.width * loadedImage.height * 3)).toInt()
        this.indices.forEach {
            val red =
                limitToRgb(((alpha * (this[it].red - avgBrightness)) + avgBrightness).toInt())
            val green =
                limitToRgb(((alpha * (this[it].green - avgBrightness)) + avgBrightness).toInt())
            val blue =
                limitToRgb(((alpha * (this[it].blue - avgBrightness)) + avgBrightness).toInt())
            this[it] = Color.rgb(red, green, blue)
        }
        return this
    }

    /** Gamma modified Red = 255∗(Red÷255)^gammaChange */
    private fun IntArray.gamma(gammaChange: Double): IntArray {
        this.indices.forEach {
            val red = (255 * ((this[it].red.toDouble() / 255).pow(gammaChange))).toInt()
            val green = (255 * ((this[it].green.toDouble() / 255).pow(gammaChange))).toInt()
            val blue = (255 * ((this[it].blue.toDouble() / 255).pow(gammaChange))).toInt()
            this[it] = Color.rgb(red, green, blue)
        }
        return this
    }

    /** Saturation modified Red = (alpha×(Red−rgbAvg))+rgbAvg
    where
    alpha = (255−saturationChange)(255+saturationChange)
    rgbAvg = (Red+Green+Blue)/3 average RGB value for each pixel
     */
    private fun IntArray.saturation(saturationChange: Int): IntArray {
        val alpha = calculateAlpha(saturationChange)
        this.indices.forEach {
            val red = this[it].red
            val green = this[it].green
            val blue = this[it].blue

            val rgbAvg = (red + green + blue) / 3

            val newRed = limitToRgb(((alpha * (red - rgbAvg)) + rgbAvg).toInt())
            val newGreen = limitToRgb(((alpha * (green - rgbAvg)) + rgbAvg).toInt())
            val newBlue = limitToRgb(((alpha * (blue - rgbAvg)) + rgbAvg).toInt())
            this[it] = Color.rgb(newRed, newGreen, newBlue)
        }
        return this
    }
}

class SlidersState(
    var brightness: Int = 0,
    var contrast: Int = 0,
    var saturation: Int = 0,
    var gamma: Double = 1.0
)