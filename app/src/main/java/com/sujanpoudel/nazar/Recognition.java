package com.sujanpoudel.nazar;

import android.graphics.RectF;
import android.support.annotation.NonNull;

public class Recognition {
    private  int classId;
    private RectF rect;
    private float accuracy;
    private String className;

    public Recognition(int classId, RectF rect, float accuracy, String className) {
        this.classId = classId;
        this.rect = rect;
        this.accuracy = accuracy;
        this.className = className;
    }

    public int getClassId() {
        return classId;
    }

    public void setRect(RectF rect) {
        this.rect = rect;
    }

    public RectF getRect() {
        return rect;

    }

    public float getConfidence() {
        return accuracy;
    }

    public String getClassName() {
        return className;
    }

}
