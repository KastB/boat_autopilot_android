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

package de.kast.android.autopilot.fragments;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import de.kast.android.autopilot.R;

public abstract class TextAndButtonsFragment extends MyFragment {
    // Layout Views
    protected MyTextView[] mTfs;
    protected MyButton[] mBts;

    TextAndButtonsFragment(int[] text_view_ids, int[] button_ids) {
        super();

        View.OnClickListener temporaryUnlockOnClickListener = new View.OnClickListener() {
            public void onClick(View v) {
                final ArrayList<MyButton> temporaryEnableButtons = new ArrayList<>();

                for (MyButton b: mBts) {
                    if (!b.mButton.isEnabled()) {
                        b.mButton.setEnabled(true);
                        temporaryEnableButtons.add(b);
                    }
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

        SparseArray<MyTextView> tfs = new SparseArray<>();
        tfs.put(R.id.goal, new MyTextView(R.id.goal,"%.0f", "m_goal"));
        tfs.put(R.id.error, new MyTextView(R.id.error,"%.0f", "m_lastError"));
        tfs.put(R.id.true_wind_speed, new MyTextView(R.id.true_wind_speed,"%.1f", "tws"));
        tfs.put(R.id.apparent_wind_angle, new MyTextView(R.id.apparent_wind_angle,"%.0f", "m_wind.apparentAngle"));
        tfs.put(R.id.gps_speed, new MyTextView(R.id.gps_speed,"%.1f", "gps_vel"));
        tfs.put(R.id.vmg, new MyTextView(R.id.vmg,"%.1f", "vmg"));
        tfs.put(R.id.yaw, new MyTextView(R.id.yaw,"%.0f", "yaw"));
        tfs.put(R.id.depth, new MyTextView(R.id.depth,"%.1f", "m_depth.depthBelowTransductor"));
        tfs.put(R.id.water_temp, new MyTextView(R.id.water_temp,"%.1f", "m_speed.waterTemp"));
        tfs.put(R.id.rudder_position, new MyTextView(R.id.rudder_position,"%.0f", "m_currentPosition"));
        tfs.put(R.id.integral, new MyTextView(R.id.integral,"%.0f", "m_errorSum"));
        tfs.put(R.id.roll, new MyTextView(R.id.roll,"%.1f", "roll"));
        tfs.put(R.id.pitch, new MyTextView(R.id.pitch,"%.1f", "pitch"));
        tfs.put(R.id.trip_milage, new MyTextView(R.id.trip_milage,"%.1f", "m_speed.tripMileage"));
        tfs.put(R.id.total_milage, new MyTextView(R.id.total_milage,"%.1f", "m_speed.totalMileage"));


        SparseArray<MyButton> bts = new SparseArray<>();
        bts.put(R.id.button_unlock, new MyButton(R.id.button_unlock, temporaryUnlockOnClickListener));
        bts.put(R.id.button_init, new MyButton(R.id.button_init, "I\r\n", false));
        bts.put(R.id.button_reinit, new MyButton(R.id.button_reinit, "RI\r\n", false));
        bts.put(R.id.button_goparking, new MyButton(R.id.button_goparking, "GP\r\n", false));
        bts.put(R.id.button_stop, new MyButton(R.id.button_stop, "S\r\n", true, "m_goalType", 0));
        bts.put(R.id.button_compass, new MyButton(R.id.button_compass, "M\r\n", true, "m_goalType", 2));
        bts.put(R.id.button_wind, new MyButton(R.id.button_wind, "W\r\n", true, "m_goalType", 1));
        bts.put(R.id.button_d10, new MyButton(R.id.button_d10, "D10\r\n"));
        bts.put(R.id.button_d, new MyButton(R.id.button_d, "D1\r\n"));
        bts.put(R.id.button_i, new MyButton(R.id.button_i, "I1\r\n"));
        bts.put(R.id.button_i10, new MyButton(R.id.button_i10, "I10\r\n"));

        mTfs = new MyTextView[text_view_ids.length];
        for(int i = 0; i < text_view_ids.length; i++) {
            mTfs[i] = tfs.get(text_view_ids[i]);
        }

        mBts = new MyButton[button_ids.length];
        for(int i = 0; i < button_ids.length; i++) {
            mBts[i] = bts.get(button_ids[i]);
        }

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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
    public void setData(String rawMessage, HashMap<String, Double> data, ArrayList<HashMap<String, Double>> history) {
        if (isVisible()) {
            for (MyTextView t : mTfs) {
                t.setText(data);
            }

            for (MyButton t : mBts) {
                t.updateEnabled(data);
            }
        }
    }
}
