package org.tensorflow.lite.examples.detection.env;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Utility class for manipulating images. */
public class ImageUtils {
  // This value is 2 ^ 18 - 1, and is used to clamp the RGB values before their ranges
  // are normalized to eight bits.
  static final int kMaxChannelValue = 262143;
  private static final String TAG = "ImageUtils";

  /**
   * Utility method to compute the allocated size in bytes of a YUV420SP image of the given
   * dimensions.
   */
  public static int getYUVByteSize(final int width, final int height) {
    // The luminance plane requires 1 byte per pixel.
    final int ySize = width * height;

    // The UV plane works on 2x2 blocks, so dimensions with odd size must be rounded up.
    // Each 2x2 block takes 2 bytes to encode, one each for U and V.
    final int uvSize = ((width + 1) / 2) * ((height + 1) / 2) * 2;

    return ySize + uvSize;
  }

  /**
   * Saves a Bitmap object to disk for analysis.
   *
   * @param bitmap The bitmap to save.
   */
  public static void saveBitmap(final Bitmap bitmap, Context context) {
    saveBitmap(bitmap, System.currentTimeMillis() + ".png", context);
  }

  /**
   * Saves a Bitmap object to disk for analysis.
   *
   * @param bitmap The bitmap to save.
   * @param filename The location to save the bitmap to.
   */
  public static void saveBitmap(final Bitmap bitmap, final String filename, Context context) {
    File pictureFile = getOutputMediaFile(context);
    if (pictureFile == null) {
      Log.d(TAG,
              "Error creating media file, check storage permissions: ");
      return;
    }
    try {
      FileOutputStream fos = new FileOutputStream(pictureFile);
      bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
      fos.close();
      Toast.makeText(context, "Image saved to" + pictureFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
    } catch (FileNotFoundException e) {
      Log.d(TAG, "File not found: " + e.getMessage());
    } catch (IOException e) {
      Log.d(TAG, "Error accessing file: " + e.getMessage());
    }
  }

  /** Create a File for saving an image or video */
  private static File getOutputMediaFile(Context context){
    // To be safe, you should check that the SDCard is mounted
    // using Environment.getExternalStorageState() before doing this.
    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            + context.getApplicationContext().getPackageName()
            + "/Files");

    // This location works best if you want the created images to be shared
    // between applications and persist after your app has been uninstalled.

    // Create the storage directory if it does not exist
    if (! mediaStorageDir.exists()){
      if (! mediaStorageDir.mkdirs()){
        return null;
      }
    }

    File test = Environment.getRootDirectory();
    // Create a media file name
    String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
    File mediaFile;
    String mImageName= timeStamp+ System.currentTimeMillis() +".png";
    mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
    return mediaFile;
  }

  public static void convertYUV420SPToARGB8888(byte[] input, int width, int height, int[] output) {
    final int frameSize = width * height;
    for (int j = 0, yp = 0; j < height; j++) {
      int uvp = frameSize + (j >> 1) * width;
      int u = 0;
      int v = 0;

      for (int i = 0; i < width; i++, yp++) {
        int y = 0xff & input[yp];
        if ((i & 1) == 0) {
          v = 0xff & input[uvp++];
          u = 0xff & input[uvp++];
        }

        output[yp] = YUV2RGB(y, u, v);
      }
    }
  }

  private static int YUV2RGB(int y, int u, int v) {
    // Adjust and check YUV values
    y = (y - 16) < 0 ? 0 : (y - 16);
    u -= 128;
    v -= 128;

    // This is the floating point equivalent. We do the conversion in integer
    // because some Android devices do not have floating point in hardware.
    // nR = (int)(1.164 * nY + 2.018 * nU);
    // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
    // nB = (int)(1.164 * nY + 1.596 * nV);
    int y1192 = 1192 * y;
    int r = (y1192 + 1634 * v);
    int g = (y1192 - 833 * v - 400 * u);
    int b = (y1192 + 2066 * u);

    // Clipping RGB values to be inside boundaries [ 0 , kMaxChannelValue ]
    r = r > kMaxChannelValue ? kMaxChannelValue : (r < 0 ? 0 : r);
    g = g > kMaxChannelValue ? kMaxChannelValue : (g < 0 ? 0 : g);
    b = b > kMaxChannelValue ? kMaxChannelValue : (b < 0 ? 0 : b);

    return 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
  }

  public static void convertYUV420ToARGB8888(
      byte[] yData,
      byte[] uData,
      byte[] vData,
      int width,
      int height,
      int yRowStride,
      int uvRowStride,
      int uvPixelStride,
      int[] out) {
    int yp = 0;
    for (int j = 0; j < height; j++) {
      int pY = yRowStride * j;
      int pUV = uvRowStride * (j >> 1);

      for (int i = 0; i < width; i++) {
        int uv_offset = pUV + (i >> 1) * uvPixelStride;

        out[yp++] = YUV2RGB(0xff & yData[pY + i], 0xff & uData[uv_offset], 0xff & vData[uv_offset]);
      }
    }
  }

  /**
   * Returns a transformation matrix from one reference frame into another. Handles cropping (if
   * maintaining aspect ratio is desired) and rotation.
   *
   * @param srcWidth Width of source frame.
   * @param srcHeight Height of source frame.
   * @param dstWidth Width of destination frame.
   * @param dstHeight Height of destination frame.
   * @param applyRotation Amount of rotation to apply from one frame to another. Must be a multiple
   *     of 90.
   * @param maintainAspectRatio If true, will ensure that scaling in x and y remains constant,
   *     cropping the image if necessary.
   * @return The transformation fulfilling the desired requirements.
   */
  public static Matrix getTransformationMatrix(
      final int srcWidth,
      final int srcHeight,
      final int dstWidth,
      final int dstHeight,
      final int applyRotation,
      final boolean maintainAspectRatio) {
    final Matrix matrix = new Matrix();

    if (applyRotation != 0) {
      if (applyRotation % 90 != 0) {
        Log.w(TAG, "Rotation of " + applyRotation + " % 90 != 0");
      }

      // Translate so center of image is at origin.
      matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

      // Rotate around origin.
      matrix.postRotate(applyRotation);
    }

    // Account for the already applied rotation, if any, and then determine how
    // much scaling is needed for each axis.
    final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

    final int inWidth = transpose ? srcHeight : srcWidth;
    final int inHeight = transpose ? srcWidth : srcHeight;

    // Apply scaling if necessary.
    if (inWidth != dstWidth || inHeight != dstHeight) {
      final float scaleFactorX = dstWidth / (float) inWidth;
      final float scaleFactorY = dstHeight / (float) inHeight;

      if (maintainAspectRatio) {
        // Scale by minimum factor so that dst is filled completely while
        // maintaining the aspect ratio. Some image may fall off the edge.
        final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
        matrix.postScale(scaleFactor, scaleFactor);
      } else {
        // Scale exactly to fill dst from src.
        matrix.postScale(scaleFactorX, scaleFactorY);
      }
    }

    if (applyRotation != 0) {
      // Translate back from origin centered reference to destination frame.
      matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
    }

    return matrix;
  }
}
