package com.sujanpoudel.nazar;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public abstract class CameraActivity extends android.app.Activity

    implements  Camera.PreviewCallback,TextureView.SurfaceTextureListener{
    private static final int FOCUS_AREA_SIZE = 30;
    protected int usecamera = Camera.CameraInfo.CAMERA_FACING_BACK;
    protected TextureView cameraPreviewImageView;
    protected Camera mCamera;
    protected int previewWidth,previewHeight;
    protected  float cameraOrientation = 90;
    protected Runnable onFocus;

    static {
        System.loadLibrary("native-lib");
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getPreferences();
        if(havePermission())
        {
            cameraPreviewImageView = getCameraPreviewImageView();
            cameraPreviewImageView.setSurfaceTextureListener(this);
        }
        else
            this.requestPermission();
        findViewById(R.id.camPreview).setOnTouchListener(onTapFocusListner);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 1)
        {
            boolean grantResult = true;
            for (int result : grantResults)
                grantResult &= result == PackageManager.PERMISSION_GRANTED;
            if(! grantResult)
                this.requestPermission();
            else
            {
                cameraPreviewImageView = getCameraPreviewImageView();
                cameraPreviewImageView.setSurfaceTextureListener(this);
                Toast.makeText(this,"Camera and Storage Permission are granted.",Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d("Nazar_Debug"," onsurfaceavialble");
        if(mCamera == null)
        {
            setupCamera();
            if(mCamera == null ) // mcamera is null when there is no camera
                return;
        }
        try {
            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();
            Log.d("Nazar_Debug"," camera preview is starting");
        } catch (IOException ioe) {
            Toast.makeText(this,"Couldn't connect with camera",Toast.LENGTH_LONG).show();
        }
    }
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if(mCamera !=null)
        {
            mCamera.stopPreview();
            mCamera.release();
        }

        return true;
    }

    private void setupCamera(){
        try {
            mCamera = Camera.open( chooseCamera() );
        }
        catch (Exception e)
        {
            new AlertDialog.Builder( this)
                    .setTitle("Couldn't connect with camera")
                    .setCancelable(false)
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            CameraActivity.this.quitApp(-1);
                        }
                    }).show();
            Log.d("Nazar Debug","Couldn't connect with camera");
            return;
        }

        List<Camera.Size> mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
        int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        int screenWidth  = Resources.getSystem().getDisplayMetrics().widthPixels;
        Camera.Size optimalPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes,screenWidth,screenHeight);

        Camera.Parameters cameraParameters = mCamera.getParameters();
        cameraParameters.setPreviewSize(optimalPreviewSize.width,optimalPreviewSize.height);
        if(cameraParameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) )
        {
            mCamera.cancelAutoFocus();
            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        else if(cameraParameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO))
            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

        mCamera.setParameters(cameraParameters);
        mCamera.setDisplayOrientation((int)cameraOrientation);

        mCamera.setPreviewCallbackWithBuffer(this);
        previewHeight = mCamera.getParameters().getPreviewSize().width; //camera is 90 degree oriented
        previewWidth = mCamera.getParameters().getPreviewSize().height; //camera is 90 degree oriented
        onPreviewSizeChosen();

    }

    private int chooseCamera() {
        int cameraIndex = -1,frontCameraIndex =-1, backCameraIndex = -1;
        Camera.CameraInfo ci = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, ci);
            if (ci.facing == Camera.CameraInfo.CAMERA_FACING_BACK )
            {
                backCameraIndex = i;
            }

            else if( ci.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
            {
                frontCameraIndex = i;
            }
        }
        Log.d("Nazar Debug","frontcamindex:"+frontCameraIndex+" backcamindex:"+backCameraIndex);
        cameraIndex = (usecamera == Camera.CameraInfo.CAMERA_FACING_FRONT)? frontCameraIndex : backCameraIndex;

        if(frontCameraIndex == -1 && backCameraIndex == -1) // no any camera found
        {
            AlertDialog.Builder builder  = new AlertDialog.Builder(this);
            builder.setMessage("Couldn't connect with camera").show();
            quitApp(1);
            return -1;
        }
        if(usecamera == Camera.CameraInfo.CAMERA_FACING_FRONT && frontCameraIndex == -1 ) //front camera selected but not found
        {
            if(backCameraIndex != -1) //no front camera but has rear camera
                cameraIndex = backCameraIndex;
            Toast.makeText(this,"Couldn't connect with front camera",Toast.LENGTH_LONG).show();
        }
        if(usecamera == Camera.CameraInfo.CAMERA_FACING_BACK && backCameraIndex ==-1) //rear camera selected but not found
        {
            if(frontCameraIndex != -1) // no rear camera but has front camera
                cameraIndex = frontCameraIndex;

            Toast.makeText(this,"Couldn't connect with rear camera",Toast.LENGTH_LONG).show();
        }
        return  cameraIndex;
    }


    // to choose best camera previewsize
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            Log.d("none","preview size:"+size.height+" width:"+size.width);
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        Log.d("none","Optimal size:"+optimalSize.height+" width:"+optimalSize.width);
        return optimalSize;
    }
    private boolean havePermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            return ( checkSelfPermission(Manifest.permission.CAMERA ) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE ) == PackageManager.PERMISSION_GRANTED  &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE ) == PackageManager.PERMISSION_GRANTED ) ;
        }
        return true;
    }
    private void requestPermission(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Toast.makeText(this,"Camera and Storage Permission are required to run this app.",Toast.LENGTH_LONG).show();
            requestPermissions(new String[]{ Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE },1);
        }
    }
    private void quitApp( int status){
        Log.d("Nazar Debug","quitting");
        this.finishActivity(-1);
        System.exit(-1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mCamera!=null)
        {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        if(mCamera==null)
        {
            setupCamera();
            if(mCamera!=null) // mcamera will be null when there is no camera
                mCamera.startPreview();
        }
    }

    private int clamp(int touchCoordinateInCameraReper, int focusAreaSize) {
        int result;
        if (Math.abs(touchCoordinateInCameraReper)+focusAreaSize/2>1000){
            if (touchCoordinateInCameraReper>0){
                result = 1000 - focusAreaSize/2;
            } else {
                result = -1000 + focusAreaSize/2;
            }
        } else{
            result = touchCoordinateInCameraReper - focusAreaSize/2;
        }
        return result;
    }
    private Rect calculateFocusArea(float x, float y) {
        int left = clamp(Float.valueOf((x / previewWidth) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);
        int top = clamp(Float.valueOf((y / previewHeight) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);

        return new Rect(left, top, left + FOCUS_AREA_SIZE, top + FOCUS_AREA_SIZE);
    }

    View.OnTouchListener onTapFocusListner =  new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            setFocusPoint(event.getX(),event.getY());
            return true;
        }
    };

    protected void setFocusPoint(float x,float y){
        Camera.Parameters parameters = mCamera.getParameters();
        if (parameters.getMaxNumMeteringAreas() > 0){
            Rect rect = calculateFocusArea(x,y);

            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
            meteringAreas.add(new Camera.Area(rect, 800));
            parameters.setFocusAreas(meteringAreas);

            mCamera.setParameters(parameters);
            mCamera.autoFocus(mAutoFocusTakePictureCallback);
        }else {
            mCamera.autoFocus(mAutoFocusTakePictureCallback);
        }
    }

    private Camera.AutoFocusCallback mAutoFocusTakePictureCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if(onFocus !=null)
                onFocus.run();
            onFocus = null;
        if (success) {
            // do something...
            Log.i("tap_to_focus","success!");
        } else {
            // do something...
            Log.i("tap_to_focus","fail!");
        }
        }
    };
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
    protected abstract TextureView getCameraPreviewImageView();
    protected abstract void onPreviewSizeChosen();
    protected abstract void getPreferences();
}
