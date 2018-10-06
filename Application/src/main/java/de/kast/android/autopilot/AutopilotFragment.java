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

public class AutopilotFragment extends MyFragment {

    protected static final String TAG = "AutopilotFragment";

    // Layout Views
    protected MyTextView[] mTfs;
    protected MyButton[] mBts;

    AutopilotFragment() {
        super();

        View.OnClickListener temporaryUnlockOnClickListener = new View.OnClickListener() {
            public void onClick(View v) {
                final ArrayList<MyButton> temporaryEnableButtons = new ArrayList<>();
                for (MyButton b: mBts) {
                    temporaryEnableButtons.add(b);
                }

                for (MyButton b: temporaryEnableButtons) {
                    b.mButton.setEnabled(true);
                }

                new CountDownTimer(3000, 10) {
                    public void onTick(long millisUntilFinished) {
                    }

                    @Override
                    public void onFinish() {
                        for (MyButton b: temporaryEnableButtons) {
                            b.mButton.setEnabled(false);
                        }
                    }
                }.start();
            }
        };

        mTfs = new MyTextView[] {
                new MyTextView(R.id.goal,"%.0f", "m_goal"),
                new MyTextView(R.id.error,"%.0f", "m_lastError"),
                new MyTextView(R.id.wind_speed,"%.1f", "tws"),
                new MyTextView(R.id.wind_direction,"%.0f", "m_wind.apparentAngle"),
                new MyTextView(R.id.speed,"%.1f", "gps_vel"),
                new MyTextView(R.id.heading,"%.0f", "yaw"),
                new MyTextView(R.id.depth,"%.1f", "m_depth.depthBelowTransductor"),
                new MyTextView(R.id.temperature,"%.1f", "m_speed.waterTemp"),
                new MyTextView(R.id.rudder_position,"%.0f", "m_currentPosition"),
                new MyTextView(R.id.integral,"%.0f", "m_errorSum"),
                new MyTextView(R.id.heel,"%.1f", "roll"),
                new MyTextView(R.id.pitch,"%.1f", "pitch"),
                new MyTextView(R.id.trip,"%.1f", "m_speed.tripMileage"),
                new MyTextView(R.id.total_milage,"%.1f", "m_speed.totalMileage")
        };


        mBts = new MyButton[] {
                new MyButton(R.id.button_unlock, temporaryUnlockOnClickListener),
                new MyButton(R.id.button_init, "I\r\n", false),
                new MyButton(R.id.button_reinit, "RI\r\n", false),
                new MyButton(R.id.button_goparking, "GP\r\n", false),
                new MyButton(R.id.button_stop, "S\r\n", true, "m_goalType", 0),
                new MyButton(R.id.button_compass, "M\r\n", true, "m_goalType", 2),
                new MyButton(R.id.button_wind, "W\r\n", true, "m_goalType", 1),
                new MyButton(R.id.button_d10, "D10\r\n"),
                new MyButton(R.id.button_d, "D1\r\n"),
                new MyButton(R.id.button_i, "I1\r\n"),
                new MyButton(R.id.button_i10, "I10\r\n"),
        };

    }

    @Override
    public View createView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_overview, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        for (MyTextView t: mTfs) {
            t.onViewCreated(view);
        }

        for (MyButton b: mBts) {
            b.onViewCreated(view);
        }
    }

    protected void setup() {
        for (MyButton b: mBts) {
            b.setup();
        }
    }

    @Override
    public boolean setData(String rawMessage, HashMap<String, Double> data, ArrayList<HashMap<String, Double>> history) {
        for (MyTextView t: mTfs) {
            t.setText(data);
        }

        for (MyButton t: mBts) {
            t.updateEnabled(data);
        }
        return true;
    }
}
