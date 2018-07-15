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
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;


public class DetectionActivity extends CameraActivity {

    int detectionMode = DetectionMode.SingleImage;
    AlphaAnimation buttonclick = new AlphaAnimation(1f,0.5f);
    SharedPreferences settings;
    ImageView resultOverlay;
    DetectionOverlayManager overMgr;
    private Object detector;
    private HandlerThread handlerThread;
    private Handler handler;
    private Matrix frameToCropTransform;
    private Runnable imageConverter;

    private int[] rgbBytes;
    private byte[] lastFrame;
    private Bitmap origionalSizedBitmap;
    private Bitmap croppedBitmap;

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
        imageConverter =
                new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.convertYUV420SPToARGB8888(lastFrame, previewWidth, previewHeight, rgbBytes);
                    }
                };
        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewHeight, previewWidth,
                        ObjectDetectionAPI.inputSize, ObjectDetectionAPI.inputSize,
                        90, true);

    }
    boolean saved = false;
    @Override
    public void onPreviewFrame(final byte[] data, Camera camera) {
        System.arraycopy(data,0,lastFrame,0,data.length);
        runInBackground(new Runnable() {
            @Override
            public void run() {
                origionalSizedBitmap.setPixels(getRgbBytes(),0,previewHeight,0,0,previewHeight,previewWidth);
                if(!saved)
                    ImageUtils.saveBitmap(origionalSizedBitmap,"origioinalsize.png");

                Canvas c= new Canvas(croppedBitmap);
                c.drawBitmap(origionalSizedBitmap,frameToCropTransform,null);
                if(!saved)
                    ImageUtils.saveBitmap(croppedBitmap,"cropped.png");
                saved = true;
            }
        });

        //saved = true;
        camera.addCallbackBuffer(data);

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
            return  null;
        }
        @Override
        protected void onPostExecute(Void spinnerView) {
            findViewById(R.id.loadingAnim).setVisibility(View.INVISIBLE);
            Toast.makeText(getApplicationContext(), "Model Loaded", Toast.LENGTH_SHORT).show();
        }
    }
}
