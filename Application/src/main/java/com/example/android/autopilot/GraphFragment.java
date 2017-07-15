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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;


/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class GraphFragment extends MyFragment {


    protected static final String TAG = "GraphFragment";

    // Layout Views
    GraphView mGraph;
    LineGraphSeries<DataPoint> mGoalSeries;
    LineGraphSeries<DataPoint> mErrorSeries;
    LineGraphSeries<DataPoint> mWindDirectionSeries;
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
        mGoalSeries= new LineGraphSeries<>(new DataPoint[] {});
        mErrorSeries= new LineGraphSeries<>(new DataPoint[] {});
        mWindDirectionSeries= new LineGraphSeries<>(new DataPoint[] {});
        mSeriesValid = false;
        mGraph.addSeries(mGoalSeries);
        mGraph.addSeries(mWindDirectionSeries);
        DataPoint[] a = new DataPoint[] {
                new DataPoint(0, 1),
                new DataPoint(1, 5),
                new DataPoint(2, 3),
                new DataPoint(3, 2),
                new DataPoint(4, 6)};
        mWindDirectionSeries.resetData(a);
        //mGraph.getViewport().setScalable(true);
        //mGraph.getViewport().setScrollable(true);
    }
}
