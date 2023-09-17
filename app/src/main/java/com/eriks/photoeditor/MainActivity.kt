package com.eriks.photoeditor

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {

    private lateinit var currentImage: ImageView
    private lateinit var pickImageBtn: Button
    private lateinit var brightnessSl: Slider
    private lateinit var currentBitmap: Bitmap

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val photoUri = result.data?.data ?: return@registerForActivityResult
                currentImage.setImageURI(photoUri)
                currentBitmap = ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(contentResolver, photoUri)
                ).copy(Bitmap.Config.RGB_565, true)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()

        //do not change this line
        currentBitmap = createBitmap()
        currentImage.setImageBitmap(currentBitmap)

        pickImageBtn.setOnClickListener {
            pickImage()
        }
        brightnessSl.addOnChangeListener { _, value, _ ->
            redrawPicture(currentBitmap, value.toInt())
        }
    }

    private fun bindViews() {
        currentImage = findViewById(R.id.ivPhoto)
        pickImageBtn = findViewById(R.id.btnGallery)
        brightnessSl = findViewById(R.id.slBrightness)
    }

    private fun limitToRgb(color: Int, value: Int): Int {
        return if (color + value < 0) 0
        else if (color + value > 255) 255
        else color + value
    }

    private fun pickImage() {
        val pickImageIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        activityResultLauncher.launch(pickImageIntent)
    }

    private fun redrawPicture(pictureToRedraw: Bitmap, brightnessChange: Int) {
        val height = pictureToRedraw.height
        val width = pictureToRedraw.width
        val editedImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//        val limitColorToRgbRange = { color: Int, value: Int -> (color + value).coerceIn(0..255) }
        @ColorInt val pixels = IntArray(height * width)

        //brightness = R+G+B/3, simply add or subtract the same amount from each RGB color.
        pictureToRedraw.getPixels(pixels, 0, width, 0, 0, width, height)
        pixels.indices.forEach {
//            val red = limitColorToRgbRange(Color.red(pixels[it]), brightnessChange)
//            val green = limitColorToRgbRange(Color.green(pixels[it]), brightnessChange)
//            val blue = limitColorToRgbRange(Color.blue(pixels[it]), brightnessChange)
            val red = limitToRgb(Color.red(pixels[it]), brightnessChange)
            val green = limitToRgb(Color.green(pixels[it]), brightnessChange)
            val blue = limitToRgb(Color.blue(pixels[it]), brightnessChange)
            pixels[it] = Color.rgb(red, green, blue)
        }
        editedImage.setPixels(pixels, 0, width, 0, 0, width, height)
        currentImage.setImageBitmap(editedImage)
    }

    // do not change this function
    fun createBitmap(): Bitmap {
        val width = 200
        val height = 100
        val pixels = IntArray(width * height)
        // get pixel array from source

        var R: Int
        var G: Int
        var B: Int
        var index: Int

        for (y in 0 until height) {
            for (x in 0 until width) {
                // get current index in 2D-matrix
                index = y * width + x
                // get color
                R = x % 100 + 40
                G = y % 100 + 80
                B = (x + y) % 100 + 120

                pixels[index] = Color.rgb(R, G, B)

            }
        }
        // output bitmap
        val bitmapOut = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        bitmapOut.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmapOut
    }
}