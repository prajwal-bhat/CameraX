package com.apps.camerax.activity;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.Image;
import android.media.MediaActionSound;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.apps.camerax.R;
import com.apps.camerax.utils.FileUtil;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity{
    private static final String TAG = "MainActivity";

    private static final int REQUEST_CODE_PERMISSIONS = 10;

    private static String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"};


    private LinearLayout mainLayout;
    private FrameLayout cameraFrameLayout;
    private PreviewView previewView;
    private TextView zoomLevel;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private CameraSelector cameraSelector;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private Preview preview;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private Button flashButton;
    private Button toggleAutoHideBtn;
    private ImageAnalysis imageAnalyzer;
    private CameraControl cameraControl;
    private SeekBar zoomSlider;
    private ScaleGestureDetector scaleDetector;
    private ScaleGestureDetector.OnScaleGestureListener listener;


    private static final double RATIO_4_3_VALUE = 4.0 / 3.0;
    private static final double RATIO_16_9_VALUE = 16.0 / 9.0;


    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private int flashMode = ImageCapture.FLASH_MODE_OFF;

    //Camera Shutter Sound
    private MediaActionSound sound = new MediaActionSound();

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==REQUEST_CODE_PERMISSIONS){
            if(allPermissionsGranted()){
                runOnUiThread(this::setUpCamera);
            }else{
                Toast.makeText(this, "Permissions not granted by the user", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted() {
        for(String permission: REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission)!= PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        previewView = findViewById(R.id.previewView);
        mainLayout = findViewById(R.id.activity_main);
        flashButton= findViewById(R.id.flashLightBtn);
        cameraFrameLayout = findViewById(R.id.frameLayout);
        zoomLevel = new TextView(this);
        zoomLevel.setGravity(Gravity.CENTER);
        zoomLevel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f);
        zoomLevel.setVisibility(View.GONE);
        cameraFrameLayout.addView(zoomLevel);
//        zoomSlider = new SeekBar(this);
//        zoomSlider.setProgress(0);
//        zoomSlider.setMax(100);
//        zoomSlider.setProgressTintList(ColorStateList.valueOf(Color.RED));
//        cameraFrameLayout.addView(zoomSlider);
        if(allPermissionsGranted()){
            runOnUiThread(this::setUpCamera);
        }else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }



    private void setUpCamera(){
        cameraExecutor = Executors.newSingleThreadExecutor();
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCase();
                setOnClickListener();
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCase() {
        int screenAspectRatio = getAspectRatio(previewView.getWidth(), previewView.getHeight());
        int rotation = previewView.getDisplay().getRotation();
        preview = new Preview.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build();
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build();
//        OrientationEventListener orientationEventListener = new OrientationEventListener(this) {
//            @Override
//            public void onOrientationChanged(int orientation) {
//                int rotation;
//                if (orientation >= 45 && orientation < 135) {
//                    rotation = Surface.ROTATION_270;
//                } else if (orientation >= 135 && orientation < 225) {
//                    rotation = Surface.ROTATION_180;
//                } else if (orientation >= 225 && orientation < 315) {
//                    rotation = Surface.ROTATION_90;
//                } else {
//                    rotation = Surface.ROTATION_0;
//                }
//                imageCapture.setTargetRotation(rotation);
//            }
//        };
//        orientationEventListener.enable();
//        imageAnalyzer = new ImageAnalysis.Builder()
//                .setTargetAspectRatio(screenAspectRatio)
//                .setTargetRotation(rotation)
//                .build();
//        imageAnalyzer.setAnalyzer(cameraExecutor, new LuminosityAnalyser());
        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll();
        preview.setSurfaceProvider(previewView.createSurfaceProvider());
        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        cameraControl = camera.getCameraControl();

    }





    private int getAspectRatio(int width, int height) {
        float previewRatio = Math.max(width, height)/Math.min(width, height);
        if(Math.abs(previewRatio - RATIO_4_3_VALUE)<=Math.abs(previewRatio - RATIO_16_9_VALUE)){
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }


//    private void setZoomSlider(){
//        zoomSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                if(progress==0){
//                    cameraControl.setLinearZoom(0);
//                }else{
//                    cameraControl.setLinearZoom(progress/100f);
//                }
//                Log.d(TAG, "onProgressChanged: "+ progress );
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//
//            }
//        });
//    }



    @SuppressLint("ClickableViewAccessibility")
    private void setOnClickListener() {
        findViewById(R.id.toggleCameraBtn).setOnClickListener(v -> toggleFrontBackCamera());
        flashButton.setOnClickListener(v -> toggleFlash());
        findViewById(R.id.captureBtn).setOnClickListener(v -> takePicture());
        setUpPreviewTouchListeners();
    }



//    private void openGalleryActivity() {
//        Intent intent = new Intent(MainActivity.this, GalleryActivity.class);
//        startActivity(intent);
//    }


    @SuppressLint("ClickableViewAccessibility")
    private void setUpPreviewTouchListeners() {
        listener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float currentZoomRatio = camera.getCameraInfo().getZoomState().getValue().getZoomRatio();
                float scaleFactor = detector.getScaleFactor();
                zoomLevel.setText(Math.floor(currentZoomRatio * scaleFactor) +"X");
                cameraControl.setZoomRatio(currentZoomRatio*scaleFactor);
                return true;
            }
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                zoomLevel.setVisibility(View.VISIBLE);
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                zoomLevel.setVisibility(View.GONE);
            }
        };
        scaleDetector = new ScaleGestureDetector(MainActivity.this, listener);
        previewView.setOnTouchListener((v, event) -> {
            if(event.getAction()==MotionEvent.ACTION_MOVE){
                //If action is move trigger listener for zoom functionality
                Log.d(TAG, "Zoom: ");
                scaleDetector.onTouchEvent(event);
                return true;
            }else if(event.getAction()==MotionEvent.ACTION_DOWN){
                //Else if simple touch then trigger tap to focus functionality
                //For focus animation use action up as well.
                Log.d(TAG, "Touch:");
                MeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(previewView.getWidth(), previewView.getHeight());
                MeteringPoint point = factory.createPoint(event.getX(), event.getY());
                FocusMeteringAction action = new FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                        .addPoint(point, FocusMeteringAction.FLAG_AE)
                        .setAutoCancelDuration(5, TimeUnit.SECONDS)
                        .build();
                cameraControl.startFocusAndMetering(action);
                return true;
            }
            return false;
        });
    }


    private void toggleFrontBackCamera(){
        lensFacing = (lensFacing==CameraSelector.LENS_FACING_BACK) ?  CameraSelector.LENS_FACING_FRONT :  CameraSelector.LENS_FACING_BACK;
        bindCameraUseCase();
    }
    
    private void showToastMessage(String message){
        Toast toast = Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private void toggleFlash(){
        Log.d(TAG, "toggleFlash: "+flashMode);
        switch (flashMode){
            case ImageCapture.FLASH_MODE_OFF:
                flashMode = ImageCapture.FLASH_MODE_ON;
                flashButton.setBackgroundResource(R.drawable.ic_flash_on_24dp);
                flashButton.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.selected_color));
                showToastMessage("Flash On");
                break;
            case ImageCapture.FLASH_MODE_ON:
                flashMode = ImageCapture.FLASH_MODE_AUTO;
                flashButton.setBackgroundResource(R.drawable.ic_flash_auto_24dp);
                flashButton.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.selected_color));
                showToastMessage("Flash Auto");
                break;
            case ImageCapture.FLASH_MODE_AUTO:
                flashMode = ImageCapture.FLASH_MODE_OFF;
                flashButton.setBackgroundResource(R.drawable.ic_flash_off_24dp);
                flashButton.setBackgroundTintList(getApplicationContext().getResources().getColorStateList(R.color.unselected_color));
                showToastMessage("Flash Off");
                break;
        }
    }


    private void takePicture()  {
        imageCapture.setFlashMode(flashMode);
        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @SuppressLint("UnsafeExperimentalUsageError")
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Bitmap imageBitmap = rotateImage(imageToBitmap(Objects.requireNonNull(image.getImage())), image.getImageInfo().getRotationDegrees());
                try {
                    FileUtil.saveBitmap(MainActivity.this, imageBitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                super.onCaptureSuccess(image);
                //image.close();
            }
            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                super.onError(exception);
            }
        });
    }




//    private ImageCapture.Metadata getMetadata(){
//        ImageCapture.Metadata metadata = new ImageCapture.Metadata();
//        metadata.setReversedHorizontal(lensFacing==CameraSelector.LENS_FACING_FRONT);
//        return metadata;
//    }

    private Bitmap imageToBitmap(Image image){
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
        return bitmap;
    }


    private Bitmap rotateImage(Bitmap imageBitmap, int angle){
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap bitmap = Bitmap.createBitmap(imageBitmap, 0,0, imageBitmap.getWidth(), imageBitmap.getHeight(), matrix, true);
        return bitmap;
    }

    private boolean onLongClick(View v) {
        //openGalleryActivity();
        return true;
    }


//    @Override
//    protected void onSwipeRight() {
//        //Toast.makeText(MainActivity.this, "Right Swipe", Toast.LENGTH_SHORT).show();
//    }
//
//    @Override
//    protected void onSwipeLeft() {
////        Intent intent = new Intent(MainActivity.this, GalleryActivity.class);
////        startActivity(intent);
//    }


}
