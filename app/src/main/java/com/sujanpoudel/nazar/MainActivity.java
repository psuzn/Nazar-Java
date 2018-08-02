package com.sujanpoudel.nazar;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.ArrayList;


class WizardPagerAdapter extends PagerAdapter {
    RadioButton[] pageIndicator ;

    public Object instantiateItem(ViewGroup collection, int position) {
        int resId = 0;
        switch (position) {
            case 0:
                resId = R.id.pageOne;
                break;
            case 1:
                resId = R.id.pageTwo;
                break;
        }
        return collection.findViewById(resId);
    }
    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0 == arg1;
    }

    @Override public void destroyItem(ViewGroup container, int position, Object object) {
        // No super
    }
}



public class MainActivity extends CameraActivity {

    SharedPreferences settings;
    ArrayList<RadioButton>  sliderPageIndicator = new ArrayList<>();
    ValueAnimator colorAnimator;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getPreferences(); // important preferences should get first
        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);
        WizardPagerAdapter adapter = new WizardPagerAdapter();
        ViewPager pager = findViewById(R.id.pager);
        pager.setAdapter(adapter);
        pager.addOnPageChangeListener(onPageChangeListner);
        //findViewById(R.id.startClassification).setOnClickListener(onClassificationButtonClick);
        findViewById(R.id.startDetection).setOnClickListener(onDetectionButtonClick);

        //for the buttom radiobuttons
        RadioGroup radioButtonGroup = findViewById(R.id.pageIndicator);
        int childs = radioButtonGroup.getChildCount();
        for(int i = 0;i <= childs;i++){
            View o  = radioButtonGroup.getChildAt(i);
            if(o instanceof RadioButton)
                sliderPageIndicator.add((RadioButton) o);
        }
        startColorAnimation();

    }
    void startColorAnimation(){
        final ImageView blurPreview = findViewById(R.id.colorOverlay);
        colorAnimator = ValueAnimator.ofFloat(0, 1);   // animate from 0 to 1
        colorAnimator.setDuration(8000);                              // for 8000 ms
        colorAnimator.setRepeatMode(ValueAnimator.REVERSE);
        colorAnimator.setRepeatCount(ValueAnimator.INFINITE);
        colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override public void onAnimationUpdate(ValueAnimator animation) {
                blurPreview.setBackgroundColor(Color.HSVToColor(new float[]{ animation.getAnimatedFraction() * 360 , 0.7f,0.7f}));
                blurPreview.setAlpha(0.7f);
            }
        });
        colorAnimator.start();
    }

    @Override
    protected TextureView getCameraPreviewImageView() {
        return findViewById(R.id.camPreview);

    }

    @Override
    protected void onPreviewSizeChosen() {
        mCamera.addCallbackBuffer (new byte[ ImageUtils.getYUVByteSize(previewWidth,previewHeight) ]);
        mCamera.addCallbackBuffer (new byte[ ImageUtils.getYUVByteSize(previewWidth,previewHeight) ]);
    }

    @Override
    public void onPreviewFrame(byte[] data, android.hardware.Camera camera) {
        camera.addCallbackBuffer(data);

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("Nazar debug","pausing");
    }

    @Override
    protected void onResume() {
        super.onResume();
        colorAnimator.start();
        Log.d("Nazar debug","resumming");
    }

    @Override
    protected void getPreferences() {
            settings = getSharedPreferences("Settings",MODE_PRIVATE);
            usecamera = settings.getInt("cameraId",usecamera);
    }

    //event listeners
    ViewPager.OnPageChangeListener onPageChangeListner =
            new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
                    if( !sliderPageIndicator.isEmpty() )
                        for(int i =0 ; i < sliderPageIndicator.size() ; i++ )
                            if(i==position)
                                sliderPageIndicator.get(i).setChecked(true);
                            else
                                sliderPageIndicator.get(i).setChecked(false);
                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            };
    View.OnClickListener onDetectionButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            colorAnimator.pause();
            Toast.makeText(MainActivity.this,"DetectionActivity will be implemented soon",Toast.LENGTH_SHORT).show();
            Intent myintent = new Intent(MainActivity.this,DetectionActivity.class);
            MainActivity.this.startActivity(myintent);
        }
    };
//    View.OnClickListener onClassificationButtonClick = new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            Toast.makeText(MainActivity.this,"ClassificationActivity will be implemented soon",Toast.LENGTH_SHORT).show();
//            colorAnimator.pause();
//            Intent myintent = new Intent(MainActivity.this,ClassificationActivity.class);
//            MainActivity.this.startActivity(myintent);
//        }
//    };
}
