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
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class BLEScan {
	private final static String TAG = BLEScan.class.getSimpleName();

	private BluetoothAdapter mBluetoothAdapter;
	private boolean mScanning;
	private Handler mHandler;
	private Boolean mCompatOnly;
	private ArrayList<BluetoothDevice> mQueriedDevices = new ArrayList<BluetoothDevice>();
	private ArrayList<BluetoothDevice> mQueryQueue = new ArrayList<BluetoothDevice>();
	private IDiscover mCallback;
	// Stops scanning after 15 seconds.
	private static final long SCAN_PERIOD = 15000;
	BluetoothLeService mBluetoothLeService;
	private DeviceListAdapter mDeviceListAdapter;
	private Context mContext;
	private Map<String, Boolean> mKnownDevs;

	public BLEScan(Context context, DeviceListAdapter adapter, Map<String, Boolean> knownDevs, Boolean compatOnly, IDiscover callback) {
		mHandler = new Handler();
		mKnownDevs = knownDevs;
		mScanning = false;
		mCompatOnly = compatOnly;
		mDeviceListAdapter = adapter;
		mContext = context;
		mCallback = callback;
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	// Code to manage Service lifecycle.
	ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			Log.i(TAG, "mServiceConnection onServiceConnected");
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
			if (mBluetoothLeService.initialize()) {
				mHandler.postDelayed(scanStopHandler, SCAN_PERIOD);
				mScanning = true;
				mBluetoothAdapter.startLeScan(mLeScanCallback);
			} else {
				Log.e(TAG, "Unable to initialize Bluetooth");
				mBluetoothLeService = null;
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			Log.i(TAG, "mServiceConnection onServiceDisconnected");
			mBluetoothLeService.disconnect();
			mBluetoothLeService = null;
		}
	};

	private void queryNext() {
		if (mQueryQueue.size() > 0) {
			BluetoothDevice device = mQueryQueue.get(0);
			mCallback.onQuery(device);
			Log.i(TAG, "querying services for " + device.getAddress() + ":" + device.getName());
			mBluetoothLeService.connect(device.getAddress());
		} else {
			Log.i(TAG, "BLE Discovery complete - no more outstanding queries");
			done();
		}
	}

	// Handles various events fired by the Service:
	//   ACTION_GATT_CONNECTED: connected to a GATT server.
	//   ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
	//   ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
	//   ACTION_DATA_AVAILABLE: received data from the device (read result or notification)
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			BluetoothDevice device = intent.getParcelableExtra(BluetoothLeService.EXTRA_DEVICE);
			Log.d(TAG, "Received " + action + " from " + device.getAddress() + ":" + device.getName());
			if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				boolean compatible = false;
				// Show all the supported services and characteristics on the user interface.
				for (BluetoothGattService gattService : mBluetoothLeService.getSupportedGattServices()) {
					Log.i(TAG, "service:" + gattService.getUuid().toString());
					if (gattService.getUuid().equals(UUID.fromString(Bluno.DFROBOT_BLUNO_SERVICE)))
						compatible = true;
					List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
					ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();
					// Loops through available Characteristics.
					for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
						Log.i(TAG, "  char:" + gattCharacteristic.getUuid().toString());
					}
				}
				if (compatible) {
					Log.i(TAG, "Found compatible device: " + device.getName());
					mDeviceListAdapter.addDevice(device);
				}
				mCallback.onDiscover(device, compatible);
				mBluetoothLeService.disconnect();
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
				mQueryQueue.remove(device);
				mBluetoothLeService.disconnect();
				queryNext();
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				Log.d(TAG, "Data Available: " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
			}
		}
	};

	/* start scanning */
	protected boolean start() {
		Log.d(TAG, "start()");
		if (mBluetoothAdapter == null)
			return false;
		if (!mBluetoothAdapter.isEnabled())
			return false;
		if (!mScanning) {
			// setup characteristic receiver
			final IntentFilter filter = new IntentFilter();
			filter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
			filter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
			filter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
			filter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
			mContext.registerReceiver(mReceiver, filter);
			Intent gattServiceIntent = new Intent(mContext, BluetoothLeService.class);
			mContext.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
			mScanning = true;
			mQueriedDevices.clear();
			mQueryQueue.clear();
		}
		return true;
	}

	/* stop scanning */
	protected void stop() {
		Log.d(TAG, "stop()");
		if (mScanning)
			done();
	}

	/* unregister services */
	protected void done() {
		mBluetoothLeService.disconnect();
		mBluetoothAdapter.stopLeScan(mLeScanCallback);
		mContext.unbindService(mServiceConnection);
		mContext.unregisterReceiver(mReceiver);
		mScanning = false;
		mCallback.onDiscoveryComplete();
	}

	// Stops scanning after a pre-defined scan period.
	Runnable scanStopHandler = new Runnable() {
		@Override
		public void run() {
			Log.d(TAG, "scanStopHandler");
			if (mScanning) {
				if (!mCompatOnly || (mQueryQueue.size() == 0))
					done();
			}
		}
	};

	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
			if (!mScanning) {
				Log.e(TAG, "found " + device.getName() + " while not scanning");
				return;
			}
/*
            if (device == null) {
                Log.e(TAG, "null device");
                return;
            }
            if (mBluetoothLeService == null) {
                Log.e(TAG, "null service");
                return;
            }
            if (mDeviceListAdapter.contains(device))
                return;
*/
		    /* BLE scan will keep reporting the same devices over and over, unlike BT scan */
			if (mQueriedDevices.contains(device))
				return;
			mQueriedDevices.add(device);
			Log.i(TAG, "Found BLE device " + device.getAddress() + ":" + device.getName());
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
			} else {
				Log.i(TAG, "Adding to query queue");
				mCallback.onQuery(device);
				mQueryQueue.add(device);
				if (mQueryQueue.size() == 1)
					queryNext();
			}
		}
	};
}
