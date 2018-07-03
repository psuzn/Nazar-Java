package com.sujanpoudel.nazar;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.TextureView;

public class Detection extends CameraActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.activity_detection);
        super.onCreate(savedInstanceState);
        Log.d("Nazar Debug","Detection activity");
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
}
