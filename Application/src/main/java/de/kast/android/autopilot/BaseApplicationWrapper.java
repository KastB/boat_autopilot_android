package de.kast.android.autopilot;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;

/**
 * Created by bernd on 01.07.17.
 */

public class BaseApplicationWrapper extends Application {
    //Hoping to hold connection more stable
    public BluetoothAdapter mBluetoothAdapter;

    @Override
    public void onCreate() {
        super.onCreate();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }
}
