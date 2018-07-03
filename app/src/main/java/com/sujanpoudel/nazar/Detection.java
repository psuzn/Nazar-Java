package com.sujanpoudel.nazar;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

public class Detection extends CameraActivity {
    private static final String sharedPrefenrences = "Settings";
    View.OnClickListener cameraSwitch  = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(Detection.this,"Switch cmaera",Toast.LENGTH_SHORT).show();
            Detection.this.recreate();
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_detection);
        super.onCreate(savedInstanceState);
        Log.d("Nazar Debug","Detection activity");
        ImageButton cameraSwitchButton = findViewById(R.id.cameraSwitch);
        cameraSwitchButton.setOnClickListener(cameraSwitch);
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
