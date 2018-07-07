package com.sujanpoudel.nazar;

import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;

public class ClassificationActivity extends CameraActivity {

    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_classification);
        super.onCreate(savedInstanceState);


    }

    @Override
    protected TextureView getCameraPreviewImageView() {
        return findViewById(R.id.camPreview);
    }

    @Override
    protected void onPreviewSizeChosen() {
        mCamera.addCallbackBuffer(new byte[ImageUtils.getYUVByteSize(previewWidth,previewHeight)]);
        mCamera.addCallbackBuffer(new byte[ImageUtils.getYUVByteSize(previewWidth,previewHeight)]);
    }

    @Override
    protected void getPreferences() {
        if(settings == null)
            settings = getSharedPreferences("Settings",MODE_PRIVATE);

        usecamera = settings.getInt("cameraId",usecamera);     // select which camera to use
    }
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        camera.addCallbackBuffer(data);
    }

}
