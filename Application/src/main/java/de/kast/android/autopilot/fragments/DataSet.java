package de.kast.android.autopilot.fragments;

/**
 * Created by bernd on 15.07.17.
 */

class DataSet {
    String mTitle;
    int mColor;
    boolean mDrawDataPoints;
    float mPointRadius;
    int mThickness;
    String mIndex;
    boolean mSecondScale;

    DataSet(String title, String index, int color, boolean drawDataPoints, float pointRadius, int thickness) {
        this(title, index, color, drawDataPoints, pointRadius, thickness, false);
    }

    DataSet(String title, String index, int color, boolean drawDataPoints, float pointRadius, int thickness, boolean secondScale) {
        mTitle = title;
        mColor = color;
        mDrawDataPoints = drawDataPoints;
        mPointRadius = pointRadius;
        mThickness = thickness;
        mIndex = index;
        mSecondScale = secondScale;
    }
}
