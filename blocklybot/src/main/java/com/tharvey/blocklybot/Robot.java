package com.tharvey.blocklybot;

public abstract class Robot {

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
    public abstract void onConectionStateChange(connectionStateEnum theconnectionStateEnum);
    public abstract void disconnect();
    public abstract int connect();
    protected String mLastRX = "";
    protected void onSerialReceived(String theString) {
        theString = theString.replace("\r\n", "");
        System.out.println(">> " + theString);
        mLastRX = theString;
    }
    public abstract void serialSend(String theString);

    public connectionStateEnum mConnectionState = connectionStateEnum.isNull;

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
        onConectionStateChange(state);
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

