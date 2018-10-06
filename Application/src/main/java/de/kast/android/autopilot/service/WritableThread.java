package de.kast.android.autopilot.service;

interface WritableThread {
    void write(byte[] buffer);

    void cancel();

    void start();
}
