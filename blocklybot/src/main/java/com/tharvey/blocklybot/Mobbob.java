package com.tharvey.blocklybot;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;

public abstract class Mobbob extends Robot {
    private final static String TAG = Mobbob.class.getSimpleName();

    public boolean mConnected = false;
    Handler mHandler;
    int mCommands = 0;

    static Mobbob mContext;

    static Mobbob getMobob() {
        return mContext;
    }

    // Standard Walk Commands
    public enum commands {
        MOVE_FORWARD,
        MOVE_BACKWARD,
        TURN_RIGHT,
        TURN_LEFT,
        SHAKE_HEAD,
        BOUNCE,
        WOBBLE,
        WOBBLE_LEFT,
        WOBBLE_RIGHT,
        TAP_FEET,
        TAP_FOOT_LEFT,
        TAP_FOOT_RIGHT,
        SHAKE_LEGS,
        SHAKE_LEG_LEFT,
        SHAKE_LEG_RIGHT,
        CMD_MAX,
    };

    private String[] command_str = { "FW", "BW", "LT", "RT", "SX", "BX",
            "WX", "WY", "WZ", "TX", "TY", "TZ",
            "LX", "LY", "LZ"};

    public Mobbob() {
        super();
        mContext = this;
    }

    public Mobbob(String name, String address) {
        super(name, address);
        mContext = this;
    }

    public void sendCommand(int command, int value) {
        mHandler.sendMessage(mHandler.obtainMessage(command, value, 0));
    }

    void start() {
        HandlerThread handlerThread = new HandlerThread("HandlerThread");
        handlerThread.start();

        // Create a handler attached to the HandlerThread's Looper
        mHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what < commands.CMD_MAX.ordinal()) {
                    String cmd = command_str[msg.what];
                    System.out.println("Bluno cmd:" + cmd + " val:" + msg.arg1);
                    serialSend("<" + cmd + "," + msg.arg1 + ">");
                    while (!mLastRX.equals("<" + cmd + ">")) {
                        SystemClock.sleep(100);
                    }
                    System.out.println("done " + cmd);
                }
            }
        };
    }
}
