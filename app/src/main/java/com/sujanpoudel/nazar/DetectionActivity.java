package com.sujanpoudel.nazar;

import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.CompoundButton;
import android.widget.Toast;


public class DetectionActivity extends CameraActivity {

    int detectionMode = DetectionMode.SingleImage;
    AlphaAnimation buttonclick = new AlphaAnimation(1f,0.5f);
    private SharedPreferences settings;

    @interface DetectionMode{
        int SingleImage = 0;
        int Realtime =1;
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getPreferences();
        setContentView(R.layout.activity_detection);
        super.onCreate(savedInstanceState);

        Log.d("Nazar Debug","DetectionActivity activity");

        //initialize event listeners
        findViewById(R.id.cameraSwitch).setOnClickListener(cameraSwitch);
        findViewById(R.id.capture).setOnClickListener(capture);
        findViewById(R.id.singleImageMode).setOnClickListener(singleImageMode);
        findViewById(R.id.realTimeMode).setOnClickListener(realtimeMode);
        ( (CompoundButton) findViewById(R.id.modeSwitch)).setOnCheckedChangeListener(modeSwitch);
        setUIElements(); // make ui elements as defines on settings
    }

    @Override
    protected TextureView getCameraPreviewImageView() {
        return findViewById(R.id.camPreview1);
    }

    @Override
    protected void onPreviewSizeChosen() {
        mCamera.addCallbackBuffer (new byte[ ImageUtils.getYUVByteSize(previewWidth,previewHeight) ]);
        mCamera.addCallbackBuffer (new byte[ ImageUtils.getYUVByteSize(previewWidth,previewHeight) ]);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        camera.addCallbackBuffer(data);
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
            findViewById(R.id.bottomContainer).setVisibility(View.INVISIBLE);
        else
            findViewById(R.id.bottomContainer).setVisibility(View.VISIBLE);
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
            findViewById(R.id.bottomContainer).setVisibility(View.VISIBLE);

        }
        else
        {
            toastMessage+="Realtime DetectionActivity Mode";
            findViewById(R.id.bottomContainer).setVisibility(View.INVISIBLE);
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

}
