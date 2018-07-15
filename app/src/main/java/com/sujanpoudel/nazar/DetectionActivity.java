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
    AlphaAnimation buttonclick = new AlphaAnimation(1f,0.5f);
    SharedPreferences settings;
    ImageView resultOverlay;
    DetectionOverlayManager overMgr;
    private ObjectDetectionAPI detector;
    private HandlerThread handlerThread;
    private Handler handler;
    private Matrix frameToCropTransform;
    private Runnable imageConverter;

    private int[] rgbBytes;
    private byte[] lastFrame;
    private Bitmap origionalSizedBitmap;
    private Bitmap croppedBitmap;
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
        findViewById(R.id.resultOverlay).setOnTouchListener(onResultOverlayTouch);

        lastFrame = new byte[ImageUtils.getYUVByteSize(previewWidth,previewHeight)];
        rgbBytes = new int[previewWidth * previewHeight];
        origionalSizedBitmap = Bitmap.createBitmap(previewHeight,previewWidth, Bitmap.Config.ARGB_8888);//image will be landscape so
        croppedBitmap = Bitmap.createBitmap(ObjectDetectionAPI.inputSize,ObjectDetectionAPI.inputSize, Bitmap.Config.ARGB_8888);//it will be portraid

        try {
            new ModelLoader().execute();
        } catch (final Exception e) {
            Toast.makeText(getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT).show();
            finish();
        }
        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewHeight, previewWidth,
                        ObjectDetectionAPI.inputSize, ObjectDetectionAPI.inputSize,
                        90, true);

    }

    public void processImage(){
        if (computingDetection){
            readyForNextFrame();
            return;
        }
        computingDetection = true;
        origionalSizedBitmap.setPixels(getRgbBytes(),0,previewHeight,0,0,previewHeight,previewWidth);
        readyForNextFrame();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(origionalSizedBitmap, frameToCropTransform, null);
        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        final long startTime = SystemClock.uptimeMillis();

                        final List<Recognition> results = detector.detect(croppedBitmap);
                        long lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                        Log.d("Nazar Debug","inference runin "+lastProcessingTimeMs);
//                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
//                        final Canvas canvas = new Canvas(cropCopyBitmap);
//                        final Paint paint = new Paint();
//                        paint.setColor(Color.RED);
//                        paint.setStyle(Style.STROKE);
//                        paint.setStrokeWidth(2.0f);
//
//                        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
//                        switch (MODE) {
//                            case TF_OD_API:
//                                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
//                                break;
//                            case MULTIBOX:
//                                minimumConfidence = MINIMUM_CONFIDENCE_MULTIBOX;
//                                break;
//                            case YOLO:
//                                minimumConfidence = MINIMUM_CONFIDENCE_YOLO;
//                                break;
//                        }
//
//                        final List<Classifier.Recognition> mappedRecognitions =
//                                new LinkedList<Classifier.Recognition>();
//
//                        for (final Classifier.Recognition result : results) {
//                            final RectF location = result.getLocation();
//                            if (location != null && result.getConfidence() >= minimumConfidence) {
//                                canvas.drawRect(location, paint);
//
//                                cropToFrameTransform.mapRect(location);
//                                result.setLocation(location);
//                                mappedRecognitions.add(result);
//                            }
//                        }
//
//                        tracker.trackResults(mappedRecognitions, luminanceCopy, currTimestamp);
//                        trackingOverlay.postInvalidate();
//
//                        requestRender();
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
        //System.arraycopy(data,0,lastFrame,0,data.length);

        //saved = true;
        //camera.addCallbackBuffer(data);

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
//        try {
//            handlerThread.join();
//            handlerThread = null;
//            handler = null;
//        } catch (final InterruptedException e) {
//
//        }
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
     View.OnTouchListener onResultOverlayTouch =  new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(overMgr==null)
                return false;
            int touchX = (int) event.getX();
            int touchY = (int) event.getY();
            int[] viewCoords = new int[2];
            resultOverlay.getLocationOnScreen(viewCoords);

            int imageX = touchX - viewCoords[0]; // viewCoords[0] is the X coordinate
            int imageY = touchY - viewCoords[1];
            overMgr.drawCircle(imageX,imageY,5);
            overMgr.drawRectnagle(new RectF(imageX-10,imageY-10,imageX+10,imageY+10));
            return  false;
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
