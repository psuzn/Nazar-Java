package com.sujanpoudel.nazar;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Debug;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
            Toast.makeText(MainActivity.this,"Detection will be implemented soon",Toast.LENGTH_SHORT).show();
            Intent myintent = new Intent(MainActivity.this,Detection.class);
            MainActivity.this.startActivity(myintent);
        }
    };
    View.OnClickListener onClassificationButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(MainActivity.this,"Classification will be implemented soon",Toast.LENGTH_SHORT).show();
        }
    };
    ArrayList<RadioButton>  sliderPageIndicator = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);
        startColorAnimation();
        WizardPagerAdapter adapter = new WizardPagerAdapter();
        ViewPager pager = findViewById(R.id.pager);
        pager.setAdapter(adapter);

        pager.addOnPageChangeListener(onPageChangeListner);
        findViewById(R.id.startClassification).setOnClickListener(onClassificationButtonClick);
        findViewById(R.id.startDetection).setOnClickListener(onDetectionButtonClick);

        //for the buttom radiobuttons
        RadioGroup radioButtonGroup = findViewById(R.id.pageIndicator);
        int childs = radioButtonGroup.getChildCount();
        for(int i = 0;i <= childs;i++){
            View o  = radioButtonGroup.getChildAt(i);
            if(o instanceof RadioButton)
                sliderPageIndicator.add((RadioButton) o);
        }

    }
    void startColorAnimation(){
        final ImageView blurPreview = findViewById(R.id.colorOverlay);
        ValueAnimator anim = ValueAnimator.ofFloat(0, 1);   // animate from 0 to 1
        anim.setDuration(8000);                              // for 8000 ms
        anim.setRepeatMode(ValueAnimator.REVERSE);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override public void onAnimationUpdate(ValueAnimator animation) {
                blurPreview.setBackgroundColor(Color.HSVToColor(new float[]{ animation.getAnimatedFraction() * 360 , 0.7f,0.7f}));
                blurPreview.setAlpha(0.7f);
            }
        });
        anim.start();
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
        Log.d("Nazar debug","resumming");
    }
}
