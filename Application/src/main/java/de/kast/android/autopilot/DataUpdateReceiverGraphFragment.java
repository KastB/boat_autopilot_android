package de.kast.android.autopilot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.jjoe64.graphview.series.DataPoint;

/**
 * Created by bernd on 11.07.17.
 */

public class DataUpdateReceiverGraphFragment extends BroadcastReceiver {
    static private final int mTimeHorizon = 120;
    static private final int mMaxDataPoints = 1024;
    static GraphFragment mGf;

    public DataUpdateReceiverGraphFragment() {

    }

    public DataUpdateReceiverGraphFragment(GraphFragment gf) {
        mGf = gf;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mGf == null)
            return;
        try {
            if (intent.getAction().equals(AutopilotService.AUTOPILOT_INTENT)) {
                FragmentActivity activity = mGf.getActivity();
                switch (intent.getIntExtra("intentType", -1)) {
                    case Constants.MESSAGE_STATE_CHANGE:
                        switch (intent.getIntExtra(Integer.toString(Constants.MESSAGE_STATE_CHANGE), -1)) {
                            case AutopilotService.STATE_CONNECTED_BT:
                                mGf.setStatus(mGf.getString(R.string.title_connected_to, mGf.mConnectedDeviceName));
                                break;
                            case AutopilotService.STATE_CONNECTING_BT:
                                mGf.setStatus(R.string.title_connecting_bt);
                            case AutopilotService.STATE_CONNECTING_TCP:
                                mGf.setStatus(R.string.title_connecting_tcp);
                                break;
                            case AutopilotService.STATE_LISTEN_BT:
                            case AutopilotService.STATE_NONE:
                                mGf.setStatus(R.string.title_not_connected);
                                break;
                        }
                        break;
                    case Constants.MESSAGE_WRITE:
                        String writeMessage = intent.getStringExtra(Integer.toString(Constants.MESSAGE_WRITE));
                        break;
                    case Constants.MESSAGE_READ:
                        if (mGf.mSeriesValid) {
                            String readMessage = intent.getStringExtra(Integer.toString(Constants.MESSAGE_READ));
                            String[] parts = readMessage.split("\t");
                            if (parts.length >= 45) {
                                double x = -1.0;
                                for (int i = 0; i < mGf.mSeries.length; i++) {
                                    DataPoint p = getDataPoint(parts, mGf.mDataSets[i].mIndex);
                                    x = p.getX();
                                    try {
                                        mGf.mSeries[i].appendData(p, true, mMaxDataPoints);
                                    } catch (java.lang.IllegalArgumentException e) {
                                        mGf.mSeriesValid = false;
                                    }
                                }
                                mGf.mGraph.getViewport().setXAxisBoundsManual(true);
                                mGf.mGraph.getViewport().setMinX(x - mTimeHorizon);
                                mGf.mGraph.getViewport().setMaxX(x + 2.0);
                            }
                        } else {
                            String[] s;
                            s = intent.getStringArrayExtra("History");
                            if (s != null) {
                                DataPoint[][] data = new DataPoint[s.length][mGf.mDataSets.length];
                                int counter = 0;
                                for (int i = 0; i < s.length; i++) {
                                    String[] parts = s[i].split("\t");
                                    if (parts.length >= 45) {
                                        try {
                                            for (int z = 0; z < mGf.mDataSets.length; z++) {
                                                data[counter][z] = getDataPoint(parts, mGf.mDataSets[z].mIndex);
                                            }
                                            counter++;
                                        } catch (java.lang.NumberFormatException e) {
                                            System.out.println("Warning: invalid data received#2");
                                        }
                                    } else {
                                        System.out.println("Warning: invalid data received");
                                    }
                                }
                                DataPoint[][] cleaned_data = new DataPoint[mGf.mDataSets.length][counter];
                                for (int i = 0; i < counter; i++) {
                                    for (int z = 0; z < mGf.mDataSets.length; z++) {
                                        cleaned_data[z][counter - i - 1] = data[i][z];
                                    }
                                }
                                mGf.mSeriesValid = true;
                                for (int z = 0; z < mGf.mDataSets.length; z++) {
                                    try {
                                        mGf.mSeries[z].resetData(cleaned_data[z]);
                                    } catch (java.lang.IllegalArgumentException e) {
                                        mGf.mSeriesValid = false;
                                    }
                                }
                            }
                        }

                        break;
                    case Constants.MESSAGE_DEVICE_NAME:
                        // save the connected device's name
                        mGf.mConnectedDeviceName = intent.getStringExtra(Integer.toString(Constants.MESSAGE_DEVICE_NAME));
                        if (null != activity) {
                            Toast.makeText(activity, "Connected to "
                                    + mGf.mConnectedDeviceName, Toast.LENGTH_SHORT).show();
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

    public DataPoint getDataPoint(String[] parts, int index) {
        Double time;
        Double value;
        try {
            time = Double.parseDouble(parts[0]) / 1000.0;
        }
        catch (java.lang.NumberFormatException e){
            time = 0.0;
        }
        try {
            value = Double.parseDouble(parts[index]);
        }
        catch (java.lang.NumberFormatException e){
            value = 0.0;
        }
        return new DataPoint(time, value);
    }
}