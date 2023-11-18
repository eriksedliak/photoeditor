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

    private var slidersState = SlidersState()

    fun changeBrightness(brightnessChange: Int): Bitmap {
        slidersState.brightness = brightnessChange
        return redrawPicture(slidersState)
    }

    fun changeContrast(contrastChange: Int): Bitmap {
        slidersState.contrast = contrastChange
        return redrawPicture(slidersState)
    }

    fun changeGamma(gammaChange: Double): Bitmap {
        slidersState.gamma = gammaChange
        return redrawPicture(slidersState)
    }

    fun changeSaturation(saturationChange: Int): Bitmap {
        slidersState.saturation = saturationChange
        return redrawPicture(slidersState)
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

    /** brightness = R+G+B/3, brightness is changed by adding or subtracting the same amount from each RGB color.
     */
    private fun modifyBrightness(pixels: IntArray, brightnessChange: Int) {
        pixels.indices.forEach {
            val red = limitToRgb(Color.red(pixels[it]) + brightnessChange)
            val green = limitToRgb(Color.green(pixels[it]) + brightnessChange)
            val blue = limitToRgb(Color.blue(pixels[it]) + brightnessChange)
            pixels[it] = Color.rgb(red, green, blue)
        }
    }

    /** contrast adjusted R equals: alpha * (R - avgBrightness) + avgBrightness
     *  where
     *  alpha = (255 + contrastChange)/(255 - contrastChange)
     *  avgBrightness = totalBrightness / all pixels * 3 (R,G,B)
     *  totalBrightness = R + G + B for every pixel
     */
    private fun modifyContrast(pixels: IntArray, contrastChange: Int, width: Int, height: Int) {
        val alpha = calculateAlpha(contrastChange)
        var totalBrightness: Long = 0
        pixels.forEach {
            totalBrightness += (it.red + it.blue + it.green)
        }
        val avgBrightness: Int = (totalBrightness / (width * height * 3)).toInt()
        pixels.indices.forEach {
            val red =
                limitToRgb(((alpha * (pixels[it].red - avgBrightness)) + avgBrightness).toInt())
            val green =
                limitToRgb(((alpha * (pixels[it].green - avgBrightness)) + avgBrightness).toInt())
            val blue =
                limitToRgb(((alpha * (pixels[it].blue - avgBrightness)) + avgBrightness).toInt())
            pixels[it] = Color.rgb(red, green, blue)
        }
    }
    /** Gamma modified Red = 255∗(Red÷255)^gammaChange */
    private fun modifyGamma(pixels: IntArray, gammaChange: Double) {
        pixels.indices.forEach {
            val red = (255 * ((pixels[it].red.toDouble() / 255).pow(gammaChange))).toInt()
            val green = (255 * ((pixels[it].green.toDouble() / 255).pow(gammaChange))).toInt()
            val blue = (255 * ((pixels[it].blue.toDouble() / 255).pow(gammaChange))).toInt()
            pixels[it] = Color.rgb(red, green, blue)
        }
    }

    /** Saturation modified Red = (alpha×(Red−rgbAvg))+rgbAvg
    where
    alpha = (255−saturationChange)(255+saturationChange)
    rgbAvg = (Red+Green+Blue)/3 average RGB value for each pixel
    */
    private fun modifySaturation(pixels: IntArray, saturationChange: Int) {
        val alpha = calculateAlpha(saturationChange)
        pixels.indices.forEach {
            val red = pixels[it].red
            val green = pixels[it].green
            val blue = pixels[it].blue

            val rgbAvg = (red + green + blue) / 3

            val newRed = limitToRgb(((alpha * (red - rgbAvg)) + rgbAvg).toInt())
            val newGreen = limitToRgb(((alpha * (green - rgbAvg)) + rgbAvg).toInt())
            val newBlue = limitToRgb(((alpha * (blue - rgbAvg)) + rgbAvg).toInt())
            pixels[it] = Color.rgb(newRed, newGreen, newBlue)
        }
    }

    private fun redrawPicture(sliders: SlidersState): Bitmap {
        val height = loadedImage.height
        val width = loadedImage.width
        val editedImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        @ColorInt val pixels = IntArray(height * width)

        loadedImage.getPixels(pixels, 0, width, 0, 0, width, height)
        modifyBrightness(pixels, sliders.brightness)
        modifyContrast(pixels, sliders.contrast, width, height)
        modifySaturation(pixels, sliders.saturation)
        modifyGamma(pixels, sliders.gamma)

        editedImage.setPixels(pixels, 0, width, 0, 0, width, height)
        return editedImage
    }
}

class SlidersState(
    var brightness: Int = 0,
    var contrast: Int = 0,
    var saturation: Int = 0,
    var gamma: Double = 1.0
)