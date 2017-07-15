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

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class AutopilotFragment extends MyFragment {


    protected static final String TAG = "AutopilotFragment";

    // Layout Views
    protected TextView mGoalView;
    protected TextView mErrorView;
    protected TextView mWindSpeedView;
    protected TextView mWindDirectionView;
    protected TextView mSpeedView;
    protected TextView mHeadingView;
    protected TextView mDepthView;
    protected TextView mTempView;
    protected TextView mIntegralView;
    protected TextView mRudderView;

    // Buttons #1#
    protected Button mInitButton;
    protected Button mGoParkingButton;
    protected Button mPositionModeButton;
    protected Button mCompassModeButton;
    protected Button mWindModeButton;
    protected Button mDecrease10Button;
    protected Button mDecreaseButton;
    protected Button mIncreaseButton;
    protected Button mIncrease10Button;

    @Override
    public DataUpdateReceiverAutopilotFragment getNewDataUpdateReceiver() {
        return new DataUpdateReceiverAutopilotFragment(this);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_overview, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mGoalView = (TextView) view.findViewById(R.id.goal);
        mErrorView = (TextView) view.findViewById(R.id.error);
        mWindSpeedView = (TextView) view.findViewById(R.id.wind_speed);
        mWindDirectionView = (TextView) view.findViewById(R.id.wind_direction);
        mSpeedView = (TextView) view.findViewById(R.id.speed);
        mHeadingView = (TextView) view.findViewById(R.id.heading);
        mDepthView = (TextView) view.findViewById(R.id.depth);
        mTempView = (TextView) view.findViewById(R.id.temperature);
        mRudderView = (TextView) view.findViewById(R.id.rudder_position);
        mIntegralView = (TextView) view.findViewById(R.id.integral);

        // connect #2#
        mInitButton = (Button) view.findViewById(R.id.button_init);
        mGoParkingButton = (Button) view.findViewById(R.id.button_goparking);
        mPositionModeButton = (Button) view.findViewById(R.id.button_stop);
        mCompassModeButton = (Button) view.findViewById(R.id.button_compass);
        mWindModeButton = (Button) view.findViewById(R.id.button_wind);
        mDecrease10Button = (Button) view.findViewById(R.id.button_d10);
        mDecreaseButton = (Button) view.findViewById(R.id.button_d);
        mIncreaseButton = (Button) view.findViewById(R.id.button_i);
        mIncrease10Button = (Button) view.findViewById(R.id.button_i10);

    }

    /**
     * Set up the UI and background operations for chat.
     */
    protected void setup() {
        // Initialize the send button with a listener that for click events
        mGoParkingButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendMessage("GP\r\n");
            }
        });
        mInitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendMessage("I\r\n");
            }
        });
        mPositionModeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendMessage("S\r\n");
            }
        });
        mCompassModeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendMessage("M\r\n");
            }
        });
        mWindModeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendMessage("W\r\n");
            }
        });
        mDecrease10Button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendMessage("D10\r\n");
            }
        });
        mDecreaseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendMessage("D1\r\n");
            }
        });
        mIncreaseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendMessage("I1\r\n");
            }
        });
        mIncrease10Button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendMessage("I10\r\n");
            }
        });
    }
}
