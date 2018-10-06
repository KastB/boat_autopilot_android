package de.kast.android.autopilot.service;

class WatchdogTCP extends Thread {
    WritableThread mHandle;
    public WatchdogTCP(WritableThread handle) {
        mHandle = handle;
    }

    public void run() {
        while(!this.isInterrupted()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return;
            }
            mHandle.write("watchdog".getBytes());
        }
    }
}
