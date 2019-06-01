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


package de.kast.android.autopilot;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Menu;

import de.kast.android.autopilot.service.AutopilotService;
import de.kast.android.common.activities.SampleActivityBase;

/**
 * A simple launcher activity containing a summary sample description, sample log and a custom
 * {@link android.support.v4.app.Fragment} which can display a view.
 * <p>
 * For devices with displays with a width of 720dp or greater, the sample log is always visible,
 * on other devices it's visibility is controlled by an item on the Action Bar.
 */
public class MainActivity extends SampleActivityBase {

    public MainActivity() {
        System.out.println("MainActivity Constructor");
    }

    private void start() {
        setWifiInterfaceAsDefault();
        if (AutopilotService.getInstance() == null) {
            startService(new Intent(this, AutopilotService.class));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start();

        // Intent request codes
        ViewPager mViewPager = (ViewPager) findViewById(R.id.view_pager);
        SwipeAdaptor swipeAdaptor = new SwipeAdaptor(getSupportFragmentManager());
        mViewPager.setAdapter(swipeAdaptor);
    }

    @Override
    public void onResume() {
        super.onResume();
        start();
    }

    @Override
    public void onStart() {
        super.onStart();
        start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        getMenuInflater().inflate(R.menu.buttons, menu);
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            menu.findItem(R.id.secure_connect_scan).setEnabled(false);
            menu.findItem(R.id.secure_connect_scan).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setWifiInterfaceAsDefault() {
        try {
            ConnectivityManager connection_manager =
                    (ConnectivityManager) getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkRequest.Builder request = new NetworkRequest.Builder();
            request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);

            connection_manager.registerNetworkCallback(request.build(), new ConnectivityManager.NetworkCallback() {

                @Override
                public void onAvailable(Network network) {
                    ConnectivityManager.setProcessDefaultNetwork(network);
                }
            });
        }
        catch(java.lang.NoClassDefFoundError ignore) {
        }
    }
}
