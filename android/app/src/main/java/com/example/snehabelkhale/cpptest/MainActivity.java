package com.example.snehabelkhale.cpptest;
import android.Manifest;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.icu.text.SimpleDateFormat;
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
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Mat;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    //camera variables
    private Camera mCamera;
    private CameraPreview mPreview;
    private Camera.PictureCallback mPicture;
    private Camera.Size optimalSize;
    private boolean cameraReady = true;

    //image processing variables
    private Mat debugMat = new Mat ();
    private Mat cameraMat = new Mat();
    private Matrix matrix = new Matrix();
    final Handler handler = new Handler();

    //touch event variables
    private float x1,x2;
    static final int MIN_DISTANCE = 150;

    //application variables
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
        CameraPermissionHelper.requestCameraPermission(this);

        //get the debug view and send it to the back
        matrix.postRotate(90); // anti-clockwise by 90 degrees
        ImageView iv = (ImageView) findViewById(R.id.debug_view);
        iv.setZ(-5);
        //set up processing for when picture taken
        mPicture = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
                Utils.bitmapToMat(bitmap, cameraMat);

                //send mat to the native C++ function readImage that decodes the QR's
                String decodedQR = readImage (cameraMat.getNativeObjAddr());

                //if image was successfully decoded..
                if (decodedQR.length() > 1){
                    //get the formatted date for the sound name
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(System.currentTimeMillis());

                    SimpleDateFormat timeFormat = new SimpleDateFormat("hh-mm-ss");
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");

                    String date = dateFormat.format(c.getTime());
                    String time = timeFormat.format(c.getTime());

                    String name = date + "_" + time + ".mp3";
                    writeToFile(decodedQR, name);

                    addToSoundListView(name);
                    restartQRReader();

                    //switch to the sound list view that will highlight and play recorded qr
                    Intent i = new Intent(getApplicationContext(), SoundListActivity.class);
                    i.putExtra("soundName", name);
                    startActivity(i);
                } else {
                    //get debug image from the native c++ code
                    getDebugImage(debugMat.getNativeObjAddr());
                    //convert to bitmap
                    Bitmap bm = Bitmap.createBitmap(debugMat.cols(), debugMat.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(debugMat, bm);

                    //draw debug image to the image view
                    ImageView iv = (ImageView) findViewById(R.id.debug_view);
                    //set the image view height and width to the optimal camera size parameters
                    iv.getLayoutParams().height = optimalSize.width;
                    iv.getLayoutParams().width = optimalSize.height;

                    //rescale the debug view image
                    int newWidth = (int)optimalSize.height*bm.getWidth()/bm.getHeight();
                    int newHeight = (int)optimalSize.height;
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bm, newWidth, newHeight, true);
                    Bitmap croppedScaledBitmap=Bitmap.createBitmap(scaledBitmap,0,0, optimalSize.width, optimalSize.height);

                    // create a new bitmap from the original using the matrix to transform the result
                    Bitmap rotatedBitmap = Bitmap.createBitmap(croppedScaledBitmap, 0, 0, croppedScaledBitmap.getWidth(), croppedScaledBitmap.getHeight(), matrix,true);
                    iv.setImageBitmap(rotatedBitmap);
                    iv.setZ(5);
                    //remove the debug view after 2000ms
                    handler.postDelayed(removeDebugView, 2000);
                }
                cameraReady = true;
                camera.startPreview();
            }
        };

        // Add a listener to the Capture button, to take pictures when clicked
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

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        if (sizes==null) return null;
        Camera.Size optimalSize = null;
        double max = -1;
        //simply pick the largest preview size
        for (Camera.Size size : sizes) {
            if (size.height > max) {
                optimalSize = size;
                max = size.height;
            }
        }
        return optimalSize;
    }

    public void openCamera () {
        mCamera = getCameraInstance();
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
        int w = 0, h = 0;
        //pick the largest picture size
        for (Camera.Size size : sizes) {
            if (size.width > w || size.height > h) {
                w = size.width;
                h = size.height;
            }
        }
        parameters.setPictureSize(w, h);

        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);

        //get optimal preview size and set the frame layout to this size
        optimalSize = getOptimalPreviewSize(previewSizes, w, h);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(optimalSize.height, optimalSize.width);
        preview.setLayoutParams(params);
        parameters.setPreviewSize(optimalSize.width, optimalSize.height);

        parameters.set("orientation", "portrait");

        //update camera with new parameters
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(90);
        mPreview = new CameraPreview(this, mCamera);
        preview.addView(mPreview);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (CameraPermissionHelper.hasCameraPermission(this)) {
            //do nothing
        } else {
            CameraPermissionHelper.requestCameraPermission(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
        } else {
            openCamera();
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
        SoundListActivity.listItems.add(0, name);
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
                if (Math.abs(deltaX) > MIN_DISTANCE) {
                    // Left to Right swipe action
                    if (x2 < x1) {
                        Intent i = new Intent(this, SoundListActivity.class);
                        startActivity(i);
                    }
                }
                else {
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
