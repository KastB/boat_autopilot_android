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


import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.util.UUID;




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

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class AutopilotService extends Service {
    // Debugging
    protected static final String TAG = "AutopilotService";

    // Name for the SDP record when creating server socket
    protected static final String NAME_SECURE = "BluetoothChatSecure";
    protected static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
    protected static final UUID MY_UUID_SECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    protected static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static final String AUTOPILOT_INTENT = "autopilot_intent";

    protected static int mRingBufferSize = 30;

    MyBuffer mBuf;

    // Member fields
    protected final BluetoothAdapter mAdapter;
    protected ConnectThread mConnectThread;
    protected ConnectedThread mConnectedThread;
    protected int mState;
    protected int mNewState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device


    // https://stackoverflow.com/questions/2463175/how-to-have-android-service-communicate-with-activity
    protected static AutopilotService sInstance;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     */
    public AutopilotService() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mNewState = mState;
        System.out.println("AutopilotService Constructor");
        mBuf = new MyBuffer(mRingBufferSize);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Update UI title according to the current state of the chat connection
     */
    protected synchronized void updateUserInterfaceTitle() {
        mState = getState();
        mNewState = mState;

        // Give the new state to the Handler so the UI Activity can update
        // TODO:
        // mAutopilotHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
        Intent in = new Intent(AutopilotService.AUTOPILOT_INTENT);
        in.setAction(AutopilotService.AUTOPILOT_INTENT);
        in.putExtra("intentType", Constants.MESSAGE_STATE_CHANGE);
        in.putExtra(Integer.toString(Constants.MESSAGE_STATE_CHANGE), mNewState);
        sendBroadcast(in);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    @Override
    public void onDestroy() {
        stop();
        sInstance = null;
        super.onDestroy();
    }


    static AutopilotService getInstance() {
        return sInstance;
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        //TODO
        /*
        Message msg = mAutopilotHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mAutopilotHandler.sendMessage(msg);*/
        Intent in = new Intent(AutopilotService.AUTOPILOT_INTENT);
        in.setAction(AutopilotService.AUTOPILOT_INTENT);
        in.putExtra("intentType", Constants.MESSAGE_DEVICE_NAME);
        in.putExtra(Integer.toString(Constants.MESSAGE_DEVICE_NAME), device.getName());
        sendBroadcast(in);

        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    protected void connectionFailed() {
        // Send a failure message back to the Activity
        //TODO
        /*
        Message msg = mAutopilotHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mAutopilotHandler.sendMessage(msg);
*/
        Intent in = new Intent(AutopilotService.AUTOPILOT_INTENT);
        in.setAction(AutopilotService.AUTOPILOT_INTENT);
        in.putExtra("intentType", Constants.TOAST);
        in.putExtra(Constants.TOAST, "Unable to connect device");
        sendBroadcast(in);


        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        AutopilotService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    protected void connectionLost() {
        // Send a failure message back to the Activity
        //TODO
        /*
        Message msg = mAutopilotHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mAutopilotHandler.sendMessage(msg);
*/
        Intent in = new Intent(AutopilotService.AUTOPILOT_INTENT);
        in.setAction(AutopilotService.AUTOPILOT_INTENT);
        in.putExtra("intentType", Constants.TOAST);
        in.putExtra(Constants.TOAST, "Device connection was lost");
        sendBroadcast(in);
        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        AutopilotService.this.start();
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    protected class ConnectThread extends Thread {
        protected final BluetoothSocket mmSocket;
        protected final BluetoothDevice mmDevice;
        protected String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID_INSECURE);
                }
            } catch (IOException e) {
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (AutopilotService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    protected class ConnectedThread extends Thread {
        protected final BluetoothSocket mmSocket;
        protected final InputStream mmInStream;
        protected final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            String readMessage = "";
            String message;
            int index = -1;
            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    readMessage = readMessage.concat(new String(buffer, 0, bytes));
                    index = readMessage.indexOf("\r");
                    if (index != -1) {
                        String tmp = readMessage.substring(0,index);
                        int index2 = tmp.indexOf("\n");
                        if (index2 != -1)
                        {
                            tmp = tmp.substring(index2+1, tmp.length());
                        }
                        //TODO
                        /*mAutopilotHandler.obtainMessage(Constants.MESSAGE_READ,readMessage.substring(0,index))
                                .sendToTarget();*/
                        Intent in = new Intent(AutopilotService.AUTOPILOT_INTENT);
                        //in.setAction(Integer.toString(Constants.MESSAGE_READ));
                        in.setAction(AUTOPILOT_INTENT);
                        in.putExtra("intentType", Constants.MESSAGE_READ);
                        in.putExtra(Integer.toString(Constants.MESSAGE_READ), tmp);


                        mBuf.add(tmp);
                        String[] s = mBuf.getAll();
                        in.putExtra("History", s);
                        sendBroadcast(in);

                        if (index + 1 < readMessage.length())
                            readMessage = readMessage.substring(index+1, readMessage.length());
                        else
                            readMessage = "";
                    }
                    else {
                        index = readMessage.indexOf('\n');
                        if (index > 0) {
                            //TODO
                            /*
                            mAutopilotHandler.obtainMessage(Constants.MESSAGE_READ, readMessage.substring(0, index))
                                    .sendToTarget();*/
                            Intent in = new Intent(AutopilotService.AUTOPILOT_INTENT);
                            in.setAction(AutopilotService.AUTOPILOT_INTENT);
                            in.putExtra("intentType", Constants.MESSAGE_READ);
                            in.putExtra(Integer.toString(Constants.MESSAGE_READ), readMessage.substring(0,index));

                            mBuf.add(readMessage.substring(0,index));
                            String[] s = mBuf.getAll();
                            in.putExtra("History", s);
                            sendBroadcast(in);

                            if (index + 1 < readMessage.length())
                                readMessage = readMessage.substring(index+1, readMessage.length());
                            else
                                readMessage = "";
                        }

                    }
                } catch (IOException e) {
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                //TODO
                /*
                mAutopilotHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();*/
                Intent in = new Intent(AutopilotService.AUTOPILOT_INTENT);
                in.setAction(AutopilotService.AUTOPILOT_INTENT);
                in.putExtra("intentType", Constants.MESSAGE_WRITE);
                in.putExtra(Integer.toString(Constants.MESSAGE_WRITE), buffer);
            } catch (IOException e) {
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }
}
