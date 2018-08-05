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


import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DateFormat;
import java.util.Date;
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
        11: diagA
        12: diagB
        13: m_P
        14: m_I
        15: m_D
        16: m_goalType
        17: m_goal
        18: m_lastError
        19: m_errorSum
        20: m_lastFilteredYaw
        21: UI
        22: yaw
        23: pitch
        24: roll
        25: freq
        26: magMin[0]
        27: magMin[1]
        28: magMin[2]
        29: magMax[0]
        30: magMax[1]
        31: magMax[2]
        32: m_speed
        33: m_speed.tripMileage
        34: m_speed.totalMileage
        35: m_speed.waterTemp
        36: m_lampIntensity
        37: m_wind.apparentAngle
        38: m_wind.apparentSpeed
        39: m_wind.displayInKnots
        40: m_wind.displayInMpS
        41: m_depth.anchorAlarm
        42: m_depth.deepAlarm
        43: m_depth.defective
        44: m_depth.depthBelowTransductor
        45: m_depth.metricUnits
        46: m_depth.shallowAlarm
        47: m_depth.unknown
        48: Position
     */

interface WritableThread {
    void write(byte[] buffer);

    void cancel();

    void start();
}

/**
 * This class does all the work for setting up and managing Bluetooth and Network
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class AutopilotService extends Service {

    public static final String AUTOPILOT_INTENT = "autopilot_intent";
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN_BT = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING_BT = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED_BT = 3;  // now connected to a remote device
    public static final int STATE_CONNECTING_TCP = 4; // now initiating an outgoing connection
    public static final int STATE_CONNECTED_TCP = 5;  // now connected to a remote device
    public static final int BT = 6;
    public static final int TCP = 7;
    // Unique UUID for this application
    protected static final UUID MY_UUID_SECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    protected static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    protected static int mRingBufferSize = 1024;
    // https://stackoverflow.com/questions/2463175/how-to-have-android-service-communicate-with-activity
    protected static AutopilotService sInstance;
    // Member fields
    protected final BluetoothAdapter mAdapterBt;
    protected ConnectedThreadBt mConnectedThreadBt;
    protected WritableThread mConnectThread;
    protected int mState;
    protected int mOldState;
    protected String mDeviceName;
    MyBuffer mBuf;
    private String mLastIp;
    private int mLastPort;
    private BluetoothDevice mLastDevice;
    private int mLastType = STATE_NONE;
    private File mLogFile;
    private FileOutputStream mLogFileStream;

    /**
     * Constructor. Prepares a new sessions.
     */
    public AutopilotService() {
        mAdapterBt = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mOldState = mState;
        System.out.println("AutopilotService Constructor");
        mBuf = new MyBuffer(mRingBufferSize);
        mDeviceName = "";
        mLogFile = null;
        mLogFileStream = null;
    }

    static AutopilotService getInstance() {
        return sInstance;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    /**
     * Update UI title according to the current state of the chat connection
     */
    protected synchronized void updateUserInterfaceTitle() {
        Intent in = new Intent(AutopilotService.AUTOPILOT_INTENT);
        in.putExtra("intentType", Constants.MESSAGE_DEVICE_NAME);
        in.putExtra(Integer.toString(Constants.MESSAGE_DEVICE_NAME), mDeviceName);
        LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(in);

        in = new Intent(AutopilotService.AUTOPILOT_INTENT);
        in.putExtra("intentType", Constants.MESSAGE_STATE_CHANGE);
        in.putExtra(Integer.toString(Constants.MESSAGE_STATE_CHANGE), mState);
        LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(in);

        in = new Intent(AutopilotService.AUTOPILOT_INTENT);
        if (mState == STATE_CONNECTED_BT || mState == STATE_CONNECTED_TCP) {
            in.putExtra("intentType", Constants.MESSAGE_DEVICE_NAME);
            in.putExtra(Integer.toString(Constants.MESSAGE_DEVICE_NAME), mDeviceName);
        } else if (mState == STATE_CONNECTING_BT || mState == STATE_CONNECTING_TCP) {
            in.putExtra("intentType", Constants.MESSAGE_TOAST);
            in.putExtra(Integer.toString(Constants.MESSAGE_TOAST), "connecting to " + mDeviceName);
        } else {
            in.putExtra("intentType", Constants.MESSAGE_TOAST);
            in.putExtra(Integer.toString(Constants.MESSAGE_TOAST), "disconnected");
        }
        LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(in);
        mOldState = mState;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        File dir = new File (Environment.getExternalStorageDirectory().getAbsolutePath() + "/Autopilot");
        dir.mkdirs();
        mLogFile = new File(dir, "autopilot_" + DateFormat.getDateTimeInstance().format(new Date()) + ".log");
        try {
            mLogFileStream = new FileOutputStream(mLogFile,true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            mLogFileStream = null;
        }
    }

    @Override
    public void onDestroy() {
        stop();
        if(mLogFileStream != null) {
            try {
                mLogFileStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mLogFileStream = null;
        }
        sInstance = null;
        super.onDestroy();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    private void cancel() {
        mState = STATE_NONE;
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThreadBt != null) {
            mConnectedThreadBt.cancel();
            mConnectedThreadBt = null;
        }
        updateUserInterfaceTitle();
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        this.cancel();
        if (this.mConnectThread != null) {
            this.mConnectThread.start();
        }
    }

    /**
     * Start the ConnectThreadBt to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connectBt
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connectBt(BluetoothDevice device, boolean secure) {
        this.cancel();
        this.mLastDevice = device;
        this.mLastType = BT;

        // Start the thread to connectBt with the given device
        mConnectThread = new ConnectThreadBt(device, secure);
        mConnectThread.start();
        // Update UI title
        updateUserInterfaceTitle();
    }

    public synchronized void connectTcp(String ip, Integer port) {
        AutopilotService.this.cancel();
        this.mLastIp = ip;
        this.mLastPort = port;
        this.mLastType = TCP;

        // Start the thread to connectBt with the given device
        mConnectThread = new ConnectThreadTcp(ip, port);
        mConnectThread.start();
        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        mLastType = STATE_NONE;
        this.cancel();
        mLastType = STATE_NONE;

        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Write to the ConnectedThreadBt in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThreadBt#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        WritableThread r;
        // Synchronize a copy of the ConnectedThreadBt
        synchronized (this) {
            if (mState == STATE_CONNECTED_BT) {
                r = mConnectedThreadBt;
            } else if (mState == STATE_CONNECTED_TCP) {
                r = mConnectThread;
            } else {
                return;
            }
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    private void broadcast_sent_msg(byte[] buffer) {
        Intent in = new Intent(AutopilotService.AUTOPILOT_INTENT);
        in.putExtra("intentType", Constants.MESSAGE_WRITE);
        in.putExtra(Integer.toString(Constants.MESSAGE_WRITE), buffer);
        // LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(in);
    }

    private void broadcast_msg(String tmp) {
        if (tmp != null) {
            Intent in = new Intent(AutopilotService.AUTOPILOT_INTENT);
            in.putExtra("intentType", Constants.MESSAGE_READ);
            in.putExtra(Integer.toString(Constants.MESSAGE_READ), tmp);

            String[] s = mBuf.addGetAll(tmp);
            in.putExtra("History", s);
            LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(in);

            if(mLogFileStream != null) {
                try {
                    mLogFileStream.write(tmp.getBytes());
                    mLogFileStream.write('\n');
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    protected class ConnectThreadBt extends Thread implements WritableThread {
        protected final BluetoothSocket mmSocket;
        protected final BluetoothDevice mmDevice;
        protected String mSocketType;

        public ConnectThreadBt(BluetoothDevice device, boolean secure) {

            mmDevice = device;
            mDeviceName = mmDevice.getName();
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
            mState = STATE_CONNECTING_BT;
            mDeviceName = mmDevice.getName();
            updateUserInterfaceTitle();
        }

        public void run() {

            setName("de.kast.autopilot.ConnectThreadBt" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapterBt.cancelDiscovery();

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
                mState = STATE_NONE;
                updateUserInterfaceTitle();
                return;
            }

            // Reset the ConnectThreadBt because we're done
            synchronized (AutopilotService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            AutopilotService.this.cancel();
            mState = STATE_CONNECTING_BT;
            updateUserInterfaceTitle();

            // Start the thread to manage the connection and perform transmissions
            mConnectedThreadBt = new ConnectedThreadBt(mmSocket);
            mConnectedThreadBt.start();

            mDeviceName = mmDevice.getName();


            // Update UI title
            updateUserInterfaceTitle();

        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }

        @Override
        public void write(byte[] buffer) {

        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    protected class ConnectedThreadBt extends Thread implements WritableThread {
        protected final BluetoothSocket mmSocket;
        protected final InputStream mmInStream;
        protected final OutputStream mmOutStream;

        public ConnectedThreadBt(BluetoothSocket socket) {
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
            mState = STATE_CONNECTED_BT;
            updateUserInterfaceTitle();
        }

        public void run() {
            setName("de.kast.autopilot.ConnectThreadBT");
            byte[] buffer = new byte[1024];
            int bytes;
            String readMessage = "";
            int index = -1;
            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED_BT) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI Activity
                    readMessage = readMessage.concat(new String(buffer, 0, bytes));
                    index = readMessage.indexOf("\r");
                    String tmp = null;
                    if (index != -1) {
                        tmp = readMessage.substring(0, index);
                        int index2 = tmp.indexOf("\n");
                        if (index2 != -1) {
                            tmp = tmp.substring(index2 + 1, tmp.length());
                        }
                    } else {
                        index = readMessage.indexOf('\n');
                        if (index > 0) {
                            tmp = readMessage.substring(0, index);
                        }
                    }
                    if (index + 1 < readMessage.length())
                        readMessage = readMessage.substring(index + 1, readMessage.length());
                    else
                        readMessage = "";
                    broadcast_msg(tmp);
                } catch (IOException e) {
                    mState = STATE_NONE;
                    updateUserInterfaceTitle();
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

                broadcast_sent_msg(buffer);
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

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    protected class ConnectThreadTcp extends Thread implements WritableThread {
        protected final String mIp;
        protected final int mPort;
        Socket mmSocket;

        private OutputStream mmOutStream;
        private BufferedReader mmInStream;

        public ConnectThreadTcp(String ip, int port) {
            mIp = ip;
            mPort = port;
            mState = STATE_CONNECTING_TCP;
            mDeviceName = mIp + ":" + mPort;

            updateUserInterfaceTitle();
        }

        public void run() {
            setName("de.kast.autopilot.ConnectThreadTcp" + mIp + ":" + mPort);

            while (mState == STATE_CONNECTING_TCP) {
                try {
                    mmSocket = new Socket(mIp, mPort);
                } catch (IOException e) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e3) {
                        return;
                    }
                    continue;
                }
                mState = STATE_CONNECTED_TCP;

                try {
                    mmOutStream = mmSocket.getOutputStream();
                    mmInStream = new BufferedReader(new InputStreamReader(mmSocket.getInputStream()));
                } catch (IOException e) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e2) {
                        return;
                    }
                    continue;
                }
                updateUserInterfaceTitle();

                while (mState == STATE_CONNECTED_TCP) {
                    try {
                        String msg = mmInStream.readLine();
                        broadcast_msg(msg);
                    } catch (IOException e) {
                        if (mState == STATE_CONNECTED_TCP) {
                            mState = STATE_CONNECTING_TCP;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e2) {
                            return;
                        }
                        updateUserInterfaceTitle();
                    }
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(final byte[] buffer) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    setName("de.kast.autopilot.tcp_writer");
                    try {
                        mmOutStream.write(buffer);
                        mmOutStream.flush();
                    } catch (IOException e) {
                        mState = STATE_CONNECTING_TCP;
                        updateUserInterfaceTitle();
                    }
                }
            }).start();
            broadcast_sent_msg(buffer);
        }

        public void cancel() {
            try {
                if (mmOutStream != null) {
                    mmOutStream.flush();
                    mmOutStream.close();
                }
                if (mmInStream != null) {
                    mmInStream.close();
                }
                if (mmSocket != null) {
                    mmSocket.close();
                }
            } catch (IOException e) {
                mState = STATE_NONE;
                updateUserInterfaceTitle();
            }
        }
    }
}