package com.example.lab11parta_cameraapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import java.io.File;

public class DisplayActivity extends AppCompatActivity {

    private ImageView imgView;
    private String currentPhotoPath;
    private int imgRotation;
    private String TAG = "DispAct";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        //Change Title
        setTitle("Display the Picture");

        //Bind imageview
        imgView = findViewById(R.id.imgView);

        //Get info from Intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            currentPhotoPath = extras.getString("path");
            imgRotation = extras.getInt("rotation");
        }

        //Set image in imageview
        Bitmap myBitmap = BitmapFactory.decodeFile(currentPhotoPath);

        //check if image require rotation and render
        if(imgRotation == 0)
        {
            imgView.setImageBitmap(myBitmap);
        }
        else
        {
            imgView.setImageBitmap(rotateImage(myBitmap, imgRotation));
        }
    }

    //rotate Bitmap
    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    // create an action bar button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // R.menu.appbarmenu is a reference to an xml file named appbarmenu.xml which should be inside your res/menu directory.
        // If you don't have res/menu, just create a directory named "menu" inside res
        getMenuInflater().inflate(R.menu.appbarmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.takeAnotherPhotoBtn) {
            deleteFile();
            Intent returnIntent = new Intent(this, MainActivity.class);
            startActivity(returnIntent);
        }
        return true;
    }

    //delete photo
    private void deleteFile()
    {
        File fdelete = new File(currentPhotoPath);
        if (fdelete.exists()) {
            if (fdelete.delete()) {
                Log.d(TAG, "file Deleted :" + currentPhotoPath);
            } else {
                Log.d(TAG,"file not Deleted :" + currentPhotoPath);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();  // Always call the superclass method first
        deleteFile();
    }


}