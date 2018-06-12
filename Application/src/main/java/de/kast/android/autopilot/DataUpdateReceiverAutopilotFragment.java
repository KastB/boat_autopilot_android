package de.kast.android.autopilot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

/**
 * Created by bernd on 11.07.17.
 */

public class DataUpdateReceiverAutopilotFragment extends BroadcastReceiver {
    static AutopilotFragment mAf;

    public DataUpdateReceiverAutopilotFragment() {

    }

    public DataUpdateReceiverAutopilotFragment(AutopilotFragment af) {
        mAf = af;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mAf == null)
            return;
        try {
            if (intent.getAction().equals(AutopilotService.AUTOPILOT_INTENT)) {
                FragmentActivity activity = mAf.getActivity();
                switch (intent.getIntExtra("intentType", -1)) {
                    case Constants.MESSAGE_STATE_CHANGE:
                        switch (intent.getIntExtra(Integer.toString(Constants.MESSAGE_STATE_CHANGE), -1)) {
                            case AutopilotService.STATE_CONNECTED_BT:
                                mAf.setStatus(mAf.getString(R.string.title_connected_to, mAf.mConnectedDeviceName));
                                break;
                            case AutopilotService.STATE_CONNECTING_BT:
                                mAf.setStatus(R.string.title_connecting_bt);
                                break;
                            case AutopilotService.STATE_LISTEN_BT:
                            case AutopilotService.STATE_CONNECTED_TCP:
                                mAf.setStatus(mAf.getString(R.string.title_connected_to, mAf.mConnectedDeviceName));
                                break;
                            case AutopilotService.STATE_CONNECTING_TCP:
                                mAf.setStatus(R.string.title_connecting_tcp);
                                break;
                            case AutopilotService.STATE_NONE:
                                mAf.setStatus(R.string.title_not_connected);
                                break;
                        }
                        break;
                    case Constants.MESSAGE_WRITE:
                        break;
                    case Constants.MESSAGE_READ:
                        String readMessage = intent.getStringExtra(Integer.toString(Constants.MESSAGE_READ));
                        String[] parts = readMessage.split("\t");
                        if (parts.length >= 45) {

                            mAf.mGoalView.setText(mAf.reducePrecision(parts[15], 0));
                            mAf.mErrorView.setText(mAf.reducePrecision(parts[16], 0));
                            mAf.mWindSpeedView.setText(mAf.reducePrecision(parts[36], 1));          //TODO: calculate/check true wind speed
                            mAf.mWindDirectionView.setText(mAf.reducePrecision(parts[35], 0));
                            mAf.mSpeedView.setText(mAf.reducePrecision(parts[30], 1));
                            mAf.mHeadingView.setText(mAf.reducePrecision(parts[20], 0));
                            mAf.mDepthView.setText(mAf.reducePrecision(parts[42], 1));
                            mAf.mTempView.setText(mAf.reducePrecision(parts[33], 0));
                            mAf.mRudderView.setText(mAf.reducePrecision(parts[1], 0));
                            mAf.mIntegralView.setText(mAf.reducePrecision(parts[17], 0));
                            mAf.mHeelView.setText(mAf.reducePrecision(parts[21],1));
                            mAf.mPitchView.setText(mAf.reducePrecision(parts[22], 1));

                            if (parts[14].equals("0")) {
                                mAf.mPositionModeButton.setEnabled(false);
                                mAf.mCompassModeButton.setEnabled(true);
                                mAf.mWindModeButton.setEnabled(true);
                            } else if (parts[14].equals("1")) {
                                mAf.mPositionModeButton.setEnabled(true);
                                mAf.mCompassModeButton.setEnabled(true);
                                mAf.mWindModeButton.setEnabled(false);
                            } else if (parts[14].equals("2")) {
                                mAf.mPositionModeButton.setEnabled(true);
                                mAf.mCompassModeButton.setEnabled(false);
                                mAf.mWindModeButton.setEnabled(true);
                            }

                        }

                        break;
                    case Constants.MESSAGE_DEVICE_NAME:
                        // save the connected device's name
                        mAf.mConnectedDeviceName = intent.getStringExtra(Integer.toString(Constants.MESSAGE_DEVICE_NAME));
                        if (null != activity) {
                            Toast.makeText(activity, "Connected to "
                                    + mAf.mConnectedDeviceName, Toast.LENGTH_SHORT).show();
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