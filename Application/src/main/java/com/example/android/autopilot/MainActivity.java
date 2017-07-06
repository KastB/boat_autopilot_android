/*
* Copyright 2013 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


package com.example.android.autopilot;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.Menu;

import com.example.android.common.activities.SampleActivityBase;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple launcher activity containing a summary sample description, sample log and a custom
 * {@link android.support.v4.app.Fragment} which can display a view.
 * <p>
 * For devices with displays with a width of 720dp or greater, the sample log is always visible,
 * on other devices it's visibility is controlled by an item on the Action Bar.
 */
public class MainActivity extends SampleActivityBase {

    public static final String TAG = "MainActivity";
    private AutopilotService mAutopilotService = null;
    private AutopilotFragment mAutopilotFragment = null;
    private DebugFragment mDebugFragment = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private ViewPager mViewPager;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    MainActivity() {
        System.out.println("MainActivity Constructor");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mAutopilotFragment == null)
            mAutopilotFragment = new AutopilotFragment();
        if (mDebugFragment == null)
            mDebugFragment = new DebugFragment();
        if (mAutopilotService == null)
            mAutopilotService = new AutopilotService(getApplicationContext(), mAutopilotFragment.getHandler(), mDebugFragment.getHandler());

        if (savedInstanceState == null) {

            mViewPager = (ViewPager) findViewById(R.id.view_pager);
            List<Fragment> fragments = new ArrayList<Fragment>();
            fragments.add(mAutopilotFragment);
            fragments.add(mDebugFragment);
            SwipeAdaptor swipeAdaptor = new SwipeAdaptor(getSupportFragmentManager(), fragments);
            mViewPager.setAdapter(swipeAdaptor);
            /*FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();



            transaction.replace(R.id.sample_content_fragment, mAutopilotFragment);
            transaction.commit();*/
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mAutopilotService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mAutopilotService.getState() == AutopilotService.STATE_NONE) {
                // Start the Bluetooth chat services
                mAutopilotService.start();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mAutopilotService == null) {
            if (mAutopilotFragment == null)
                mAutopilotFragment = new AutopilotFragment();
            mAutopilotService = new AutopilotService(this, mAutopilotFragment.getHandler(), mDebugFragment.getHandler());
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public AutopilotService getAutopilotService() {
        return mAutopilotService;
    }

public void onDestroy() {
        super.onDestroy();
        if (mAutopilotService != null) {
            mAutopilotService.stop();
        }
    }

}
