package de.kast.android.autopilot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by bernd on 11.07.17.
 */

public class DataUpdateReceiverFragment extends BroadcastReceiver {
    private MyFragment mFragment;
    static String mHeader = "Millis,m_currentPosition,m_pressedButtonDebug,m_bytesToSent,CurrentPosition,CurrentDirection,TargetPosition,MSStopped,startButton,stopButton,parkingButton,diagA,diagB,m_P,m_I,m_D,m_goalType,m_goal,m_lastError,m_errorSum,m_lastFilteredYaw,UI,yaw,roll,pitch,freq,magMin[0],magMin[1],magMin[2],magMax[0],magMax[1],magMax[2],m_speed,m_speed.tripMileage,m_speed.totalMileage,m_speed.waterTemp,m_lampIntensity,m_wind.apparentAngle,m_wind.apparentSpeed,m_wind.displayInKnots,m_wind.displayInMpS,m_depth.anchorAlarm,m_depth.deepAlarm,m_depth.defective,m_depth.depthBelowTransductor,m_depth.metricUnits,m_depth.shallowAlarm,m_depth.unknown,Position";
    String mParts[];


    public DataUpdateReceiverFragment(MyFragment fragment) {
        mFragment = fragment;
        mParts = mHeader.split(",");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mFragment == null)
            return;
        try {
            if (intent.getAction().equals(AutopilotService.AUTOPILOT_INTENT)) {
                FragmentActivity activity = mFragment.getActivity();
                switch (intent.getIntExtra("intentType", -1)) {
                    case Constants.MESSAGE_STATE_CHANGE:
                        switch (intent.getIntExtra(Integer.toString(Constants.MESSAGE_STATE_CHANGE), -1)) {
                            case AutopilotService.STATE_CONNECTED_BT:
                                break;
                            case AutopilotService.STATE_CONNECTING_BT:
                                mFragment.setStatus(R.string.title_connecting_bt);
                                break;
                            case AutopilotService.STATE_LISTEN_BT:
                            case AutopilotService.STATE_CONNECTED_TCP:
                                mFragment.setStatus(mFragment.getString(R.string.title_connected_to, mFragment.mConnectedDeviceName));
                                break;
                            case AutopilotService.STATE_CONNECTING_TCP:
                                mFragment.setStatus(R.string.title_connecting_tcp);
                                break;
                            case AutopilotService.STATE_NONE:
                                mFragment.setStatus(R.string.title_not_connected);
                                break;
                        }
                        break;
                    case Constants.MESSAGE_WRITE:
                        break;
                    case Constants.MESSAGE_READ:
                        String readMessage = intent.getStringExtra(Integer.toString(Constants.MESSAGE_READ));
                        try {
                            HashMap<String, Double> msg = decodeRawData(readMessage);
                            String[] h = intent.getStringArrayExtra("History");
                            ArrayList<HashMap<String, Double>> history = new ArrayList<>();
                            if (h != null) {
                                for (String dat : h) {
                                    try {
                                        history.add(decodeRawData(dat));
                                    } catch (IndexOutOfBoundsException ignored) {
                                    }
                                }
                            }
                            mFragment.setData(readMessage, msg, history);
                        }
                        catch (IndexOutOfBoundsException ignored) {}

                        break;
                    case Constants.MESSAGE_DEVICE_NAME:
                        // save the connected device's name
                        String deviceName = intent.getStringExtra(Integer.toString(Constants.MESSAGE_DEVICE_NAME));
                        if (null != activity) {
                            Toast.makeText(activity, "Connected to "
                                    + deviceName, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case Constants.MESSAGE_TOAST:
                        if (null != activity) {
                            Toast.makeText(activity, intent.getStringExtra(Integer.toString(Constants.MESSAGE_TOAST)),
                                    Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            }
        } catch (java.lang.IllegalStateException e) {
            System.out.println(e.toString());
        }
    }

    public HashMap<String, Double> decodeRawData(String line) {
        String parts[] = line.split("\t");
        HashMap<String, Double> result = new HashMap<>();
        Double value;
        if (parts.length < mParts.length) {
            throw new IndexOutOfBoundsException();
        }
        for(int i = 0; i < mParts.length; i++) {
            try {
                value = Double.parseDouble(parts[i]);
            }
            catch (java.lang.NumberFormatException e){
                value = 0.0;
            }
            result.put(mParts[i], value);
        }
        return result;
    }
}