package com.tharvey.blocklybot;

import android.bluetooth.BluetoothDevice;

/**
 * Created by tharvey on 9/9/16.
 */
public interface IDiscover {
    void onDiscover(BluetoothDevice device, Boolean compatible);
    void onDiscoveryComplete();
}
