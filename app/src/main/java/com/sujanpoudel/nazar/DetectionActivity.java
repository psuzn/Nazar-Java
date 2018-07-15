package com.sujanpoudel.nazar;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.List;


public class DetectionActivity extends CameraActivity {

    int detectionMode = DetectionMode.SingleImage;
    private float minimumConfidence = 0.5f;
    AlphaAnimation buttonclick = new AlphaAnimation(1f,0.5f);
    SharedPreferences settings;
    ImageView resultOverlay;
    DetectionOverlayManager overMgr;
    private ObjectDetectionAPI detector;
    private HandlerThread handlerThread;
    private Handler handler;
    private Matrix resizeToInputMatrix;
    private Matrix resizeToPreviewMatrix;
    private Matrix MakePotraidMatrix;

    private Runnable imageConverter;

    private int[] rgbBytes;
    private byte[] lastFrame;
    private Bitmap onPreviewCallbackBitmap;
    private Bitmap onPreviewCallbackPotraidBitmp;
    private Bitmap inputBitmap;
    Canvas onPreviewCallbackPotraidCanvas;
    Canvas inputBitmapCanvas;


    private Runnable postInferenceCallback;
    private boolean isProcessingFrame = false;
    private boolean computingDetection = false;
    private boolean modelLoaded = false;

    @interface DetectionMode{
        int SingleImage = 0;
        int Realtime =1;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getPreferences();
        setContentView(R.layout.activity_detection);
        super.onCreate(savedInstanceState);
        resultOverlay = findViewById(R.id.resultOverlay);
        Log.d("Nazar Debug","DetectionActivity activity");

        //initialize event listeners
        findViewById(R.id.cameraSwitch).setOnClickListener(cameraSwitch);
        findViewById(R.id.capture).setOnClickListener(capture);
        findViewById(R.id.singleImageMode).setOnClickListener(singleImageMode);
        findViewById(R.id.realTimeMode).setOnClickListener(realtimeMode);
        ( (CompoundButton) findViewById(R.id.modeSwitch)).setOnCheckedChangeListener(modeSwitch);
        setUIElements(); // make ui elements as defines on settings
        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    protected TextureView getCameraPreviewImageView() {
        return findViewById(R.id.camPreview);
    }

    @Override
    protected void onPreviewSizeChosen() {
        mCamera.addCallbackBuffer (new byte[ ImageUtils.getYUVByteSize(previewWidth,previewHeight) ]);
        mCamera.addCallbackBuffer (new byte[ ImageUtils.getYUVByteSize(previewWidth,previewHeight) ]);
        Log.d("Nazar Debug","previewWidth:"+previewWidth+" PreviewHeight:"+previewHeight);
        rgbBytes = new int[previewWidth * previewHeight];
        onPreviewCallbackBitmap = Bitmap.createBitmap(previewHeight,previewWidth, Bitmap.Config.ARGB_8888);//image will be landscape so
        inputBitmap = Bitmap.createBitmap(ObjectDetectionAPI.inputSize,ObjectDetectionAPI.inputSize, Bitmap.Config.ARGB_8888);//it will be portraid
        onPreviewCallbackPotraidBitmp = Bitmap.createBitmap(previewWidth,previewHeight, Bitmap.Config.ARGB_8888);
        onPreviewCallbackPotraidCanvas = new Canvas(onPreviewCallbackPotraidBitmp);
        inputBitmapCanvas = new Canvas(inputBitmap);

        try {
            new ModelLoader().execute();
        } catch (final Exception e) {
            Toast.makeText(getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT).show();
            finish();
        }

        resizeToPreviewMatrix = new Matrix();
        MakePotraidMatrix = ImageUtils.getTransformationMatrix(previewHeight,previewWidth,previewWidth,previewHeight,90,true);
        resizeToInputMatrix =ImageUtils.getTransformationMatrix(
                                    previewWidth, previewHeight,
                                    ObjectDetectionAPI.inputSize, ObjectDetectionAPI.inputSize,
                                    0, true);
        resizeToInputMatrix.invert(resizeToPreviewMatrix);
    }

