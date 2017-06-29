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
import android.content.Intent;
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

    private static final String TAG = "AutopilotFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

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
    private ListView mConversationView;
    private EditText mOutEditText;

    private TextView mGoalView;
    private TextView mErrorView;
    private TextView mWindSpeedView;
    private TextView mWindDirectionView;
    private TextView mSpeedView;
    private TextView mHeadingView;
    private TextView mDepthView;
    private TextView mTempView;

    // Buttons #1#
    private Button mSendButton;
    private Button mInitButton;
    private Button mGoParkingButton;
    private Button mPositionModeButton;
    private Button mCompassModeButton;
    private Button mWindModeButton;
    private Button mDecrease10Button;
    private Button mDecreaseButton;
    private Button mIncreaseButton;
    private Button mIncrease10Button;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private AutopilotService mChatService = null;

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
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == AutopilotService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_overview, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mConversationView = (ListView) view.findViewById(R.id.in);
        mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);

        mGoalView = (TextView) view.findViewById(R.id.goal);
        mErrorView = (TextView) view.findViewById(R.id.error);
        mWindSpeedView = (TextView) view.findViewById(R.id.wind_speed);
        mWindDirectionView = (TextView) view.findViewById(R.id.wind_direction);
        mSpeedView = (TextView) view.findViewById(R.id.speed);
        mHeadingView = (TextView) view.findViewById(R.id.heading);
        mDepthView = (TextView) view.findViewById(R.id.depth);
        mTempView = (TextView) view.findViewById(R.id.temperature);

        // connect #2#
        mSendButton = (Button) view.findViewById(R.id.button_send);
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
    private void setupChat() {
        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);

        mConversationView.setAdapter(mConversationArrayAdapter);


        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Init #3#

        // Initialize the send button with a listener that for click events
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                if (null != view) {
                    TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
                    String message = textView.getText().toString();
                    sendMessage(message);
                }
            }
        });
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


        // Initialize the AutopilotService to perform bluetooth connections
        mChatService = new AutopilotService(getActivity(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != AutopilotService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the AutopilotService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
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
    private void setStatus(CharSequence subTitle) {
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

    /**
     * The Handler that gets information back from the AutopilotService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case AutopilotService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case AutopilotService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case AutopilotService.STATE_LISTEN:
                        case AutopilotService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);

                    break;
                case Constants.MESSAGE_READ:
                    String readMessage = (String) msg.obj;
                    String[] parts = readMessage.split("\t");
                    if (parts.length >= 45)
                    {
                        mConversationArrayAdapter.add(readMessage);

                        mGoalView.setText(reducePrecision(parts[15],0));
                        mErrorView.setText(reducePrecision(parts[16], 0));
                        mWindSpeedView.setText(reducePrecision(parts[36],1));          //TODO: calculate/check true wind speed
                        mWindDirectionView.setText(reducePrecision(parts[35],0));
                        mSpeedView.setText(reducePrecision(parts[30],1));
                        mHeadingView.setText(reducePrecision(parts[20],0));
                        mDepthView.setText(reducePrecision(parts[42],1));
                        mTempView.setText(reducePrecision(parts[33],0));

                        if (parts[14].equals("0")) {
                            mPositionModeButton.setEnabled(false);
                            mCompassModeButton.setEnabled(true);
                            mWindModeButton.setEnabled(true);
                        } else if (parts[14].equals("1")) {
                            mPositionModeButton.setEnabled(true);
                            mCompassModeButton.setEnabled(true);
                            mWindModeButton.setEnabled(false);
                        } else if (parts[14].equals("2")) {
                            mPositionModeButton.setEnabled(true);
                            mCompassModeButton.setEnabled(false);
                            mWindModeButton.setEnabled(true);
                        }

                    }

                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };
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
                    setupChat();
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
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
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
