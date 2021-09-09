package com.example.lab11parta_cameraapp;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    //debugging
    private String TAG = "MainAct";
    //path to image file
    private String currentPhotoPath;
    //UI Componants
    private FloatingActionButton fab;
    private PreviewView previewView;
    //Camera objects
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imagecapture;
    //Permissions
    private final String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Change Title
        setTitle("Take a picture");

        //Bind Preview View
        previewView = findViewById(R.id.previewView);

        //Bind Button & Set onclick listener
        fab = findViewById(R.id.bCapture);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                capturePhoto();
            }
        });
        fab.setEnabled(false);

        // Check permissions
        if(hasPermissions(PERMISSIONS))
        {
            fab.setEnabled(true); // We have permissions so we can set the image capture fab as active
            initCameraPreview(); // initalise Camera preview
        }
        else
        {
            askPermissions(PERMISSIONS); //Not permissions so we will ask permission and initalise Camera preview IF they are granted.
        }

    }

    private void initCameraPreview()
    {
        //Camera
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        //Add listener to provider
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                startCameraX(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void capturePhoto() {
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert photoFile != null;
        imagecapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(photoFile).build(),
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(MainActivity.this, "Photo capture success", Toast.LENGTH_SHORT).show();
                        // Add path to intent and go to Display activity
                        Intent intent = new Intent(getApplicationContext(), DisplayActivity.class);
                        Log.d(TAG, "Sending Path: " + currentPhotoPath);
                        intent.putExtra("path", currentPhotoPath);
                        intent.putExtra("rotation", getCameraPhotoOrientation());
                        startActivity(intent);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(MainActivity.this, "Photo capture error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }

        );
    }

    void startCameraX(@NonNull ProcessCameraProvider cameraProvider) {

        cameraProvider.unbindAll();

        //Select Camera (I.e. front/back)
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Create ImagePreview
        Preview preview = new Preview.Builder().build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Capture Image
        imagecapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imagecapture);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmssS", Locale.getDefault());
        String timeStamp = sdf.format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES); //These files are private to app and will be deleted upon uninstall
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // Capture image orientation, this is required as some devices rotate the image 90 degrees on capture.
    // By getting the rotation from the Exif data we can pass it on to the DisplayActivity to ensure the image is displayed correctly.
    public int getCameraPhotoOrientation() {
        int rotate = 0;
        try {
            ExifInterface exif = new ExifInterface(currentPhotoPath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }

            Log.i("RotateImage", "Exif orientation: " + orientation);
            Log.i("RotateImage", "Rotate value: " + rotate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rotate;
    }

    /****** Everything below here is just real-time permissions which you have used many times now******/

    //helper function to check permission status
    private boolean hasPermissions(String[] perms) {
        boolean permissionStatus = true;
        for (String permission : perms) {
            if (ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission is granted: " + permission);
            } else {
                Log.d(TAG, "Permission is not granted: " + permission);
                permissionStatus = false;
            }
        }
        return permissionStatus;
    }

    //helper function to ask user permissions
    private void askPermissions(String[] perms) {
        if (!hasPermissions(perms)) {
            Log.d(TAG, "Launching multiple contract permission launcher for ALL required permissions");
            multiplePermissionActivityResultLauncher.launch(perms);
        } else {
            Log.d(TAG, "All permissions are already granted");
        }
    }

    //Result launcher for permissions
    private final ActivityResultLauncher<String[]> multiplePermissionActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
                Log.d(TAG, "Launcher result: " + isGranted.toString());
                //permissions are granted lets get to work!
                fab.setEnabled(true);
                initCameraPreview();
                if (isGranted.containsValue(false)) {
                    Toast.makeText(MainActivity.this, "At least one of the permissions was not granted, please enable permissions to ensure app functionality", Toast.LENGTH_SHORT).show();
                }
            });

}