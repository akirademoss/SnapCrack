package edu.iastate.snapcrack

/**
 * CameraX implementation done with use of the Google Codelab
 * https://codelabs.developers.google.com/codelabs/camerax-getting-started/#0
 */

import ImageHelper
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.view.Menu
import android.view.MenuItem
import android.view.Surface
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import edu.iastate.snapcrack.ui.SettingsFragment
import edu.iastate.snapcrack.util.ImageUtils
import edu.iastate.snapcrack.util.MultiBoxTracker
import edu.iastate.snapcrack.util.OverlayView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors


// This is an arbitrary number we are using to keep track of the permission
// request. Where an app has multiple context for requesting permission,
// this can help differentiate the different contexts.
internal const val REQUEST_CODE_PERMISSIONS = 10

// This is an array of all the permission specified in the manifest.
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

typealias ModelListener = (model: Double) -> Unit

class MainActivity : AppCompatActivity(), LifecycleOwner {

    private var detector: Classifier? = null

    private var lastProcessingTimeMs: Long = 0
    private val rgbFrameBitmap: Bitmap? = null
    private var cropCopyBitmap: Bitmap? = null

    private var computingDetection = false

    private var timestamp: Long = 0

    private val frameToCropTransform: Matrix? = null
    private val cropToFrameTransform: Matrix? = null

    private lateinit var tracker: MultiBoxTracker
    private lateinit var trackingOverlay: OverlayView
    private lateinit var imageHelper: ImageHelper

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var viewFinder: PreviewView
    private var cameraRunning: Boolean = false
    private var takingPicture: Boolean = false;
    private val TAG = "MainActivity"
    private var cameraSpeed = 1000

