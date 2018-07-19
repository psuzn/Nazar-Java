package com.sujanpoudel.nazar;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.Log;
import android.widget.ImageView;

public class DetectionOverlayManager  {
    private ImageView imgOverlay;
    private  Canvas canvas;
    private Paint Rectpaint;
    TextPaint textPaint;
    private  int STROKE_WIDTH  =2;
    private  int touches = 0;
    int Padding = 5;

    DetectionOverlayManager(ImageView imgOverlay, int overlayWidth, int overlayHeight){
        this.imgOverlay = imgOverlay;
        Bitmap bitmap = Bitmap.createBitmap(overlayWidth, overlayHeight, Bitmap.Config.ARGB_4444);
        canvas =  new Canvas(bitmap);
        Rectpaint = new Paint();
        Rectpaint.setStrokeWidth(STROKE_WIDTH);
        Rectpaint.setColor(Color.BLUE);
        Rectpaint.setStyle(Paint.Style.STROKE);
        Rectpaint.setAntiAlias(true);

        textPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLUE);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.NORMAL));
        textPaint.setTextAlign(Paint.Align.LEFT);
        imgOverlay.setImageBitmap(bitmap);
        imgOverlay.bringToFront();
        //drawCircle(10,10,20);
    }

    public void  drawRectnagle(RectF rect ){
        Rectpaint.setColor(Color.BLUE);
        Log.d("none","drawing rectangle \n\n\n");
        canvas.drawRect(rect,Rectpaint);
        imgOverlay.draw(canvas);

    }
    public void  drawtText(String text,RectF r ){
        Rectpaint.setColor(Color.CYAN);
        textPaint.setTextSize(r.height() * 0.10f); //text size 10% of the rectangle height
        Log.d("none","drawing rectangle \n\n\n");
        canvas.drawText(text,r.left+Padding,r.bottom-Padding, textPaint);
        imgOverlay.draw(canvas);
    }
    public void  drawCircle(float x,float y,float r ){
        Log.d("none","drawing circle \n\n\n");
        canvas.drawCircle(x,y,r,Rectpaint);
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
    public void invalidate(){
        imgOverlay.invalidate();
    }

}
