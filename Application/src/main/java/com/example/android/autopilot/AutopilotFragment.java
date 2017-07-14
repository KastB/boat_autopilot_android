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
public class AutopilotFragment extends Fragment {


    AutopilotFragment() {
        System.out.println("AutopilotFragment Constructor");
    }

    protected static final String TAG = "AutopilotFragment";

    // Intent request codes
    protected static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    protected static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    protected static final int REQUEST_ENABLE_BT = 3;

    /* By now there is a hard coded mapping between index of data incoming and the type of that data
        a better solution would probably be to ask for the current configuration, get an configuration
        list and work with that. The problem of this mechanism is, that the continously incoming
        data could corrupt the requested information. => Stopping of continuous information is needed
        beforehand.
        Mapping by now:
        Millis	m_currentPosition	m_pressedButtonDebug	m_bytesToSent	CurrentPosition	CurrentDirection	TargetPosition	MSStopped	startButton	stopButton	parkingButton	m_P	m_I	m_D	m_goalType	m_goal	m_lastError	m_errorSum	m_lastFilteredYaw	UI	yaw	pitch	roll	freq	magMin[0]	magMin[1]	magMin[2]	magMax[0]	magMax[1]	magMax[2]	m_speed	m_speed.tripMileage	m_speed.totalMileage	m_speed.waterTemp	m_lampIntensity	m_wind.apparentAngle	m_wind.apparentSpeed	m_wind.displayInKnots	m_wind.displayInMpS	m_depth.anchorAlarm	m_depth.deepAlarm	m_depth.defective	m_depth.depthBelowTransductor	m_depth.metricUnits	m_depth.shallowAlarm	m_depth.unknown	Position
        0: Millis
        1: m_currentPosition
        2: m_pressedButtonDebug
        3: m_bytesToSent
        4: CurrentPosition
        5: CurrentDirection
        6: TargetPosition
        7: MSStopped
        8: startButton
        9: stopButton
        10: parkingButton
        11: m_P
        12: m_I
        13: m_D
        14: m_goalType
        15: m_goal
        16: m_lastError
        17: m_errorSum
        18: m_lastFilteredYaw
        19: UI
        20: yaw
        21: pitch
        22: roll
        23: freq
        24: magMin[0]
        25: magMin[1]
        26: magMin[2]
        27: magMax[0]
        28: magMax[1]
        29: magMax[2]
        30: m_speed
        31: m_speed.tripMileage
        32: m_speed.totalMileage
        33: m_speed.waterTemp
        34: m_lampIntensity
        35: m_wind.apparentAngle
        36: m_wind.apparentSpeed
        37: m_wind.displayInKnots
        38: m_wind.displayInMpS
        39: m_depth.anchorAlarm
        40: m_depth.deepAlarm
        41: m_depth.defective
        42: m_depth.depthBelowTransductor
        43: m_depth.metricUnits
        44: m_depth.shallowAlarm
        45: m_depth.unknown
        46: Position
     */
    // Layout Views



    protected TextView mGoalView;
    protected TextView mErrorView;
    protected TextView mWindSpeedView;
    protected TextView mWindDirectionView;
    protected TextView mSpeedView;
    protected TextView mHeadingView;
    protected TextView mDepthView;
    protected TextView mTempView;

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

    /**
     * Name of the connected device
     */
    protected String mConnectedDeviceName = null;

    /**
     * String buffer for outgoing messages
     */
    protected StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    protected BluetoothAdapter mBluetoothAdapter = null;

    DataUpdateReceiverAutopilotFragment mDataUpdateReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }



    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            if (mDataUpdateReceiver != null) {
                getContext().unregisterReceiver(mDataUpdateReceiver);
            }
        }
        catch (java.lang.IllegalArgumentException e) {

        }
    }
    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            setup();
        }
        if (mDataUpdateReceiver == null)
            mDataUpdateReceiver = new DataUpdateReceiverAutopilotFragment(this);
        IntentFilter intentFilter = new IntentFilter(AutopilotService.AUTOPILOT_INTENT);
        getContext().registerReceiver(mDataUpdateReceiver, intentFilter);
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
/*
        mDecreaseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendMessage("348050\t109\t0\t0\t109\t1\t109\t50456\t1\t1\t1\t4.00\t0.20\t3.00\t0\t240.00\t1.00\t196.54\t223.20\t229.52\t4.62\t9.73\t51.40\t-69.00\t-48.00\t-220.00\t123.00\t144.00\t55.00\t8.30\t50.00\t50.00\t21.00\t-1\t289.00\t1.70\t1\t0\t0\t0\t0\t10.70\t1\t0\t1\r\n");
            }
        });
        mIncreaseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendMessage("348050\t109\t0\t0\t109\t1\t109\t50456\t1\t1\t1\t4.00\t0.20\t3.00\t0\t245.00\t250.0\t196.54\t223.20\t229.52\t4.62\t9.73\t51.40\t-69.00\t-48.00\t-220.00\t123.00\t144.00\t55.00\t8.30\t50.00\t50.00\t21.00\t-1\t289.00\t1.70\t1\t0\t0\t0\t0\t10.70\t1\t0\t1\r\n");
            }
        });
        mIncrease10Button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendMessage("348050\t109\t0\t0\t109\t1\t109\t50456\t1\t1\t1\t4.00\t0.20\t3.00\t0\t250.00\t260.00\t196.54\t223.20\t229.52\t4.62\t9.73\t51.40\t-69.00\t-48.00\t-220.00\t123.00\t144.00\t55.00\t8.30\t50.00\t50.00\t21.00\t-1\t289.00\t1.70\t1\t0\t0\t0\t0\t10.70\t1\t0\t1\r\n");
            }
        });
*/


        // Initialize the AutopilotService to perform bluetooth connections
//        mAutopilotService = new AutopilotService(getActivity(), mAutopilotHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    protected void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (AutopilotService.getInstance() == null)
            return;
        if (AutopilotService.getInstance().getState() != AutopilotService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the AutopilotService to write
            byte[] send = message.getBytes();
            AutopilotService.getInstance().write(send);
        }
    }
    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    protected void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    protected void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    public String reducePrecision(String str, int prec) {
        if(prec < 1) {
            if(str.contains("."))
                return str.substring(0,str.indexOf("."));
            else
                return str;
        }

        //untested
        if(!str.contains("."))
            str.concat(".");
        while(str.length() - str.indexOf(".") < prec + 1)
            str.concat("0");
        return str.substring(0,str.indexOf(".") + prec + 1);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setup();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    protected void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        if (AutopilotService.getInstance() == null)
            return;
        AutopilotService.getInstance().connect(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
        }
        return false;
    }

}