    /** From tutorial **/
    private var tfLiteClassifier: TFLiteClassifier = TFLiteClassifier((this@MainActivity))

    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalyzer: ImageAnalysis
    private var camera: Camera? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.preview_view)

        tracker = MultiBoxTracker(this)
        trackingOverlay = findViewById(R.id.tracking_overlay)
        trackingOverlay.addCallback(OverlayView.DrawCallback {
            tracker.draw(it)
        })

        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

        val toolbar: Toolbar = findViewById(R.id.my_toolbar)
        setSupportActionBar(toolbar)
        toolbar.showOverflowMenu()

        tfLiteClassifier
            .initialize()
            .addOnSuccessListener { }
            .addOnFailureListener { e -> Log.e(TAG, "Error in setting up the classifier.", e) }


        try {
            detector = TFLiteObjectDetectionAPIModel.create(
                assets,
                TF_OD_API_MODEL_FILE,
                TF_OD_API_LABELS_FILE,
                TF_OD_API_INPUT_SIZE,
                TF_OD_API_IS_QUANTIZED
            )
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("Issue initializing classifier", e.toString())
            val toast = Toast.makeText(
                applicationContext, "Classifier could not be initialized", Toast.LENGTH_SHORT
            )
            toast.show()
            finish()
        }


        val previewView = findViewById<PreviewView>(R.id.preview_view)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            // Camera provider is now guaranteed to be available
            val cameraProvider = cameraProviderFuture.get()

            // Set up the preview use case to display camera preview.
            val preview = Preview.Builder()
                .build()

            // Set up the capture use case to allow users to take photos.
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            imageHelper = ImageHelper(this, imageCapture, executor)

            // Set up analyzer use case so we can make inferences
            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(
                        executor,
                        ClassifierAnalyzer(
                            tfLiteClassifier,
                            predictedTextView,
                            detector!!,
                            trackingOverlay,
                            tracker,
                            imageHelper
                        ))
                }

            // Choose the camera by requiring a lens facing
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            // Attach use cases to the camera with the same lifecycle owner
            try {
                val camera = cameraProvider.bindToLifecycle(
                    this as LifecycleOwner, cameraSelector, preview, imageCapture, imageAnalyzer
                )
            } catch(e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }

            // Connect the preview use case to the previewView
            preview.setSurfaceProvider(
                previewView.createSurfaceProvider(camera?.cameraInfo)
            )
        }, ContextCompat.getMainExecutor(this))

        capture_button.setOnClickListener{ view ->
            takingPicture = true;
            takePicture()
        }
    }

    override fun onDestroy() {
        tfLiteClassifier.close()

        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_sign_out -> signOut()
        }
        return true
    }

    private fun takePicture2() {
        var time = System.currentTimeMillis().toString()
        val file = File(this.externalMediaDirs.first(), "${System.currentTimeMillis()}.jpg")
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(outputFileOptions, executor, object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Error capturing image", exception)
            }

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                Log.d(TAG, "Image captured at time $time")
                //Toast.makeText(applicationContext, "Image captured", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when(viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)
    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera(){}




    private fun ImageProxy.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer // Y
        val uBuffer = planes[1].buffer // U
        val vBuffer = planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun takePicture() {
        var startTime = System.currentTimeMillis()
        val file = File(externalMediaDirs.first(), "${System.currentTimeMillis()}.jpg")
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(outputFileOptions, executor, object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Error capturing image", exception)
            }

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val endTime = System.currentTimeMillis();
                Log.d(TAG, "Image captured at time $startTime, time to save was ${endTime - startTime}")
                //Toast.makeText(applicationContext, "Image captured", Toast.LENGTH_SHORT).show()
                takingPicture = false;
            }
        })
    }

    fun updateImageSpeed(num: Int) {
        cameraSpeed = num * 1000
    }


    fun onSettingsClicked(item: MenuItem) {
        Log.d(TAG, "settings clicked")
        supportFragmentManager.beginTransaction()
            .add(R.id.main_container, SettingsFragment.newInstance(), "settings")
            .addToBackStack(null)
            .commit()
    }

    private fun signOut() {
        val sharedPreferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", false).apply()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    private class ClassifierAnalyzer(private val tfLiteClassifier: TFLiteClassifier,
                                     private val predictedTextView: TextView,
                                     private val detector: Classifier,
                                     private val trackingOverlay: OverlayView,
                                     private val tracker: MultiBoxTracker,
                                     private val imageHelper: ImageHelper
    ) : ImageAnalysis.Analyzer {
        private val TAG = "MainActivity.ClassifierAnalyzer"
        lateinit var cropToFrameTransform: Matrix
        lateinit var rgbFrameBitmap: Bitmap
        lateinit var cropCopyBitmap: Bitmap
        lateinit var croppedBitmap: Bitmap
        lateinit var frameToCropTransform: Matrix
        val cropSize = 300

        @SuppressLint("UnsafeExperimentalUsageError")
        override fun analyze(image: ImageProxy) {
            val bitmap = image.image?.toBitmap()
            trackingOverlay.postInvalidate()


//            tfLiteClassifier
//                .classifyAsync(bitmap!!)
//                .addOnSuccessListener { resultText -> predictedTextView.text = resultText }
//                .addOnFailureListener { error -> Log.e(TAG, "Error occurred during classification", error)}

                val previewWidth = 640;
                val previewHeight = 480;
                rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
                croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)
                //rgbFrameBitmap.setPixels(ImageUtils.getRgbBytes(), 0, previewWidth, 0, 0, previewHeight)

                val sensorOrientation = 0

                cropToFrameTransform = Matrix()
                frameToCropTransform = ImageUtils.getTransformationMatrix(
                    previewWidth,
                    previewHeight,
                    cropSize,
                    cropSize,
                    sensorOrientation,
                    MAINTAIN_ASPECT
                )
                frameToCropTransform.invert(cropToFrameTransform)
                val results = detector.recognizeImage(croppedBitmap)

                cropCopyBitmap = Bitmap.createBitmap(croppedBitmap)
                val canvas = Canvas(cropCopyBitmap)
                //canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null)
                val paint = Paint()
                paint.color = Color.RED
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2.0f
                val minimumConfidence: Float =
                    MINIMUM_CONFIDENCE_TF_OD_API

                val mappedRecognitions: MutableList<Classifier.Recognition> =
                    LinkedList<Classifier.Recognition>()

                for (result: Classifier.Recognition in results) {
                    //Log.d(TAG, "Result: ${result.title}, Confidence: ${result.confidence}")
                    val location = result.location
                    if (location != null && result.confidence >= minimumConfidence) {
                        Log.w(TAG, "Minimum confidence reached, ${result.title} detected")
                        canvas.drawRect(location, paint)
                        cropToFrameTransform.mapRect(location)
                        result.setLocation(location)
                        mappedRecognitions.add(result)
                        imageHelper.takePicture2()
                    }
                }
                tracker.trackResults(mappedRecognitions, SystemClock.currentThreadTimeMillis())
                trackingOverlay.postInvalidate()
            image.close()
        }

        fun Image.toBitmap(): Bitmap {
            val yBuffer = planes[0].buffer // Y
            val uBuffer = planes[1].buffer // U
            val vBuffer = planes[2].buffer // V

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            //U and V are swapped
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
            val imageBytes = out.toByteArray()
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }
    }

    companion object {
        // Configuration values for the prepackaged SSD model.
        const val TF_OD_API_INPUT_SIZE = 300
        const val TF_OD_API_IS_QUANTIZED = false
        const val TF_OD_API_MODEL_FILE = "model.tflite"
        const val TF_OD_API_LABELS_FILE = "file:///android_asset/model_labels.txt"
        // Minimum detection confidence to track a detection.
        const val MINIMUM_CONFIDENCE_TF_OD_API = 0.1f
        const val MAINTAIN_ASPECT = false
        val DESIRED_PREVIEW_SIZE = Size(640, 480)
        const val SAVE_PREVIEW_BITMAP = false
        const val TEXT_SIZE_DIP = 10f
        val sensorOrientation: Int? = null
    }

}