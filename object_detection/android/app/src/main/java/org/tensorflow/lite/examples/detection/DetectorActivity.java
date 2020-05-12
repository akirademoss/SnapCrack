package org.tensorflow.lite.examples.detection;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.location.Location;
import android.media.ImageReader.OnImageAvailableListener;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.tflite.Classifier;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;
import org.tensorflow.lite.examples.detection.env.ImageUtils;

import static org.tensorflow.lite.examples.detection.env.VolleyCrackUtilKt.uploadImage;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {

  private static final String TAG = "DetectorActivity";

  // Configuration values for the prepackaged SSD model.
  private static final int TF_OD_API_INPUT_SIZE = 300;
  private static final boolean TF_OD_API_IS_QUANTIZED = false;
  private static final String TF_OD_API_MODEL_FILE = "model2.tflite";
  private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/model_labels.txt";
  private static final DetectorMode MODE = DetectorMode.TF_OD_API;
  // Minimum detection confidence to track a detection.
  private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
  private static final boolean MAINTAIN_ASPECT = false;
  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
  private static final boolean SAVE_PREVIEW_BITMAP = false;
  private static final float TEXT_SIZE_DIP = 10;
  OverlayView trackingOverlay;
  private Integer sensorOrientation;

  private Classifier detector;

  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;

  private boolean computingDetection = false;

  private long timestamp = 0;

  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;

  private MultiBoxTracker tracker;

  private FusedLocationProviderClient fusedLocationProvider;


  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {

    tracker = new MultiBoxTracker(this);
    fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this);

    int cropSize = TF_OD_API_INPUT_SIZE;

    try {
      detector =
          TFLiteObjectDetectionAPIModel.create(
              getAssets(),
              TF_OD_API_MODEL_FILE,
              TF_OD_API_LABELS_FILE,
              TF_OD_API_INPUT_SIZE,
              TF_OD_API_IS_QUANTIZED);
      cropSize = TF_OD_API_INPUT_SIZE;
    } catch (final IOException e) {
      e.printStackTrace();
      Log.e(TAG, "Exception initializing classifier!", e);
      Toast toast =
          Toast.makeText(
              getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
      toast.show();
      finish();
    }

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();
    Log.i(TAG, "Camera orientation relative to screen canvas: " + sensorOrientation);

    Log.i(TAG, "Initializing at size " + previewWidth + "x" + previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

    frameToCropTransform =
        ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            cropSize, cropSize,
            sensorOrientation, MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);

    trackingOverlay = findViewById(R.id.tracking_overlay); //overlay with bounding boxes
    trackingOverlay.addCallback(
            canvas -> {
              tracker.draw(canvas);
              if (isDebug()) {
                tracker.drawDebug(canvas);
              }
            });

    tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
  }

  @Override
  protected void processImage() {
    ++timestamp;
    final long currTimestamp = timestamp;
    trackingOverlay.postInvalidate();

    // No mutex needed as this method is not reentrant.
    if (computingDetection) {
      readyForNextImage();
      return;
    }
    computingDetection = true;
    //Log.i(TAG, "Preparing image " + currTimestamp + " for detection in bg thread.");

    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

    readyForNextImage();

    final Canvas canvas = new Canvas(croppedBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) {
      ImageUtils.saveBitmap(croppedBitmap, getApplicationContext());
    }

    runInBackground(
            () -> {
              //Log.i(TAG, "Running detection on image " + currTimestamp);
              final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);

              cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
              final Canvas canvas1 = new Canvas(cropCopyBitmap);
              final Paint paint = new Paint();
              paint.setColor(Color.RED);
              paint.setStyle(Style.STROKE);
              paint.setStrokeWidth(2.0f);

              float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
              switch (MODE) {
                case TF_OD_API:
                  minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                  break;
              }

              final List<Classifier.Recognition> mappedRecognitions =
                  new LinkedList<Classifier.Recognition>();

              for (final Classifier.Recognition result : results) {
                final RectF location = result.getLocation();
                if (location != null && result.getConfidence() >= minimumConfidence) { //detected crack/pothole
                  //ImageUtils.saveBitmap(croppedBitmap, getApplicationContext());
                  canvas1.drawRect(location, paint);
                  cropToFrameTransform.mapRect(location);

                  result.setLocation(location);
                  mappedRecognitions.add(result);

                    final Location[] gps = new Location[1];
                    fusedLocationProvider.getLastLocation().addOnSuccessListener(this, location1 -> {
                        if(location1 != null) {
                            gps[0] = location1;
                            Log.d(TAG, result.getTitle());
                            Log.d(TAG, "Location was latitude " + gps[0].getLatitude() + " longitude " + gps[0].getLongitude());
                            //ImageUtils.saveBitmap(croppedBitmap, getApplicationContext());
                            uploadImage(getApplicationContext(), croppedBitmap, result.getTitle(), formatLocation(gps));
                        } else {
                            Log.w(TAG, "Location was null");
                        }
                    });

                }
              }

              tracker.trackResults(mappedRecognitions, currTimestamp);
              trackingOverlay.postInvalidate();

              //get screenshot with bounding box
              View screen = getWindow().getDecorView().getRootView();
              screen.setDrawingCacheEnabled(true);
              computingDetection = false;
            });
  }

  String formatLocation(Location[] gps) {
      return "[" + gps[0].getLatitude() + ", " + gps[0].getLongitude() + "]";
  }

  @Override
  protected int getLayoutId() {
    return R.layout.tfe_od_camera_connection_fragment_tracking;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  @Override
  public void onClick(View view) {

  }

  @Override
  public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

  }

  // Which detection model to use: by default uses Tensorflow Object Detection API frozen
  // checkpoints.
  private enum DetectorMode {
    TF_OD_API;
  }

}
