package com.tharvey.blocklybot;

import android.util.Log;

public abstract class Robot {
    private final static String TAG = Robot.class.getSimpleName();

    static Robot mContext;

    static Robot getRobot() {
        return mContext;
    }

    public enum connectionStateEnum {
        isNull,
        isScanning,
        isToScan,
        isConnecting,
        isConnected,
        isDisconnecting
    };

    private String mDeviceName;
    private String mDeviceAddress;
    protected String mLastRX = "";
    public connectionStateEnum mConnectionState = connectionStateEnum.isNull;

    public abstract void serialSend(String theString);
    public abstract void disconnect();
    public abstract int connect();

    public Robot() {
        mContext = this;
    }

    public Robot(String name, String address) {
        mContext = this;
        mDeviceAddress = address;
        mDeviceName = name;
    }

    protected void setState(connectionStateEnum state) {
        mConnectionState = state;
        onConnectionStateChange(state);
    }

    protected void onSerialReceived(String theString) {
        theString = theString.replace("\r\n", "");
        Log.d(TAG, ">> " + theString);
        mLastRX = theString;
    }

    protected void onConnectionStateChange(connectionStateEnum theConnectionState) {
        switch (theConnectionState) {
            case isConnected:
                Log.i(TAG, "Connected");
                break;
            case isConnecting:
                Log.i(TAG, "Connecting");
                break;
            case isToScan:
                Log.i(TAG, "Scan");
                break;
            case isScanning:
                Log.i(TAG, "Scanning");
                break;
            case isDisconnecting:
                Log.i(TAG, "isDisconnecting");
                break;
            default:
                break;
        }
    }

    public String getAddress() {
        return mDeviceAddress;
    }

    public String getName() {
        return mDeviceName;
    }

    public String toString() {
        return mDeviceName + ":" + mDeviceAddress;
    }
}

