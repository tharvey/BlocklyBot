package com.tharvey.blocklybot;

import android.util.Log;

/**
 * Created by tharvey on 9/15/16.
 */
public interface IConnection {

    enum connectionStateEnum {
        isNull,
        isScanning,
        isToScan,
        isConnecting,
        isConnected,
        isDisconnecting
    };

    void connectionStateChanged(connectionStateEnum state);
}
