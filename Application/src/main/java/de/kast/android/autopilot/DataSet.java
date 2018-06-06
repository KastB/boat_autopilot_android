package de.kast.android.autopilot;

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
    boolean mSecondScale;

    DataSet(String title, int index, int color, boolean drawDataPoints, float pointRadius, int thickness) {
        DataSet(title, index, color, drawDataPoints, pointRadius, thickness, false);
    }

    DataSet(String title, int index, int color, boolean drawDataPoints, float pointRadius, int thickness, boolean secondScale) {
        DataSet(title, index, color, drawDataPoints, pointRadius, thickness, secondScale);
    }

    private void DataSet(String title, int index, int color, boolean drawDataPoints, float pointRadius, int thickness, boolean secondScale) {
        mTitle = title;
        mColor = color;
        mDrawDataPoints = drawDataPoints;
        mPointRadius = pointRadius;
        mThickness = thickness;
        mIndex = index;
        mSecondScale = secondScale;
    }
}
