/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tharvey.blocklybot;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Map;

/**
 * Activity for scanning Bluetooth devices.
 */
public class BluetoothScan {
    private final static String TAG = BluetoothScan.class.getSimpleName();

    private DeviceListAdapter mDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private Boolean mCompatOnly;
    private static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"; /* RFComm Service */
    private Context mContext;
    private Boolean mScanning;
    private DiscoverInterface mCallback;
    private ArrayList<BluetoothDevice> mQueryQueue;
    private Map<String, Boolean> mKnownDevs;

    public BluetoothScan(Context context, DeviceListAdapter adapter, Map<String, Boolean> knownDevs, Boolean compatOnly, DiscoverInterface callback) {
        mContext = context;
        mKnownDevs = knownDevs;
        mDeviceListAdapter = adapter;
        mCallback = callback;
        mCompatOnly = compatOnly;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mScanning = false;
        mQueryQueue = new ArrayList<>();
    }

    // Create a BroadcastReceiver
    //  ACTION_FOUND - discovered device
    //  ACTION_UUID - discovered services for a device
    //  ACTION_DISCOVERY_FINISHED - discovery complete
    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (mDeviceListAdapter.contains(device))
                    return;
                // skip BLE devices - some such as Bluno Beetle show in BT discovery as well
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && device.getType() == BluetoothDevice.DEVICE_TYPE_LE)
                    return;
                Log.i(TAG, "Found BT device " + device.getAddress() + ":" + device.getName());
                if (mKnownDevs.containsKey(device.getAddress())) {
                    if (mKnownDevs.get(device.getAddress())) {
                        Log.i(TAG, "adding known compatible device");
                        mDeviceListAdapter.addDevice(device);
                    } else {
                        Log.i(TAG, "skipping known uncompatible device");
                    }
                    return;
                }
                /* unknown device */
                if (!mCompatOnly) {
                    Log.i(TAG, "adding");
                    mDeviceListAdapter.addDevice(device);
                }
                // query services for this device
                else if (!mQueryQueue.contains(device)) {
                    Log.d(TAG, "Querying services for " + device);
                    mQueryQueue.add(device);
                    device.fetchUuidsWithSdp();
                }
            }
            // When discovery is completed
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.i(TAG, "BT Discovery finished");
                mScanning = false;
                if (mQueryQueue.size() == 0)
                    done();
            }
            // When SDP records are found from the call to fetchUuidsWithSdp()
            else if (BluetoothDevice.ACTION_UUID.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mQueryQueue.remove(device);
                Boolean compatible = false;
                Log.i(TAG, "Services discovered for " + device + " (waiting on " + mQueryQueue.size() + " more scanning=" + mScanning + ")");
                Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                if (uuidExtra != null) {
                    Log.d(TAG, "Got " + uuidExtra.length + " services:");
                    for (int i = 0; i < uuidExtra.length; i++) {
                        Log.d(TAG, "  " + uuidExtra[i].toString());
                        // Well-known SPP UUID for RFComm
                        if (uuidExtra[i].toString().equalsIgnoreCase(SPP_UUID))
                            compatible = true;
                    }
                }
                if (compatible) {
                    Log.i(TAG, "Found compatible device: " + device.getName());
                    mDeviceListAdapter.addDevice(device);
                }
                mCallback.onDiscover(device, compatible);
                if (!mScanning && mQueryQueue.size() == 0)
                    done();
            }
        }
    };

    private void done() {
        Log.i(TAG, "BT Discovery complete - no more outstanding queries");
        mCallback.onDiscoveryComplete();
        mContext.unregisterReceiver(mReceiver);
    }

    /* start scanning */
    protected boolean start() {
        Log.i(TAG, "start()");
        if (mBluetoothAdapter == null)
            return false;
        if (!mBluetoothAdapter.isEnabled())
            return false;
        if (!mScanning) {
            // Register the BroadcastReceiver
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothDevice.ACTION_UUID);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            mContext.registerReceiver(mReceiver, filter);
            mScanning = true;
            mBluetoothAdapter.startDiscovery();
        }
        return true;
    }

    /* stop scanning */
    protected void stop() {
        Log.i(TAG, "stop()");
        if (mScanning) {
            mBluetoothAdapter.cancelDiscovery();
            mContext.unregisterReceiver(mReceiver);
            mScanning = false;
        }
    }
}
