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

package com.example.android.autopilot;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;


/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class GraphFragment extends MyFragment {


    protected static final String TAG = "GraphFragment";

    // Layout Views
    GraphView mGraph;
    LineGraphSeries<DataPoint>[] mSeries;
    DataSet[] mDataSets;
    boolean mSeriesValid;

    @Override
    public DataUpdateReceiverGraphFragment getNewDataUpdateReceiver() {
        return new DataUpdateReceiverGraphFragment(this);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_graph, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mGraph = (GraphView) view.findViewById(R.id.graph);
    }

    /**
     * Set up the UI and background operations for chat.
     */
    protected void setup() {
        mGraph.removeAllSeries();
        mGraph.getSecondScale().removeAllSeries();
        mDataSets = new DataSet[] {
                new DataSet("Ziel", 15, Color.BLACK, false, 5, 3),
                new DataSet("Kurs", 20, Color.DKGRAY, false, 5, 3),
                new DataSet("Geschwindigkeit", 30, Color.GREEN, false, 5, 3, true),
                new DataSet("Windrichtung", 35, Color.BLUE, false, 5, 3),
                new DataSet("Windgeschwindigkeit", 36, Color.CYAN, false, 5, 3, true),
                new DataSet("Fehler", 16, Color.RED, false, 5, 3, true)
        };
        mSeries = new LineGraphSeries[mDataSets.length];
        for(int i = 0; i < mDataSets.length; i++)
        {
            mSeries[i] = new LineGraphSeries<>(new DataPoint[]{});
            mSeries[i].setTitle(mDataSets[i].mTitle);
            mSeries[i].setColor(mDataSets[i].mColor);
            mSeries[i].setDataPointsRadius(mDataSets[i].mPointRadius);
            mSeries[i].setDrawDataPoints(mDataSets[i].mDrawDataPoints);
            mSeries[i].setThickness(mDataSets[i].mThickness);
            if (mDataSets[i].mSecondScale)
            {
                mGraph.getSecondScale().addSeries(mSeries[i]);
            }
            else {
                mGraph.addSeries(mSeries[i]);
            }
        }
        mGraph.getLegendRenderer().setVisible(true);
        mGraph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        mGraph.getSecondScale().setMinY(-20);
        mGraph.getSecondScale().setMaxY(20);
        mSeriesValid = false;
    }
}
