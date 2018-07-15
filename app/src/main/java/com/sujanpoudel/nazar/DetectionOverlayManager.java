package com.sujanpoudel.nazar;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.widget.ImageView;

public class DetectionOverlayManager  {
    private ImageView imgOverlay;
    private  Canvas canvas;
    private Paint paint;
    private  int STROKE_WIDTH  =2;
    private  int touches = 0;

    DetectionOverlayManager(ImageView imgOverlay, int overlayWidth, int overlayHeight){
        this.imgOverlay = imgOverlay;
        Bitmap bitmap = Bitmap.createBitmap(overlayWidth, overlayHeight, Bitmap.Config.ARGB_4444);
        canvas =  new Canvas(bitmap);
        paint = new Paint();
        paint.setStrokeWidth(STROKE_WIDTH);
        imgOverlay.setImageBitmap(bitmap);
        imgOverlay.bringToFront();
        //drawCircle(10,10,20);
    }

    public void  drawRectnagle(RectF rect ){
        Log.d("none","drawing rectangle \n\n\n");
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(rect,paint);
        imgOverlay.draw(canvas);

    }
    public void  drawCircle(float x,float y,float r ){
        Log.d("none","drawing circle \n\n\n");
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(x,y,r,paint);
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
