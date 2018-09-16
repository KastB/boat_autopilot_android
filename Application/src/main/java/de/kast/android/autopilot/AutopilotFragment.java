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

import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

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
    protected TextView mPitchView;
    protected TextView mHeelView;
    protected  TextView mTripView;
    protected TextView mTotalView;

    // Buttons #1#
    protected Button mInitButton;
    protected Button mReInitButton;
    protected Button mUnlockButton;
    protected Button mGoParkingButton;
    protected Button mPositionModeButton;
    protected Button mCompassModeButton;
    protected Button mWindModeButton;
    protected Button mDecrease10Button;
    protected Button mDecreaseButton;
    protected Button mIncreaseButton;
    protected Button mIncrease10Button;


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
        mHeelView = (TextView) view.findViewById(R.id.heel);
        mPitchView = (TextView) view.findViewById(R.id.pitch);
        mTripView = (TextView) view.findViewById(R.id.trip);
        mTotalView = (TextView) view.findViewById(R.id.total_milage);

        // connect #2#
        mInitButton = (Button) view.findViewById(R.id.button_init);
        mReInitButton = (Button) view.findViewById(R.id.button_reinit);
        mUnlockButton = (Button) view.findViewById(R.id.button_unlock);
        mGoParkingButton = (Button) view.findViewById(R.id.button_goparking);
        mPositionModeButton = (Button) view.findViewById(R.id.button_stop);
        mCompassModeButton = (Button) view.findViewById(R.id.button_compass);
        mWindModeButton = (Button) view.findViewById(R.id.button_wind);
        mDecrease10Button = (Button) view.findViewById(R.id.button_d10);
        mDecreaseButton = (Button) view.findViewById(R.id.button_d);
        mIncreaseButton = (Button) view.findViewById(R.id.button_i);
        mIncrease10Button = (Button) view.findViewById(R.id.button_i10);

        mInitButton.setEnabled(false);
        mReInitButton.setEnabled(false);
        mGoParkingButton.setEnabled(false);
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
        mReInitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendMessage("RI\r\n");
            }
        });
        mUnlockButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mInitButton.setEnabled(true);
                mReInitButton.setEnabled(true);
                mGoParkingButton.setEnabled(true);

                new CountDownTimer(3000, 10) {
                    public void onTick(long millisUntilFinished) {
                    }

                    @Override
                    public void onFinish() {
                        mInitButton.setEnabled(false);
                        mReInitButton.setEnabled(false);
                        mGoParkingButton.setEnabled(false);
                    }
                }.start();

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

    @Override
    public boolean setData(String rawMessage, HashMap<String, Double> data, ArrayList<HashMap<String, Double>> history) {
        this.mGoalView.setText(String.format("%.0f", data.get("m_goal")));
        this.mErrorView.setText(String.format("%.0f", data.get("m_lastError")));
        this.mWindSpeedView.setText(String.format("%.1f", data.get("m_wind.apparentSpeed")));
        this.mWindDirectionView.setText(String.format("%.0f", data.get("m_wind.apparentAngle")));
        this.mSpeedView.setText(String.format("%.1f", data.get("m_speed")));
        this.mHeadingView.setText(String.format("%.0f", data.get("yaw")));
        this.mDepthView.setText(String.format("%.1f", data.get("m_depth.depthBelowTransductor")));
        this.mTempView.setText(String.format("%.1f", data.get("m_speed.waterTemp")));
        this.mRudderView.setText(String.format("%.0f", data.get("m_currentPosition")));
        this.mIntegralView.setText(String.format("%.0f", data.get("m_errorSum")));
        this.mHeelView.setText(String.format("%.1f", data.get("roll")));
        this.mPitchView.setText(String.format("%.1f", data.get("pitch")));
        switch (data.get("m_goalType").intValue()) {
            case 0:
                this.mPositionModeButton.setEnabled(false);
                this.mCompassModeButton.setEnabled(true);
                this.mWindModeButton.setEnabled(true);
                break;
            case 1:
                this.mPositionModeButton.setEnabled(true);
                this.mCompassModeButton.setEnabled(true);
                this.mWindModeButton.setEnabled(false);
                break;
            case 2:
                this.mPositionModeButton.setEnabled(true);
                this.mCompassModeButton.setEnabled(false);
                this.mWindModeButton.setEnabled(true);
                break;
        }
        return true;
    }
}
