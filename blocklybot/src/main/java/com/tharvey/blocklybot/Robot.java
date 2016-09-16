package com.tharvey.blocklybot;

import android.util.Log;

public abstract class Robot {
    private final static String TAG = Robot.class.getSimpleName();

    static Robot mContext;
    static Robot getRobot() {
        return mContext;
    }

    private String mDeviceName;
    private String mDeviceAddress;
    private IConnection.connectionStateEnum mConnectionState = IConnection.connectionStateEnum.isNull;
    protected String mLastRX;
    private IConnection mConnectionListener = null;

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
        mLastRX = "";
    }

    public IConnection.connectionStateEnum getConnectionState() {
        return mConnectionState;
    }

    protected void setConnectionListener(IConnection listener) {
        Log.i(TAG, "setConnectionListener");
        mConnectionListener = listener;
    }

    protected void setState(IConnection.connectionStateEnum state) {
        Log.i(TAG, "setState:" + state);
        mConnectionState = state;
        if (mConnectionListener != null)
            mConnectionListener.connectionStateChanged(state);
    }

    protected void onSerialReceived(String theString) {
        theString = theString.replace("\r\n", "");
        mLastRX = theString;
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

