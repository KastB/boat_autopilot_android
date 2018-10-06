package de.kast.android.autopilot.service;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This thread runs during a connection with a remote device.
 * It handles all incoming and outgoing transmissions.
 */
class ConnectedThreadBt extends Thread implements WritableThread {
    private AutopilotService autopilotService;
    protected final BluetoothSocket mmSocket;
    protected final InputStream mmInStream;
    protected final OutputStream mmOutStream;

    public ConnectedThreadBt(AutopilotService autopilotService, BluetoothSocket socket) {
        this.autopilotService = autopilotService;
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the BluetoothSocket input and output streams
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
        autopilotService.mState = AutopilotService.STATE_CONNECTED_BT;
        autopilotService.updateUserInterfaceTitle();
    }

    public void run() {
        setName("de.kast.autopilot.ConnectThreadBT");
        byte[] buffer = new byte[1024];
        int bytes;
        String readMessage = "";
        int index = -1;
        // Keep listening to the InputStream while connected
        while (autopilotService.mState == AutopilotService.STATE_CONNECTED_BT) {
            try {
                // Read from the InputStream
                bytes = mmInStream.read(buffer);
                // Send the obtained bytes to the UI Activity
                readMessage = readMessage.concat(new String(buffer, 0, bytes));
                index = readMessage.indexOf("\r");
                String tmp = null;
                if (index != -1) {
                    tmp = readMessage.substring(0, index);
                    int index2 = tmp.indexOf("\n");
                    if (index2 != -1) {
                        tmp = tmp.substring(index2 + 1, tmp.length());
                    }
                } else {
                    index = readMessage.indexOf('\n');
                    if (index > 0) {
                        tmp = readMessage.substring(0, index);
                    }
                }
                if (index + 1 < readMessage.length())
                    readMessage = readMessage.substring(index + 1, readMessage.length());
                else
                    readMessage = "";
                autopilotService.broadcast_msg(tmp);
            } catch (IOException e) {
                autopilotService.mState = AutopilotService.STATE_NONE;
                autopilotService.updateUserInterfaceTitle();
                break;
            }
        }
    }

    /**
     * Write to the connected OutStream.
     *
     * @param buffer The bytes to write
     */
    public void write(byte[] buffer) {
        try {
            mmOutStream.write(buffer);

            autopilotService.broadcast_sent_msg(buffer);
        } catch (IOException e) {
        }
    }

    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
        }
    }
}