    public void processImage(){
        if (computingDetection){
            readyForNextFrame();
            return;
        }
        computingDetection = true;
        onPreviewCallbackBitmap.setPixels(getRgbBytes(),0,previewHeight,0,0,previewHeight,previewWidth);
        readyForNextFrame();
        overMgr.invalidate();
        onPreviewCallbackPotraidCanvas.drawBitmap(onPreviewCallbackBitmap,MakePotraidMatrix,null);
        final Canvas canvas = new Canvas(inputBitmap);
        inputBitmapCanvas.drawBitmap(onPreviewCallbackPotraidBitmp, resizeToInputMatrix, null);

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        final long startTime = SystemClock.uptimeMillis();

                        final List<Recognition> results = detector.detect(inputBitmap);
                        overMgr.clear();
                        long lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                        Log.d("Nazar Debug","inference runin "+lastProcessingTimeMs);
                        for (final Recognition result : results) {

                            final RectF location = result.getRect();
                            Log.d("Nazar Debug","detectedClass:"+result.getClassId()+" confidence"+result.getConfidence());
                            if (location != null && result.getConfidence() >= minimumConfidence) {
                                resizeToPreviewMatrix.mapRect(location);
                                overMgr.drawRectnagle(location);
                            }
                        }

                        computingDetection = false;
                    }
                });

    }

    void readyForNextFrame(){
        postInferenceCallback.run();

    }
    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        if(isProcessingFrame)
        {
            Log.d("Nazar Debug","frame dropping");
        }
        if(!modelLoaded)
        {
            camera.addCallbackBuffer(data);
            return;
        }
        isProcessingFrame = true;
        imageConverter =
                new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.convertYUV420SPToARGB8888(data, previewHeight, previewWidth, rgbBytes); //coz preview frame is landscape
                    }
                };
        postInferenceCallback =
                new Runnable() {
                    @Override
                    public void run() {
                        camera.addCallbackBuffer(data);
                        isProcessingFrame = false;
                    }
                };
        processImage();

    }
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            overMgr = new DetectionOverlayManager( resultOverlay, resultOverlay.getWidth(), resultOverlay.getHeight());
        }
    }
    private void setUIElements() {
        CompoundButton toggle =  findViewById(R.id.modeSwitch);
        Log.d("Nazar Debug","button"+toggle.isChecked());
        if( detectionMode == DetectionMode.SingleImage &&  toggle.isChecked()) {
            toggle.toggle();
            Toast.makeText(DetectionActivity.this,"Single Image DetectionActivity Mode",Toast.LENGTH_SHORT).show();
        }
        else if( detectionMode == DetectionMode.Realtime &&  !toggle.isChecked() )
        {
            toggle.toggle();
            Toast.makeText(DetectionActivity.this,"Realtime DetectionActivity Mode",Toast.LENGTH_SHORT).show();
        }
        if(detectionMode == DetectionMode.Realtime)
        {
            findViewById(R.id.capture).setVisibility(View.INVISIBLE);
            findViewById(R.id.addImage).setVisibility(View.INVISIBLE);
        }
        else
        {
            findViewById(R.id.capture).setVisibility(View.VISIBLE);
            findViewById(R.id.addImage).setVisibility(View.VISIBLE);
        }
    }
    void changeDetectionMode(int d){
        CompoundButton toggle =  findViewById(R.id.modeSwitch);
        if( ( toggle.isChecked() && d  == DetectionMode.SingleImage ) || ( !toggle.isChecked() && d  == DetectionMode.Realtime ) )
            toggle.toggle();
        if(d == detectionMode)
            return;
        String toastMessage ="";
        if(d == DetectionMode.SingleImage)
        {
            toastMessage+="Single Image DetectionActivity Mode";
            findViewById(R.id.capture).setVisibility(View.VISIBLE);
            findViewById(R.id.addImage).setVisibility(View.VISIBLE);

        }
        else
        {
            toastMessage+="Realtime DetectionActivity Mode";
            findViewById(R.id.capture).setVisibility(View.INVISIBLE);
            findViewById(R.id.addImage).setVisibility(View.INVISIBLE);
        }
        detectionMode = d;
        Toast.makeText(DetectionActivity.this,toastMessage,Toast.LENGTH_SHORT).show();
        settings.edit().putInt("detectionMode",detectionMode).apply();
    }
    @Override
    protected void getPreferences() {
        if(settings == null)
            settings = getSharedPreferences("Settings",MODE_PRIVATE);

        usecamera = settings.getInt("cameraId",usecamera);     // select which camera to use
        detectionMode = settings.getInt("detectionMode", detectionMode);

    }
    public int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }
    @Override
    protected void onPause() {
        super.onPause();
        handlerThread.quit();
        try {
            handlerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        handlerThread = null;
        handler = null;

    }
    @Override
    protected void onResume() {
        super.onResume();
        if(handlerThread==null)
        {
            handlerThread = new HandlerThread("inference");
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper());
        }
    }
    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }
    View.OnClickListener cameraSwitch  = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            v.startAnimation(buttonclick);
            Toast.makeText(DetectionActivity.this,"Camera Switched",Toast.LENGTH_SHORT).show();
            int nextCam = (usecamera == Camera.CameraInfo.CAMERA_FACING_BACK )?
                    Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
            SharedPreferences settings = getSharedPreferences("Settings",MODE_PRIVATE);
            settings.edit().putInt("cameraId",nextCam).apply();
            DetectionActivity.this.recreate();
        }
    };

    View.OnClickListener capture  = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            v.startAnimation(buttonclick);
        }
    };
    View.OnClickListener singleImageMode  = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            v.startAnimation(buttonclick);
            DetectionActivity.this.changeDetectionMode(DetectionMode.SingleImage);
        }
    };

    View.OnClickListener realtimeMode  = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            v.startAnimation(buttonclick);
            DetectionActivity.this.changeDetectionMode(DetectionMode.Realtime);
        }
    };
    CompoundButton.OnCheckedChangeListener  modeSwitch = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(!isChecked)
                DetectionActivity.this.changeDetectionMode(DetectionMode.SingleImage);
            else
                DetectionActivity.this.changeDetectionMode(DetectionMode.Realtime);
        }
    };
    private class ModelLoader extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground( Void... voids) {
            detector = new ObjectDetectionAPI(getAssets());
            modelLoaded = true;
            return  null;
        }
        @Override
        protected void onPostExecute(Void spinnerView) {
            findViewById(R.id.loadingAnim).setVisibility(View.INVISIBLE);
            Toast.makeText(getApplicationContext(), "Model Loaded", Toast.LENGTH_SHORT).show();
        }
    }
}
