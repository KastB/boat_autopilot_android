package de.kast.android.autopilot.fragments;

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
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.text.InputType;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import de.kast.android.autopilot.MainActivity;
import de.kast.android.autopilot.service.AutopilotService;
import de.kast.android.autopilot.DeviceListActivity;
import de.kast.android.autopilot.R;

import static java.lang.Math.max;

/**
 * Created by bernd on 30.06.17.
 */

abstract class MyFragment extends Fragment {
    // Intent request codes
    protected static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
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

    abstract void setup();
    abstract public View createView(LayoutInflater inflater, @Nullable ViewGroup container,
                                    @Nullable Bundle savedInstanceState);

    abstract public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState);
    abstract public void setData(String rawMessage, HashMap<String, Double> data, ArrayList<HashMap<String, Double>> history);
    abstract public String getFragmentName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupConnections();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = createView(inflater, container, savedInstanceState);
        view.setOnTouchListener(new TouchListener(this));
        updateLayout(view);
        return view;
    }

    private void updateLayout(View view) {
        setHasOptionsMenu(true);

        int fullscreen_mode = getPreference("fullscreen_mode", 0);
        int mode = getPreference("screen_orientation", ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        try {
            MainActivity activity = (MainActivity)getActivity();
            ActionBar ab = activity.getSupportActionBar();
            ab.setDisplayShowTitleEnabled(false);
            activity.setRequestedOrientation(mode);
            if (fullscreen_mode == 1)
                ab.hide();
            else
                ab.show();
        } catch (NullPointerException ignored){
        }

        updateFontSizes(view);
    }

    private void setupConnections() {
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }
        String tcpServer = "";
        tcpServer = getPreference("last_tcp_server", tcpServer);
        connectToTCPServer(tcpServer, true);
    }


    @Override
    public void onStart() {
        super.onStart();
        setup();
        if (mDataUpdateReceiver == null)
            mDataUpdateReceiver = new DataUpdateReceiverFragment(this);
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
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mDataUpdateReceiver == null)
            mDataUpdateReceiver = new DataUpdateReceiverFragment(this);
        IntentFilter intentFilter = new IntentFilter(AutopilotService.AUTOPILOT_INTENT);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mDataUpdateReceiver, intentFilter);
    }

    public void doubleClicked() {
        int fullscreen_mode = getPreference("fullscreen_mode", 0);
        if (fullscreen_mode == 0) {
            fullscreen_mode = 1;
        }
        else {
            fullscreen_mode = 0;
        }
        setPreference("fullscreen_mode", fullscreen_mode);
        updateLayout(getView());
    }

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    protected void setStatus(int resId) {
        MainActivity activity = (MainActivity) getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getSupportActionBar();
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
        MainActivity activity = (MainActivity)getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getSupportActionBar();
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
                    connectDeviceBtPopup(data);
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

    protected void connectDeviceTcpPopup() {
        if (AutopilotService.getInstance() == null)
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setTitle("Please enter ip:port");
        final EditText input = new EditText(this.getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        String serverAddress = "10.0.0.1:2948";
        getPreference("last_tcp_server", serverAddress);
        input.setText(serverAddress);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String serverAddress;
                serverAddress = input.getText().toString();

                //save text in preferences
                setPreference("last_tcp_server", serverAddress);
                System.out.println(serverAddress);
                connectToTCPServer(serverAddress);
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
    private void connectToTCPServer(String serverAddress) {
        connectToTCPServer(serverAddress, false);
    }

    private void connectToTCPServer(String serverAddress, boolean compliant) {
        try {
            String ip = serverAddress.substring(0, serverAddress.indexOf(":"));
            int port = Integer.parseInt(serverAddress.substring(ip.length() + 1, serverAddress.length()));
            AutopilotService.getInstance().connectTcp(ip, port, compliant);
        }
        catch (java.lang.StringIndexOutOfBoundsException ignored) {
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     */
    protected void connectDeviceBtPopup(Intent data) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connectBt to the device
        if (AutopilotService.getInstance() == null)
            return;
        AutopilotService.getInstance().connectBt(device);
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
                connectDeviceTcpPopup();
                return true;
            }
            case R.id.rotate: {
                String preference = "screen_orientation";
                int mode = getPreference(preference, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

                int new_mode = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                if (mode == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    new_mode = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                }
                setPreference(preference, new_mode);
                updateLayout(getView());
                return true;
            }
            case R.id.increase_font_size: {
                String preference = "font_size_text" + getFragmentName() + getPreference("screen_orientation", ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                int font_size = getPreference(preference, 22);
                font_size += 1;
                setPreference(preference, font_size);
                updateLayout(getView());
                return true;
            }
            case R.id.decrease_font_size: {
                String preference = "font_size_text" + getFragmentName() + getPreference("screen_orientation", ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                int font_size = getPreference(preference, 22);
                font_size -= 1;
                font_size = max(0, font_size);
                setPreference(preference, font_size);
                updateLayout(getView());
                return true;
            }
            case R.id.increase_label_size: {
                String preference = "font_size_label" + getFragmentName() + getPreference("screen_orientation", ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                int font_size = getPreference(preference, 11);
                font_size += 1;
                setPreference(preference, font_size);
                updateLayout(getView());
                return true;
            }
            case R.id.decrease_label_size: {
                String preference = "font_size_label" + getFragmentName() + getPreference("screen_orientation", ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                int font_size = getPreference(preference, 11);
                font_size -= 1;
                font_size = max(0, font_size);
                setPreference(preference, font_size);
                updateLayout(getView());
                return true;
            }
            case R.id.shutdown_server: {
                AutopilotService.sendMessage("#SHUTDOWN");
                return true;
            }
            case R.id.reboot_server: {
                AutopilotService.sendMessage("#REBOOT");
                return true;
            }
        }
        return false;
    }

    private void updateFontSizes(View view) {
        int font_size_text = getPreference("font_size_text" + getFragmentName() + getPreference("screen_orientation", ActivityInfo.SCREEN_ORIENTATION_PORTRAIT), 22);
        int font_size_label = getPreference("font_size_label" + getFragmentName() + getPreference("screen_orientation", ActivityInfo.SCREEN_ORIENTATION_PORTRAIT), 11);
        updateFontSize(view, font_size_text, font_size_label);
    }

    public static void updateFontSize(View v, int font_size_text, int font_size_label){
        try {
            if (v instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) v;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    View child = vg.getChildAt(i);
                    updateFontSize(child, font_size_text, font_size_label);
                }
            } else if (v instanceof TextView) {
                if (v.getLabelFor() == -1) {
                    ((TextView) v).setTextSize(TypedValue.COMPLEX_UNIT_DIP, font_size_text);
                }
                else {
                    ((TextView) v).setTextSize(TypedValue.COMPLEX_UNIT_DIP, font_size_label);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setPreference(String preference, int new_mode) {
        try {
            SharedPreferences sharedPref = getContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            sharedPref.edit().putInt(preference, new_mode).apply();
        } catch (NullPointerException ignore) {
        }
    }

    private int getPreference(String preference, int default_value) {
        int mode = default_value;
        try {
            SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            mode = sharedPref.getInt(preference, mode);
        } catch (NullPointerException ignore) {
        }
        return mode;
    }

    private void setPreference(String preference, String new_mode) {
        try {
            SharedPreferences sharedPref = getContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            sharedPref.edit().putString(preference, new_mode).apply();
        } catch (NullPointerException ignore) {
        }
    }

    private String getPreference(String preference, String default_value) {
        String mode = default_value;
        try {
            SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            mode = sharedPref.getString(preference, mode);
        } catch (NullPointerException ignore) {
        }
        return mode;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            try {
                updateLayout(getView());
            }
            catch(IllegalStateException ignore) {}
        }
    }
}
