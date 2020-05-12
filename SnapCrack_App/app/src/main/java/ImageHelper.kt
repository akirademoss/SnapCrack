import android.content.Context
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import java.io.File
import java.util.concurrent.Executor

class ImageHelper (private val context: Context, private val imageCapture: ImageCapture, private val executor: Executor) {

    private val TAG = "ImageHelper"

    public fun takePicture2() {
//        var time = System.currentTimeMillis().toString()
//        val file = File(context.externalMediaDirs.first(), "${System.currentTimeMillis()}.jpg")
//        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
//        imageCapture.takePicture(outputFileOptions, executor, object : ImageCapture.OnImageSavedCallback {
//            override fun onError(exception: ImageCaptureException) {
//                Log.e(TAG, "Error capturing image", exception)
//            }
//
//            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
//                Log.d(TAG, "Image captured at time $time")
//                //Toast.makeText(applicationContext, "Image captured", Toast.LENGTH_SHORT).show()
//            }
//        })
        //TODO add back after timing issue is fixed
    }

}