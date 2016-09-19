package com.tharvey.blocklybot;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

public abstract class Mobbob extends Robot {
    private final static String TAG = Mobbob.class.getSimpleName();

    Handler mHandler;
    static Mobbob mContext;

    static Mobbob getMobob() {
        return mContext;
    }

    // Standard Walk Commands
    public enum commands {
        STOP,
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

    private String[] command_str = { "ST", "FW", "BW", "RT", "LT", "SX", "BX",
            "WX", "WY", "WZ", "TX", "TY", "TZ",
            "LX", "LY", "LZ"};

    public Mobbob(Handler handler, String name, String address) {
        super(name, address);
        mContext = this;
        start();
    }

    /** synchronous - block until command complete */
    public void doCommand(int nCmd, int nVal) {
        if (nCmd < commands.CMD_MAX.ordinal()) {
            String cmd = command_str[nCmd];
            Log.i(TAG, "Mobob cmd:" + cmd + " val:" + nVal);
            mLastRX = "";
            serialSend("<" + cmd + "," + nVal + ">");
            while (!mLastRX.equals("<" + cmd + ">")) {
                SystemClock.sleep(100);
            }
            Log.i(TAG, "done " + cmd);
        }
    }

    /** async - add command to the queue */
    public void queueCommand(int command, int value) {
        mHandler.sendMessage(mHandler.obtainMessage(command, value, 0));
    }

    /** create a background thread who's looper plays commands from the queue */
    void start() {
        HandlerThread handlerThread = new HandlerThread("HandlerThread");
        handlerThread.start();

        // Create a handler attached to the HandlerThread's Looper
        mHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                doCommand(msg.what, msg.arg1);
            }
        };
    }
}
