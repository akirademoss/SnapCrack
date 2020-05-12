package org.tensorflow.lite.examples.detection;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Toast;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import org.tensorflow.lite.examples.detection.env.ImageUtils;

public abstract class CameraActivity extends AppCompatActivity
    implements OnImageAvailableListener,
        Camera.PreviewCallback,
        CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {

  private static final String TAG = "CameraActivity";
  private static final int PERMISSIONS_REQUEST = 1;

  private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
  private static final String PERMISSION_STORAGE_WRITE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
  private static final String PERMISSION_STORAGE_READ = Manifest.permission.READ_EXTERNAL_STORAGE;
  private static final String PERMISSION_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
  private static final String PERMISSION_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
  protected int previewWidth = 0;
  protected int previewHeight = 0;
  private boolean debug = false;
  private Handler handler;
  private HandlerThread handlerThread;
  private boolean useCamera2API;
  private boolean isProcessingFrame = false;
  private byte[][] yuvBytes = new byte[3][];
  private int[] rgbBytes = null;
  private int yRowStride;
  private Runnable postInferenceCallback;
  private Runnable imageConverter;

  private Button stopButton;


  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    Log.d(TAG, "onCreate" + this);
    super.onCreate(null);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    setContentView(R.layout.activity_camera);

    stopButton = findViewById(R.id.capture_button);
    stopButton.setOnClickListener(view -> onStopClicked());

    if (hasPermission()) {
      setFragment();
    } else {
      requestPermission();
    }

  }

  private void onStopClicked() {
    Intent intent = new Intent(this, MainActivity.class);
    startActivity(intent);
  }

  protected int[] getRgbBytes() {
    imageConverter.run();
    return rgbBytes;
  }

  /**
   * Check if device is connected with a valid internet connection
   * @return true if connected, false otherwise
   */
  private boolean isNetworkConnected() {
    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

    if(connectivityManager.getActiveNetworkInfo() != null &&
    connectivityManager.getActiveNetworkInfo().isConnected()) { //check if connected
      try { //check if internet is available
        InetAddress ipAddress = InetAddress.getByName("google.com");
        return !ipAddress.equals("");
      } catch (Exception e) {
        Log.e(TAG, "No internet connectivity detected", e);
        return false;
      }
    }

    return false;
  }

  /** Callback for android.hardware.Camera API */
  @Override
  public void onPreviewFrame(final byte[] bytes, final Camera camera) {
    if (isProcessingFrame) {
      Log.w(TAG, "Dropping frame!");
      return;
    }

    try {
      // Initialize the storage bitmaps once when the resolution is known.
      if (rgbBytes == null) {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        previewHeight = previewSize.height;
        previewWidth = previewSize.width;
        rgbBytes = new int[previewWidth * previewHeight];
        onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
      }
    } catch (final Exception e) {
      Log.e(TAG, "Exception!", e);
      return;
    }

    isProcessingFrame = true;
    yuvBytes[0] = bytes;
    yRowStride = previewWidth;

    imageConverter =
            () -> ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);

    postInferenceCallback =
            () -> {
              camera.addCallbackBuffer(bytes);
              isProcessingFrame = false;
            };
    processImage();
  }

  /** Callback for Camera2 API */
  @Override
  public void onImageAvailable(final ImageReader reader) {
    // We need wait until we have some size from onPreviewSizeChosen
    if (previewWidth == 0 || previewHeight == 0) {
      return;
    }
    if (rgbBytes == null) {
      rgbBytes = new int[previewWidth * previewHeight];
    }
    try {
      final Image image = reader.acquireLatestImage();

      if (image == null) {
        return;
      }

      if (isProcessingFrame) {
        image.close();
        return;
      }
      isProcessingFrame = true;
      Trace.beginSection("imageAvailable");
      final Plane[] planes = image.getPlanes();
      fillBytes(planes, yuvBytes);
      yRowStride = planes[0].getRowStride();
      final int uvRowStride = planes[1].getRowStride();
      final int uvPixelStride = planes[1].getPixelStride();

      imageConverter =
              () -> ImageUtils.convertYUV420ToARGB8888(
                  yuvBytes[0],
                  yuvBytes[1],
                  yuvBytes[2],
                  previewWidth,
                  previewHeight,
                  yRowStride,
                  uvRowStride,
                  uvPixelStride,
                  rgbBytes);

      postInferenceCallback =
              () -> {
                image.close();
                isProcessingFrame = false;
              };
      processImage();
    } catch (final Exception e) {
      Log.e(TAG, "Exception!", e);
      Trace.endSection();
      return;
    }
    Trace.endSection();
  }

  @Override
  public synchronized void onStart() {
    Log.d(TAG, "onStart" + this);
    super.onStart();
  }

  @Override
  public synchronized void onResume() {
    Log.d(TAG, "onResume" + this);
    super.onResume();
    resumeInference();
  }

  void resumeInference() {
    handlerThread = new HandlerThread("inference");
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
  }

  @Override
  public synchronized void onPause() {
    Log.d(TAG, "onPause" + this);
    pauseInference();
    super.onPause();
  }

  void pauseInference() {
    handlerThread.quitSafely();
    try {
      handlerThread.join();
      handlerThread = null;
      handler = null;
    } catch (final InterruptedException e) {
      Log.e(TAG, "Exception!", e);
    }
  }

  @Override
  public synchronized void onStop() {
    Log.d(TAG, "onStop" + this);
    super.onStop();
  }

  @Override
  public synchronized void onDestroy() {
    Log.d(TAG, "onDestroy" + this);
    super.onDestroy();
  }

  protected synchronized void runInBackground(final Runnable r) {
    if (handler != null) {
      handler.post(r);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      final int requestCode, final String[] permissions, final int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == PERMISSIONS_REQUEST) {
      if (allPermissionsGranted(grantResults)) {
        setFragment();
      } else {
        requestPermission();
      }
    }
  }

  private static boolean allPermissionsGranted(final int[] grantResults) {
    for (int result : grantResults) {
      if (result != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  private boolean hasPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED &&
              checkSelfPermission(PERMISSION_STORAGE_WRITE) == PackageManager.PERMISSION_GRANTED &&
              checkSelfPermission(PERMISSION_STORAGE_READ) == PackageManager.PERMISSION_GRANTED;
//      return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
    } else {
      return true;
    }
  }

  private void requestPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
        Toast.makeText(
                CameraActivity.this,
                "Camera permission is required for this demo",
                Toast.LENGTH_LONG)
            .show();
      }
      if (shouldShowRequestPermissionRationale(PERMISSION_STORAGE_WRITE)) {
        Toast.makeText(
                CameraActivity.this,
                "External write permission required to save captured images",
                Toast.LENGTH_LONG
        ).show();
      }
      if (shouldShowRequestPermissionRationale(PERMISSION_STORAGE_READ)) {
        Toast.makeText(
                CameraActivity.this,
                "External storage read permission required to retrieve images to send to server",
                Toast.LENGTH_LONG
        ).show();
      }
      if (shouldShowRequestPermissionRationale(PERMISSION_COARSE_LOCATION)) {
          Toast.makeText(
                  this,
                  "Location required to save location of detections",
                  Toast.LENGTH_SHORT
          ).show();
      }
        if (shouldShowRequestPermissionRationale(PERMISSION_FINE_LOCATION)) {
            Toast.makeText(
                    this,
                    "Location required to save location of detections",
                    Toast.LENGTH_SHORT
            ).show();
        }
      requestPermissions(new String[] {PERMISSION_CAMERA, PERMISSION_STORAGE_READ, PERMISSION_STORAGE_WRITE, PERMISSION_COARSE_LOCATION, PERMISSION_FINE_LOCATION}, PERMISSIONS_REQUEST);
    }
  }

  // Returns true if the device supports the required hardware level, or better.
  private boolean isHardwareLevelSupported(
      CameraCharacteristics characteristics, int requiredLevel) {
    int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
    if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
      return requiredLevel == deviceLevel;
    }
    // deviceLevel is not LEGACY, can use numerical sort
    return requiredLevel <= deviceLevel;
  }

  private String chooseCamera() {
    final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    try {
      for (final String cameraId : manager.getCameraIdList()) {
        final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

        // We don't use a front facing camera in this sample.
        final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
          continue;
        }

        final StreamConfigurationMap map =
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map == null) {
          continue;
        }

        // Fallback to camera1 API for internal cameras that don't have full support.
        // This should help with legacy situations where using the camera2 API causes
        // distorted or otherwise broken previews.
        useCamera2API =
            (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                || isHardwareLevelSupported(
                    characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        Log.i(TAG, "Camera API 1v2?: " + useCamera2API);
        return cameraId;
      }
    } catch (CameraAccessException e) {
      Log.e(TAG, "Not allowed access to camera", e);
    }

    return null;
  }

  protected void setFragment() {
    String cameraId = chooseCamera();

    Fragment fragment;
    if (useCamera2API) {
      CameraConnectionFragment camera2Fragment =
          CameraConnectionFragment.newInstance(
                  (size, rotation) -> {
                    previewHeight = size.getHeight();
                    previewWidth = size.getWidth();
                    CameraActivity.this.onPreviewSizeChosen(size, rotation);
                  },
              this,
              getLayoutId(),
              getDesiredPreviewFrameSize());

      camera2Fragment.setCamera(cameraId);
      fragment = camera2Fragment;
    } else {
      fragment =
          new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
    }

    getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
  }

  protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
    // Because of the variable row stride it's not possible to know in
    // advance the actual necessary dimensions of the yuv planes.
    for (int i = 0; i < planes.length; ++i) {
      final ByteBuffer buffer = planes[i].getBuffer();
      if (yuvBytes[i] == null) {
        Log.d(TAG, "Initializing buffer " + i + " at size " + buffer.capacity());
        yuvBytes[i] = new byte[buffer.capacity()];
      }
      buffer.get(yuvBytes[i]);
    }
  }

  public boolean isDebug() {
    return debug;
  }

  protected void readyForNextImage() {
    if (postInferenceCallback != null) {
      postInferenceCallback.run();
    }
  }

  protected int getScreenOrientation() {
    switch (getWindowManager().getDefaultDisplay().getRotation()) {
      case Surface.ROTATION_270:
        return 270;
      case Surface.ROTATION_180:
        return 180;
      case Surface.ROTATION_90:
        return 90;
      default:
        return 0;
    }
  }

  protected abstract void processImage();

  protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

  protected abstract int getLayoutId();

  protected abstract Size getDesiredPreviewFrameSize();

}
