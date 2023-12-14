package com.eriks.photoeditor

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.material.slider.Slider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private var lastJob: Job? = null

    private lateinit var currentImage: ImageView
    private lateinit var pickImageBtn: Button
    private lateinit var saveImageBtn: Button
    private lateinit var brightnessSl: Slider
    private lateinit var contrastSl: Slider
    private lateinit var saturationSl: Slider
    private lateinit var gammaSl: Slider
    private lateinit var currentBitmap: Bitmap
    private lateinit var imageProcessor: ImageProcessor
    private var slidersState = SlidersState()

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let {
                    val bitmap = imageProcessor.createBitmap(it)
                    currentImage.setImageBitmap(bitmap)
                }
            }
        }

    private val cameraRequestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
            ::onRequestCameraPermissionResult
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()

        //do not change this line
        currentBitmap = createBitmap()
        currentImage.setImageBitmap(currentBitmap)
        imageProcessor = ImageProcessor(this, currentBitmap)

        pickImageBtn.setOnClickListener {
            pickImage()
        }

        saveImageBtn.setOnClickListener {
            saveImage()
        }

        brightnessSl.addOnChangeListener { brightnessSl, value, _ ->
            onSliderChanges(brightnessSl, value)
        }

        contrastSl.addOnChangeListener { contrastSl, value, _ ->
            onSliderChanges(contrastSl, value)
        }

        saturationSl.addOnChangeListener { saturationSl, value, _ ->
            onSliderChanges(saturationSl, value)
        }

        gammaSl.addOnChangeListener { gammaSl, value, _ ->
            onSliderChanges(gammaSl, value)
        }
    }

    private fun onSliderChanges(slider: Slider, sliderValue: Float) {
        when (slider.id) {
            R.id.slBrightness -> slidersState.brightness = sliderValue.toInt()
            R.id.slContrast -> slidersState.contrast = sliderValue.toInt()
            R.id.slSaturation -> slidersState.saturation = sliderValue.toInt()
            R.id.slGamma -> slidersState.gamma = sliderValue.toDouble()
        }
        lastJob?.cancel()
        lastJob = CoroutineScope(Dispatchers.Default).launch {
            currentImage.drawable ?: return@launch
            val image = this.async { imageProcessor.recalculate(slidersState) }
            withContext(Dispatchers.Main) {
                currentImage.setImageBitmap(image.await())
            }
        }
    }

    private fun bindViews() {
        currentImage = findViewById(R.id.ivPhoto)
        pickImageBtn = findViewById(R.id.btnGallery)
        saveImageBtn = findViewById(R.id.btnSave)
        brightnessSl = findViewById(R.id.slBrightness)
        contrastSl = findViewById(R.id.slContrast)
        saturationSl = findViewById(R.id.slSaturation)
        gammaSl = findViewById(R.id.slGamma)
    }

    private fun pickImage() {
        val pickImageIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        activityResultLauncher.launch(pickImageIntent)
    }

    private fun onRequestCameraPermissionResult(granted: Boolean) {
        if (granted) {
            saveToStorage()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Permission required")
                .setMessage("This app needs permission to access this feature.")
        }
    }

    private fun saveImage() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED -> {
                saveToStorage()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) -> {
                AlertDialog.Builder(this)
                    .setTitle("Permission required")
                    .setMessage("This app needs media permission to save files to storage.")
                    .setPositiveButton("Allow") { _, _ ->
                        cameraRequestPermissionLauncher
                            .launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            else -> {
                cameraRequestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }
    }
    private fun saveToStorage() {
        val bitmap: Bitmap = currentImage.drawable.toBitmap()
        val timestamp: Long = System.currentTimeMillis()
        val values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_$timestamp.jpg")
        values.put(MediaStore.Images.Media.DATE_TAKEN, timestamp)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        values.put(MediaStore.Images.ImageColumns.WIDTH, bitmap.width)
        values.put(MediaStore.Images.ImageColumns.HEIGHT, bitmap.height)

        val uri = this@MainActivity.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        ) ?: return

        contentResolver.openOutputStream(uri).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
        Toast.makeText(this, "IMG_$timestamp.jpg saved to device storage", Toast.LENGTH_SHORT)
            .show()
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