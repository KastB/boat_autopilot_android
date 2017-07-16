package com.example.android.autopilot;

import android.graphics.Color;

/**
 * Created by bernd on 15.07.17.
 */

public class DataSet {
    String mTitle;
    int mColor;
    boolean mDrawDataPoints;
    float mPointRadius;
    int mThickness;
    int mIndex;

    DataSet(String title, int index, int color, boolean drawDataPoints, float pointRadius, int thickness) {
        mTitle = title;
        mColor = color;
        mDrawDataPoints = drawDataPoints;
        mPointRadius = pointRadius;
        mThickness = thickness;
        mIndex = index;
    }
}
