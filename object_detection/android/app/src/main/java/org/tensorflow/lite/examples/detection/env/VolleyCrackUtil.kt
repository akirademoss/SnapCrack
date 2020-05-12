package org.tensorflow.lite.examples.detection.env

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import org.tensorflow.lite.examples.detection.util.VolleyRequest.jsonObjectPostRequest
import java.io.ByteArrayOutputStream
import java.lang.Exception

private val TAG = "VolleyCrackUtil"

fun uploadImage(context: Context, bitmap: Bitmap, type: String, location: String) {
    val stream: ByteArrayOutputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    val imageData = stream.toByteArray()
    imageData ?: return
    val request = object : VolleyFileUploadRequest(
            Method.POST,
            "https://snapcrack.ngrok.io/upload",
            Response.Listener {
                Log.d(TAG, "Response is: ${it.data.toString()}")
                var fileName = ""
                for (item in it.data ) {
                    var temp = item.toChar()
                    fileName += temp.toString()
                }
                Log.d(TAG, fileName)
                try {
                    var json = JSONObject()
                    json.put("type", type)
                    json.put("imageName", fileName)
                    json.put("notes", "")
                    json.put("location", location)
                    jsonObjectPostRequest(json, "http://snapcrack.ngrok.io/crack", context)
                } catch (e: Exception) {
                    Log.e(TAG, e.message)
                }

            },
            Response.ErrorListener {
                if(it.networkResponse != null) {
                    Log.e(TAG, "Error is ${it.networkResponse.data.get(0)} with code ${it.networkResponse.statusCode}")
                }
            }
    ) {

        override fun getByteData(): MutableMap<String, FileDataPart> {
            var params = HashMap<String, FileDataPart>()
            params["imageFile"] = FileDataPart("myImage", imageData!!, "png")
            return params
        }
    }
    Volley.newRequestQueue(context).add(request)
}


fun getImage(context: Context): ByteArray? {
    val img = context.assets.open("bob.png") //TODO change to captured image
    val imgData = img.readBytes()
    return imgData
}