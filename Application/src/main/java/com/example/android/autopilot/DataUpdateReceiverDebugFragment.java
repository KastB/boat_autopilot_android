package com.example.android.autopilot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

/**
 * Created by bernd on 11.07.17.
 */

public class DataUpdateReceiverDebugFragment extends BroadcastReceiver {
    static DebugFragment mDf;

    public DataUpdateReceiverDebugFragment() {

    }

    public DataUpdateReceiverDebugFragment(DebugFragment df) {
        mDf = df;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mDf == null)
            return;
        try {
            if (intent.getAction().equals(AutopilotService.AUTOPILOT_INTENT)) {
                FragmentActivity activity = mDf.getActivity();
                switch (intent.getIntExtra("intentType", -1)) {
                    case Constants.MESSAGE_STATE_CHANGE:
                        switch (intent.getIntExtra(Integer.toString(Constants.MESSAGE_STATE_CHANGE), -1)) {
                            case AutopilotService.STATE_CONNECTED_BT:
                                mDf.setStatus(mDf.getString(R.string.title_connected_to, mDf.mConnectedDeviceName));
                                break;
                            case AutopilotService.STATE_CONNECTING_BT:
                                mDf.setStatus(R.string.title_connecting_bt);
                                break;
                            case AutopilotService.STATE_LISTEN_BT:
                            case AutopilotService.STATE_CONNECTED_TCP:
                                mDf.setStatus(mDf.getString(R.string.title_connected_to, mDf.mConnectedDeviceName));
                                break;
                            case AutopilotService.STATE_CONNECTING_TCP:
                                mDf.setStatus(R.string.title_connecting_bt);
                                break;
                            case AutopilotService.STATE_NONE:
                                mDf.setStatus(R.string.title_not_connected);
                                break;
                        }
                        break;
                    case Constants.MESSAGE_WRITE:

                        String writeMessage = intent.getStringExtra(Integer.toString(Constants.MESSAGE_WRITE));

                        break;
                    case Constants.MESSAGE_READ:
                        String readMessage = intent.getStringExtra(Integer.toString(Constants.MESSAGE_READ));
                        String[] parts = readMessage.split("\t");
                        if (parts.length >= 45) {
                            mDf.mConversationArrayAdapter.add(readMessage);
                        }

                        break;
                    case Constants.MESSAGE_DEVICE_NAME:
                        // save the connected device's name
                        mDf.mConnectedDeviceName = intent.getStringExtra(Integer.toString(Constants.MESSAGE_DEVICE_NAME));
                        if (null != activity) {
                            Toast.makeText(activity, "Connected to "
                                    + mDf.mConnectedDeviceName, Toast.LENGTH_SHORT).show();
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
}