package de.kast.android.autopilot.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

/**
 * This thread runs while attempting to make an outgoing connection
 * with a device. It runs straight through; the connection either
 * succeeds or fails.
 */
class ConnectThreadTcp extends Thread implements WritableThread {
    private AutopilotService autopilotService;
    private final String mIp;
    private final int mPort;
    private Socket mmSocket;

    private OutputStream mmOutStream;
    private BufferedReader mmInStream;

    ConnectThreadTcp(AutopilotService autopilotService, String ip, int port) {
        this.autopilotService = autopilotService;
        mIp = ip;
        mPort = port;
        autopilotService.mState = AutopilotService.STATE_CONNECTING_TCP;
        autopilotService.mDeviceName = mIp + ":" + mPort;

        autopilotService.updateUserInterfaceTitle();
    }

    public void run() {
        setName("de.kast.autopilot.ConnectThreadTcp" + mIp + ":" + mPort);

        while (autopilotService.mState == AutopilotService.STATE_CONNECTING_TCP) {
            try {
                mmSocket = new Socket(mIp, mPort);
            } catch (IOException e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e3) {
                    return;
                }
                continue;
            }
            autopilotService.mState = AutopilotService.STATE_CONNECTED_TCP;

            try {
                mmOutStream = mmSocket.getOutputStream();
                mmInStream = new BufferedReader(new InputStreamReader(mmSocket.getInputStream()));
            } catch (IOException e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e2) {
                    return;
                }
                continue;
            }
            autopilotService.updateUserInterfaceTitle();
            WatchdogTCP watchdog = new WatchdogTCP(this);
            watchdog.start();

            while (autopilotService.mState == AutopilotService.STATE_CONNECTED_TCP) {
                try {
                    String msg = mmInStream.readLine();
                    autopilotService.broadcast_msg(msg);
                } catch (IOException e) {
                    if (autopilotService.mState == AutopilotService.STATE_CONNECTED_TCP) {
                        autopilotService.mState = AutopilotService.STATE_CONNECTING_TCP;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e2) {
                        return;
                    }
                    autopilotService.updateUserInterfaceTitle();
                }
            }
            watchdog.interrupt();
        }
    }

    /**
     * Write to the connected OutStream.
     *
     * @param buffer The bytes to write
     */
    public void write(final byte[] buffer) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                setName("de.kast.autopilot.tcp_writer");
                try {
                    mmOutStream.write(buffer);
                    mmOutStream.flush();
                } catch (IOException e) {
                    autopilotService.mState = AutopilotService.STATE_CONNECTING_TCP;
                    autopilotService.updateUserInterfaceTitle();
                }
            }
        }).start();
        autopilotService.broadcast_sent_msg(buffer);
    }

    public void cancel() {
        try {
            if (mmOutStream != null) {
                mmOutStream.flush();
                mmOutStream.close();
            }
            if (mmInStream != null) {
                mmInStream.close();
            }
            if (mmSocket != null) {
                mmSocket.close();
            }
        } catch (IOException e) {
            autopilotService.mState = AutopilotService.STATE_NONE;
            autopilotService.updateUserInterfaceTitle();
        }
    }
}
