package com.sujanpoudel.nazar;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;

public class ClassificationActivity extends CameraActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classification);
    }

    @Override
    protected TextureView getCameraPreviewImageView() {
        return null;
    }

    @Override
    protected void onPreviewSizeChosen() {

    }

    @Override
    protected void getPreferences() {

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

    }
}
