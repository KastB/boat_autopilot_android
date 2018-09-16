package de.kast.android.autopilot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by bernd on 11.07.17.
 */

public class DataUpdateReceiverFragment extends BroadcastReceiver {
    private MyFragment mFragment;

    public DataUpdateReceiverFragment(MyFragment fragment) {
        mFragment = fragment;
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
                    case Constants.MESSAGE_READ_RAW:
                    String rawMessage                               =                                       intent.getStringExtra(Integer.toString(Constants.MESSAGE_READ_RAW));
                        HashMap<String, Double> msg                 = (HashMap<String, Double>)             intent.getSerializableExtra(Integer.toString(Constants.MESSAGE_READ_PROCESSED));
                        ArrayList<HashMap<String, Double>> history  = (ArrayList<HashMap<String, Double>>)  intent.getSerializableExtra(Integer.toString(Constants.MESSAGE_READ_HISTORY));
                        mFragment.setData(rawMessage, msg, history);
                        break;
                    case Constants.MESSAGE_DEVICE_NAME:
                        // save the connected device's name
                        String deviceName = intent.getStringExtra(Integer.toString(Constants.MESSAGE_DEVICE_NAME));
                        if (!deviceName.equals(mFragment.mConnectedDeviceName)) {
                            if (null != activity) {
                                Toast.makeText(activity, "Connected to "
                                        + deviceName, Toast.LENGTH_SHORT).show();
                            }
                        }
                        mFragment.mConnectedDeviceName = deviceName;
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
}