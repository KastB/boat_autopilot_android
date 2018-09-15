/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.kast.android.autopilot;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class GraphFragment extends MyFragment {
    protected static final String TAG = "GraphFragment";
    static private final int mTimeHorizon = 120;
    static private final int mMaxDataPoints = 1024;

    // Layout Views
    GraphView mGraph;
    LineGraphSeries<DataPoint>[] mSeries;
    DataSet[] mDataSets;
    boolean mSeriesValid;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_graph, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mGraph = (GraphView) view.findViewById(R.id.graph);
    }

    @Override
    public void setData(String rawMessage, HashMap<String, Double> data, ArrayList<HashMap<String, Double>> history) {
        if (this.mSeriesValid) {
            double x = -1.0;
            for (int i = 0; i < this.mSeries.length; i++) {
                DataPoint p = new DataPoint(data.get("Millis") / 1000.0f, data.get(this.mDataSets[i].mIndex));
                x = p.getX();
                try {
                    this.mSeries[i].appendData(p, true, mMaxDataPoints);
                } catch (java.lang.IllegalArgumentException e) {
                    this.mSeriesValid = false;
                }
            }
            this.mGraph.getViewport().setXAxisBoundsManual(true);
            this.mGraph.getViewport().setMinX(x - mTimeHorizon);
            this.mGraph.getViewport().setMaxX(x + 2.0);
        } else {
            DataPoint[][] dataPoints = new DataPoint[this.mDataSets.length][history.size()];
            int counter = 0;
            for (HashMap<String, Double> h : history) {
                for (int z = 0; z < this.mDataSets.length; z++) {
                    dataPoints[z][counter] = new DataPoint(h.get("Millis") / 1000.0f, h.get(this.mDataSets[z].mIndex));
                }
                counter++;
            }
            this.mSeriesValid = true;
            for (int z = 0; z < this.mDataSets.length; z++) {
                try {
                    this.mSeries[z].resetData(dataPoints[z]);
                } catch (java.lang.IllegalArgumentException e) {
                    this.mSeriesValid = false;
                }
            }
        }
    }

    /**
     * Set up the UI and background operations for chat.
     */
    protected void setup() {
        mGraph.removeAllSeries();
        mGraph.getSecondScale().removeAllSeries();
        mDataSets = new DataSet[]{
                new DataSet("Ziel", "m_goal", Color.BLACK, false, 5, 3),
                new DataSet("Kurs", "yaw", Color.DKGRAY, false, 5, 3),
                new DataSet("Geschwindigkeit", "m_speed", Color.GREEN, false, 5, 3, true),
                new DataSet("Windrichtung", "m_wind.apparentAngle", Color.BLUE, false, 5, 3),
                new DataSet("Windgeschwindigkeit", "m_wind.apparentSpeed", Color.CYAN, false, 5, 3, true),
                new DataSet("Fehler", "m_lastError", Color.RED, false, 5, 3, true)
        };
        mSeries = new LineGraphSeries[mDataSets.length];
        for (int i = 0; i < mDataSets.length; i++) {
            mSeries[i] = new LineGraphSeries<>(new DataPoint[]{});
            mSeries[i].setTitle(mDataSets[i].mTitle);
            mSeries[i].setColor(mDataSets[i].mColor);
            mSeries[i].setDataPointsRadius(mDataSets[i].mPointRadius);
            mSeries[i].setDrawDataPoints(mDataSets[i].mDrawDataPoints);
            mSeries[i].setThickness(mDataSets[i].mThickness);
            if (mDataSets[i].mSecondScale) {
                mGraph.getSecondScale().addSeries(mSeries[i]);
            } else {
                mGraph.addSeries(mSeries[i]);
            }
        }
        // mGraph.getLegendRenderer().setVisible(true);
        mGraph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        mGraph.getSecondScale().setMinY(-20);
        mGraph.getSecondScale().setMaxY(20);
        mGraph.getGridLabelRenderer().setHorizontalLabelsAngle(90);

        mGraph.getGridLabelRenderer().setPadding(64);

        mGraph.getViewport().setScalable(true);

        mSeriesValid = false;


        mGraph.setOnTouchListener(new TouchListener(this));
    }

    private class TouchListener implements View.OnTouchListener {
        private final GraphFragment graphFragment;

        TouchListener(GraphFragment gf) {
            this.graphFragment = gf;
        }
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (this.graphFragment.mGraph.getLegendRenderer().isVisible()) {
                    this.graphFragment.mGraph.getLegendRenderer().setVisible(false);
                } else {
                    this.graphFragment.mGraph.getLegendRenderer().setVisible(true);
                }
            }
            return false;
        }
    }
}
