package com.example.android.autopilot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Set;

/**
 * Created by bernd on 11.07.17.
 */

public class DataUpdateReceiverGraphFragment extends BroadcastReceiver {
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
                            case AutopilotService.STATE_CONNECTED:
                                mGf.setStatus(mGf.getString(R.string.title_connected_to, mGf.mConnectedDeviceName));
                                break;
                            case AutopilotService.STATE_CONNECTING:
                                mGf.setStatus(R.string.title_connecting);
                                break;
                            case AutopilotService.STATE_LISTEN:
                            case AutopilotService.STATE_NONE:
                                mGf.setStatus(R.string.title_not_connected);
                                break;
                        }
                        break;
                    case Constants.MESSAGE_WRITE:

                        String writeMessage = intent.getStringExtra(Integer.toString(Constants.MESSAGE_WRITE));

                        break;
                    case Constants.MESSAGE_READ:

                        if(mGf.mSeriesValid)
                        {
                            String readMessage = intent.getStringExtra(Integer.toString(Constants.MESSAGE_READ));
                            String[] parts = readMessage.split("\t");
                            if (parts.length >= 45) {
                                DataPoint p = getDataPoint(parts);
                                mGf.mGoalSeries.appendData(p,true, 120);
                                mGf.mGraph.getViewport().setXAxisBoundsManual(true);
                                mGf.mGraph.getViewport().setMinX(p.getX()-120.0);
                                mGf.mGraph.getViewport().setMaxX(p.getX() + 2.0);
                            }
                        }
                        else {
                            String[] s;
                            s = intent.getStringArrayExtra("History");
                            if (s != null) {
                                DataPoint[] data = new DataPoint[s.length];
                                int counter = 0;

                                for (int i = 0; i < s.length; i++) {
                                    String[] parts = s[i].split("\t");
                                    if (parts.length >= 45) {
                                        data[counter] = getDataPoint(parts);
                                        counter++;
                                    } else {
                                        System.out.println("Warning: invalid data received");
                                    }
                                }
                                DataPoint[] cleaned_data = new DataPoint[counter];
                                for (int i = 0; i < counter; i++) {
                                    cleaned_data[counter - i-1] = data[i];
                                    System.out.println(data[i].getX());
                                }
                                mGf.mSeriesValid = true;
                                mGf.mGoalSeries.resetData(cleaned_data);
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

    public DataPoint getDataPoint(String[] parts)
    {
        DataPoint p = new DataPoint((double) Integer.parseInt(parts[0]) / 1000, Double.parseDouble(parts[15]));
        return p;
    }
}