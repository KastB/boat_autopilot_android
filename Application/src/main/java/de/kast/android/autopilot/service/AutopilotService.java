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

package de.kast.android.autopilot.service;


import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import de.kast.android.autopilot.Constants;


public class AutopilotService extends Service {

    static String mHeader = "Millis,m_currentPosition,CurrentPosition,LastSpeed,TargetPosition,startButton,stopButton,parkingButton,DiagA,DiagB,m_P,m_I,m_D,m_goalType,m_goal,m_lastError,m_errorSum,m_lastFilteredYaw,UI,yaw,roll,pitch,freq,magMin[0],magMin[1],magMin[2],magMax[0],magMax[1],magMax[2],m_speed,m_speed.tripMileage,m_speed.totalMileage,m_speed.waterTemp,m_lampIntensity,m_wind.apparentAngle,m_wind.apparentSpeed,m_wind.displayInKnots,m_wind.displayInMpS,m_depth.anchorAlarm,m_depth.deepAlarm,m_depth.defective,m_depth.depthBelowTransductor,m_depth.metricUnits,m_depth.shallowAlarm,m_depth.unknown,GPS,m_voltage,m_current,m_power,twd,tws,gps_vel,vmg";
    String[] mParts;

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
    protected static final UUID UUID_SECURE =
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
    private int mLastType = STATE_NONE;
    private File mLogFile;
    private FileOutputStream mLogFileStream;
    private HashMap<String, Boolean> mNormalizeMap;

    /**
     * Constructor. Prepares a new sessions.
     */
    public AutopilotService() {
        mParts = mHeader.split(",");
        mAdapterBt = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mOldState = mState;
        System.out.println("AutopilotService Constructor");
        mBuf = new MyBuffer(mRingBufferSize);
        mDeviceName = "";
        mLogFile = null;
        mLogFileStream = null;
        mNormalizeMap = new HashMap<>();
        mNormalizeMap.put("m_goal", true);
        mNormalizeMap.put("m_lastError", true);
        mNormalizeMap.put("m_lastFilteredYaw", true);
        mNormalizeMap.put("yaw", true);
        mNormalizeMap.put("m_wind.apparentAngle", true);
        mNormalizeMap.put("twd", true);
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
        if (mOldState != mState) {
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
        }
        mOldState = mState;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        File dir = new File (Environment.getExternalStorageDirectory().getAbsolutePath() + "/Autopilot");
        if (dir.isDirectory() || dir.mkdirs()) {
            mLogFile = new File(dir, ("autopilot_" + DateFormat.getDateTimeInstance().format(new Date()) + ".log").replace(":", "_").replace(" ", "__"));
            try {
                mLogFileStream = new FileOutputStream(mLogFile, true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                mLogFileStream = null;
            }
        }
        else {
            System.out.println("Probably insufficient permissions to write file");
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

    public void cancel() {
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
     * Stop all threads
     */
    public synchronized void stop() {
        mLastType = STATE_NONE;
        this.cancel();
        mLastType = STATE_NONE;
    }

    /**
     * Start the ConnectThreadBt to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connectBt
     */
    public synchronized void connectBt(BluetoothDevice device) {
        this.cancel();
        this.mLastType = BT;

        // Start the thread to connectBt with the given device
        mConnectThread = new ConnectThreadBt(this, device);
        mConnectThread.start();
        // Update UI title
        updateUserInterfaceTitle();
    }

    public synchronized void connectTcp(String ip, Integer port, boolean complient) {
        if (    complient &&
                ip.equals(this.mLastIp) &&
                this.mLastPort == port &&
                this.mLastType == TCP) {
            return;
        }

        AutopilotService.this.cancel();
        this.mLastIp = ip;
        this.mLastPort = port;
        this.mLastType = TCP;

        // Start the thread to connectBt with the given device
        mConnectThread = new ConnectThreadTcp(this, ip, port);
        mConnectThread.start();
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

    public void broadcast_sent_msg(byte[] buffer) {
        Intent in = new Intent(AutopilotService.AUTOPILOT_INTENT);
        in.putExtra("intentType", Constants.MESSAGE_WRITE);
        in.putExtra(Integer.toString(Constants.MESSAGE_WRITE), buffer);
        // LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(in);
    }

    public void broadcast_msg(String rawMessage) {
        if (rawMessage != null) {
            writeToLog(rawMessage);

            HashMap<String, Double> processedMsg;
            try {
                processedMsg = decodeRawData(rawMessage);
            }
            catch (IndexOutOfBoundsException e) {
                return;
            }
            ArrayList<HashMap<String, Double>> history = mBuf.addGetAll(processedMsg);

            //corrupt order => clear history
            if (history.size() > 1) {
                boolean clear = false;
                try {
                    if (history.get(0).get("Millis") < history.get(1).get("Millis")) {
                        clear = true;
                    }
                }
                catch (NullPointerException ignored) {
                    clear = true;
                }
                if (clear) {
                    mBuf.clear();
                    history = mBuf.addGetAll(processedMsg);
                }
            }
            Intent in = new Intent(AutopilotService.AUTOPILOT_INTENT);
            in.putExtra("intentType", Constants.MESSAGE_READ_RAW);
            in.putExtra(Integer.toString(Constants.MESSAGE_READ_RAW),       rawMessage);
            in.putExtra(Integer.toString(Constants.MESSAGE_READ_PROCESSED), processedMsg);
            in.putExtra(Integer.toString(Constants.MESSAGE_READ_HISTORY),   history);

            LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(in);
        }
    }

    public HashMap<String, Double> decodeRawData(String line) throws IndexOutOfBoundsException {
        String[] parts = line.split(",");
        HashMap<String, Double> result;
        double value;

        result = new HashMap<>();
        if (parts.length < mParts.length) {
            throw new IndexOutOfBoundsException();
        }
        for(int i = 0; i < mParts.length; i++) {
            try {
                value = Double.parseDouble(parts[i]);
                if (mNormalizeMap.containsKey(mParts[i])) {
                    value = normalize(value);
                }
            }
            catch (java.lang.NumberFormatException e){
                value = 0.0;
            }
            result.put(mParts[i], value);
        }
        return result;
    }

    private void writeToLog(String tmp) {
        if(mLogFileStream != null) {
            try {
                mLogFileStream.write(tmp.getBytes());
                mLogFileStream.write('\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (AutopilotService.getInstance() == null)
            return;
        if (AutopilotService.getInstance().getState() != AutopilotService.STATE_CONNECTED_BT &&
                AutopilotService.getInstance().getState() != AutopilotService.STATE_CONNECTED_TCP) {
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the AutopilotService to write
            byte[] send = message.getBytes();
            AutopilotService.getInstance().write(send);
        }
    }

    private double normalize(double in) {
        while (in > 180.0) {
            in -= 360.0;
        }
        while (in < -180.0) {
            in += 180.0;
        }
        return in;
    }

    public static AutopilotService getInstance() {
        return sInstance;
    }
}