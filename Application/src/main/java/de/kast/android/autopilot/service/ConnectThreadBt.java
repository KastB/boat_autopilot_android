package de.kast.android.autopilot.service;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;

/**
 * This thread runs while attempting to make an outgoing connection
 * with a device. It runs straight through; the connection either
 * succeeds or fails.
 */
class ConnectThreadBt extends Thread implements WritableThread {
    private final AutopilotService autopilotService;
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;

    ConnectThreadBt(AutopilotService autopilotService, BluetoothDevice device) {
        this.autopilotService = autopilotService;

        mmDevice = device;
        autopilotService.mDeviceName = mmDevice.getName();
        BluetoothSocket tmp = null;

        // Get a BluetoothSocket for a connection with the
        // given BluetoothDevice
        try {
            tmp = device.createRfcommSocketToServiceRecord(
                    AutopilotService.UUID_SECURE);
        } catch (IOException ignored) {
        }
        mmSocket = tmp;
        autopilotService.mState = AutopilotService.STATE_CONNECTING_BT;
        autopilotService.mDeviceName = mmDevice.getName();
        autopilotService.updateUserInterfaceTitle();
    }

    public void run() {

        setName("de.kast.autopilot.ConnectThreadBt");

        // Always cancel discovery because it will slow down a connection
        autopilotService.mAdapterBt.cancelDiscovery();

        // Make a connection to the BluetoothSocket
        try {
            // This is a blocking call and will only return on a
            // successful connection or an exception
            mmSocket.connect();
        } catch (IOException e) {
            // Close the socket
            try {
                mmSocket.close();
            } catch (IOException ignored) {
            }
            autopilotService.mState = AutopilotService.STATE_NONE;
            autopilotService.updateUserInterfaceTitle();
            return;
        }

        // Reset the ConnectThreadBt because we're done
        synchronized (autopilotService) {
            autopilotService.mConnectThread = null;
        }

        // Start the connected thread
        autopilotService.cancel();
        autopilotService.mState = AutopilotService.STATE_CONNECTING_BT;
        autopilotService.updateUserInterfaceTitle();

        // Start the thread to manage the connection and perform transmissions
        autopilotService.mConnectedThreadBt = new ConnectedThreadBt(autopilotService, mmSocket);
        autopilotService.mConnectedThreadBt.start();

        autopilotService.mDeviceName = mmDevice.getName();


        // Update UI title
        autopilotService.updateUserInterfaceTitle();

    }

    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void write(byte[] buffer) {

    }
}
