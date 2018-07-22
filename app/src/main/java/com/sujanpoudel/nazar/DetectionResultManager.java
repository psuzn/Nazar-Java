package com.sujanpoudel.nazar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Typeface;
import java.text.DecimalFormat;

import android.os.SystemClock;
import android.text.TextPaint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class DetectionResultManager {
    private ImageView imgOverlay;
    private  Canvas canvas;
    private Paint rectPaint;
    private TextPaint textPaint;
    private  int STROKE_WIDTH  =2;
    private  int touches = 0;
    private int detectionBoxTextPadding = 5;
    private LinearLayout detectionInfoLinkContainer;
    private ArrayList<List<Object>> InstantiatedDetectionInfoLinks = new ArrayList<>();//0th place contains the view,visible start time
    private List<Recognition> lastDetectionResult = new ArrayList<>();

    DetectionResultManager(ImageView imgOverlay, int overlayWidth, int overlayHeight,View DetectionInfoLinkContainer){
        this.imgOverlay = imgOverlay;
        detectionInfoLinkContainer = (LinearLayout) DetectionInfoLinkContainer;
        Bitmap bitmap = Bitmap.createBitmap(overlayWidth, overlayHeight, Bitmap.Config.ARGB_4444);
        canvas =  new Canvas(bitmap);

        //style for rectangle
        rectPaint = new Paint();
        rectPaint.setStrokeWidth(STROKE_WIDTH);
        rectPaint.setColor(Color.BLUE);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setAntiAlias(true);

        //style for text
        textPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLUE);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.NORMAL));
        textPaint.setTextAlign(Paint.Align.LEFT);
        imgOverlay.setImageBitmap(bitmap);
        //imgOverlay.bringToFront();
        //startDetectionInfoLinkremovalTimer();
    }

    public void  drawRectnagle(RectF rect ){
        rectPaint.setColor(Color.BLUE);
        Log.d("none","drawing rectangle \n\n\n");
        canvas.drawRect(rect, rectPaint);
        imgOverlay.draw(canvas);

    }
    public void handleDetectionResults( List<Recognition> results){
        if(DetectionActivity.detectionMode ==  DetectionActivity.DetectionMode.realtime)
            clear(); //clear the boxes each frame
        for ( Recognition r:results ) {
            drawRectnagle(r.getRect());
            String text = r.getClassName()+"("+new DecimalFormat("#.##").format(r.getConfidence())+")";
            drawtText(text,r.getRect());
            lastDetectionResult = results;
        }
    }
    public void  drawtText(String text,RectF r ){
        rectPaint.setColor(Color.CYAN);
        textPaint.setTextSize(r.height() * 0.10f); //text size 10% of the rectangle height
        Log.d("none","drawing rectangle \n\n\n");
        canvas.drawText(text,r.left+ detectionBoxTextPadding,r.bottom- detectionBoxTextPadding, textPaint);
        imgOverlay.draw(canvas);
    }
    public void  drawCircle(float x,float y,float r ){
        Log.d("none","drawing circle \n\n\n");
        canvas.drawCircle(x,y,r, rectPaint);
        imgOverlay.draw(canvas);
        touches++;
        if(touches>10)
        {
            this.clear();
            touches=0;
        }
    }

    public void clear(){
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
    }
    public void invalidate(Context c){
        imgOverlay.invalidate();
        removeInvisiblelinks();
        if(lastDetectionResult!=null)
        {
            detectionInfoLinkContainer.setVisibility(View.VISIBLE);
            for (Recognition r : lastDetectionResult){
                int index = alreadyExistInView(r.getClassId());
                if(index<0){ // not instantiated
                    final RelativeLayout view = (RelativeLayout) LayoutInflater.from(c).inflate(R.layout.detectioninfolinktemplate,null);
                    view.setId(r.getClassId());
                    ((TextView)view.getChildAt(0)).setText(r.getClassName());
                    detectionInfoLinkContainer.addView(view);
                    ArrayList<Object> tmpList = new ArrayList<>();
                    tmpList.add(view);
                    tmpList.add(SystemClock.uptimeMillis());
                    InstantiatedDetectionInfoLinks.add(tmpList);
                }
                else { //view contains the infolink of the current class
                    ((RelativeLayout)InstantiatedDetectionInfoLinks.get(index).get(0)).setVisibility(View.VISIBLE);
                    InstantiatedDetectionInfoLinks.get(index).set(1,SystemClock.uptimeMillis());
                }
            }
            lastDetectionResult = null;
        }


    }
    private  int alreadyExistInView( int classId ) {
        for (int i = 0; i < InstantiatedDetectionInfoLinks.size(); i++) {
            if ( ((RelativeLayout) InstantiatedDetectionInfoLinks.get(i).get(0)).getId() == classId) {
                return i;
            }
        }
        return -1;
    }
    public void removeInvisiblelinks(){
        if(DetectionActivity.detectionMode == DetectionActivity.DetectionMode.singleImage)
            return;
        for (int i = 0; i < InstantiatedDetectionInfoLinks.size(); i++) {
            long visibleDuration = SystemClock.uptimeMillis () - (long) InstantiatedDetectionInfoLinks.get(i).get(1) ;
            // Log.d();
            if( visibleDuration > 7000 && ((RelativeLayout)InstantiatedDetectionInfoLinks.get(i).get(0)).getVisibility() == View.VISIBLE )
            {
                ((RelativeLayout)InstantiatedDetectionInfoLinks.get(i).get(0)).setVisibility(View.GONE);
                Log.d("Nazar Debug", "visibility:"+visibleDuration+" removing: "+ ((RelativeLayout)InstantiatedDetectionInfoLinks.get(i).get(0)).getId());
            }
        }
    }
    public void  drawBitmap(Bitmap b){
        canvas.drawBitmap(b,0,0,null);
        //imgOverlay.setImageBitmap(b);
        imgOverlay.invalidate();
    }


}
