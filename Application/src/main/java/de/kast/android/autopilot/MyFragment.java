package de.kast.android.autopilot;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by bernd on 30.06.17.
 */

abstract class MyFragment extends Fragment {
    // Intent request codes
    protected static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    protected static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    protected static final int REQUEST_ENABLE_BT = 3;
    /**
     * Name of the connected device
     */
    protected String mConnectedDeviceName = null;
    /**
     * Local Bluetooth adapter
     */
    protected BluetoothAdapter mBluetoothAdapter = null;
    BroadcastReceiver mDataUpdateReceiver;

    abstract BroadcastReceiver getNewDataUpdateReceiver();

    abstract void setup();

    abstract public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                      @Nullable Bundle savedInstanceState);

    abstract public void onViewCreated(View view, @Nullable Bundle savedInstanceState);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        setup();
        if (mDataUpdateReceiver == null)
            mDataUpdateReceiver = getNewDataUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter(AutopilotService.AUTOPILOT_INTENT);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mDataUpdateReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            if (mDataUpdateReceiver != null) {
                LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mDataUpdateReceiver);
            }
        } catch (IllegalArgumentException e) {

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mDataUpdateReceiver == null)
            mDataUpdateReceiver = getNewDataUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter(AutopilotService.AUTOPILOT_INTENT);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mDataUpdateReceiver, intentFilter);
    }


    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    protected void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (AutopilotService.getInstance() == null)
            return;
        if (AutopilotService.getInstance().getState() != AutopilotService.STATE_CONNECTED_BT &&
                AutopilotService.getInstance().getState() != AutopilotService.STATE_CONNECTED_TCP) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the AutopilotService to write
            byte[] send = message.getBytes();
            AutopilotService.getInstance().write(send);
        }
    }

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    protected void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    protected void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connectBt
                if (resultCode == Activity.RESULT_OK) {
                    connectDeviceBt(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connectBt
                if (resultCode == Activity.RESULT_OK) {
                    connectDeviceBt(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setup();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    protected void connectDeviceTcp() {
        if (AutopilotService.getInstance() == null)
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setTitle("Please enter ip:port");
        final EditText input = new EditText(this.getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        String text = "192.168.178.26:2948";
        try {
            SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            text = sharedPref.getString("last_tcp_server", text);
        } catch (NullPointerException e) {
        }
        input.setText(text);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text;
                text = input.getText().toString();

                //save text in preferences
                try {
                    SharedPreferences sharedPref = getContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                    sharedPref.edit().putString("last_tcp_server", text).apply();
                } catch (NullPointerException e) {
                }

                System.out.println(text);
                String ip = text.substring(0, text.indexOf(":"));
                int port = Integer.parseInt(text.substring(ip.length() + 1, text.length()));
                AutopilotService.getInstance().connectTcp(ip, port);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    protected void connectDeviceBt(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connectBt to the device
        if (AutopilotService.getInstance() == null)
            return;
        AutopilotService.getInstance().connectBt(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.buttons, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {

                AutopilotService.getInstance().stop();

                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.disconnect: {
                AutopilotService.getInstance().stop();
                return true;
            }
            case R.id.connect_tcp: {
                AutopilotService.getInstance().stop();
                connectDeviceTcp();
            }
        }
        return false;
    }


    public String reducePrecision(String str, int prec) {
        if (prec < 1) {
            if (str.contains("."))
                return str.substring(0, str.indexOf("."));
            else
                return str;
        }

        //untested
        if (!str.contains("."))
            str.concat(".");
        while (str.length() - str.indexOf(".") < prec + 1)
            str.concat("0");
        return str.substring(0, str.indexOf(".") + prec + 1);
    }
}
