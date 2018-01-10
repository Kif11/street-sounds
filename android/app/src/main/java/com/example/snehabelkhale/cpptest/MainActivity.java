package com.example.snehabelkhale.cpptest;
import android.Manifest;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private Camera mCamera;
    private CameraPreview mPreview;
    private Camera.PictureCallback mPicture;
    private boolean cameraReady = true;
    private Mat debugMat = new Mat ();
    private Mat cameraMat = new Mat();
    private Matrix matrix = new Matrix();
    final Handler handler = new Handler();
    private float x1,x2;
    static final int MIN_DISTANCE = 150;

    private MediaPlayer mediaPlayer = new MediaPlayer();
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
    private static final String TAG = "MyActivity";

    final Runnable removeDebugView = new Runnable() {
        public void run() {
            ImageView iv = (ImageView) findViewById(R.id.debug_view);
            iv.setZ(-5);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.CAMERA},
                1);
        mCamera = getCameraInstance();
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
        int w = 0, h = 0;
        for (Camera.Size size : sizes) {
            Log.d(TAG, "cam width: " + size.width);
            if (size.width > w || size.height > h) {
                w = size.width;
                h = size.height;
            }

        }
        parameters.setPictureSize(w, h);
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(90);
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);

        preview.addView(mPreview);
        Log.d(TAG, "creating");
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                Log.i("Completion Listener","Song Complete");
                mp.stop();
                mp.reset();
            }
        });
        matrix.postRotate(90); // anti-clockwise by 90 degrees
        ImageView iv = (ImageView) findViewById(R.id.debug_view);
        iv.setZ(-5);
        mPicture = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                BitmapFactory.Options opts = new BitmapFactory.Options(); // This was missing.
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, opts);

                Utils.bitmapToMat(bitmap, cameraMat);

                // Note: when the matrix is to large mat.dump() might also freeze your app.
                Log.d(TAG, "clicked");
                String temp = readImage (cameraMat.getNativeObjAddr());

                if (temp.length() > 1){
                    Log.e(TAG, "**** passed");
                    Long tsLong = System.currentTimeMillis()/1000;
                    String date = tsLong.toString();
                    String name = date + ".mp3";
                    writeToFile(temp, name);
                    addToSoundListView(name);

                    playSound(name);
                    restartQRReader();
                } else {
                    Log.e(TAG, "**** failed");
                    //show Debug image
                    getDebugImage(debugMat.getNativeObjAddr());
                    //convert to bitmap:
                    Bitmap bm = Bitmap.createBitmap(debugMat.cols(), debugMat.rows(),Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(debugMat, bm);
                    //find the imageview and draw it!
                    ImageView iv = (ImageView) findViewById(R.id.debug_view);
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bm, iv.getWidth(), iv.getHeight(), true);
                    // create a matrix object
                    // create a new bitmap from the original using the matrix to transform the result
                    Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap , 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
                    iv.setImageBitmap(rotatedBitmap);
                    iv.setZ(5);
                    handler.postDelayed(removeDebugView, 2000);
                }
                cameraReady = true;
                camera.startPreview();

            }
        };



        // Add a listener to the Capture button
        Button captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        if(cameraReady) {
                            cameraReady = false;
                            mCamera.takePicture(null, null, mPicture);
                        } else {
                            Log.e(TAG, "already taking picture");
                        }
                    }
                }
        );

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d(TAG, "PERMISSION DENIED.");
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            Log.e(TAG, "Camera not found.");
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private void writeToFile(String data, String name) {
        String filename = name;
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(filename, this.MODE_PRIVATE);
            byte[] decodedString = Base64.decode(data, Base64.DEFAULT);
            outputStream.write(decodedString);
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void addToSoundListView(String name) {
        SoundListActivity.listItems.add(name);
    }
    public void playSound (String soundName){
        File t = this.getFileStreamPath(soundName);
        Uri u = Uri.fromFile(t);
        try {
            mediaPlayer.setDataSource(getApplicationContext(), u);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.start();
        return;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                float deltaX = x2 - x1;

                if (Math.abs(deltaX) > MIN_DISTANCE)
                {
                    // Left to Right swipe action
                    if (x2 < x1)
                    {
                        Intent i = new Intent(this, SoundListActivity.class);
                        startActivity(i);
                    }

                    // Right to left swipe action
                }
                else
                {
                    // consider as something else - a screen tap for example
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String readImage(long test);
    public native void getDebugImage(long debug);

    public native void restartQRReader();

}
