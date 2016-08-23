package com.tharvey.blocklybot;

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

public class Bluno extends Mobbob {
	private final static String TAG = Bluno.class.getSimpleName();
	private int mBaudrate = 115200;    //set the default baud rate to 115200
	private String mPassword = "AT+PASSWOR=DFRobot\r\n";
	private String mBaudrateBuffer = "AT+CURRUART=" + mBaudrate + "\r\n";
	private static BluetoothGattCharacteristic mSCharacteristic;
	private static BluetoothGattCharacteristic mModelNumberCharacteristic;
	private static BluetoothGattCharacteristic mSerialPortCharacteristic;
	private static BluetoothGattCharacteristic mCommandCharacteristic;
	private final Handler mHandler;
	BluetoothLeService mBluetoothLeService;
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
			new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

	public Bluno(Context context, Handler handler, BluetoothDevice device) {
		super(handler, device.getName(), device.getAddress());
		mHandler = handler;

		context.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

		Intent gattServiceIntent = new Intent(context, BluetoothLeService.class);
		context.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
	}

	public void onConectionStateChange(connectionStateEnum theConnectionState) {
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

	public void serialSend(String theString) {
		if (mConnectionState == connectionStateEnum.isConnected) {
			Log.d(TAG, ">> " + theString);
			mSCharacteristic.setValue(theString);
			mBluetoothLeService.writeCharacteristic(mSCharacteristic);
		}
	}

	private Runnable mConnectingOverTimeRunnable = new Runnable() {
		@Override
		public void run() {
			if (mConnectionState == connectionStateEnum.isConnecting)
				setState(connectionStateEnum.isToScan);
			mBluetoothLeService.close();
		}
	};

	private Runnable mDisonnectingOverTimeRunnable = new Runnable() {
		@Override
		public void run() {
			if (mConnectionState == connectionStateEnum.isDisconnecting)
				setState(connectionStateEnum.isToScan);
			mBluetoothLeService.close();
		}
	};

	public static final String SerialPortUUID = "0000dfb1-0000-1000-8000-00805f9b34fb";
	public static final String CommandUUID = "0000dfb2-0000-1000-8000-00805f9b34fb";
	public static final String ModelNumberStringUUID = "00002a24-0000-1000-8000-00805f9b34fb";

	// Handles various events fired by the Service:
	//   ACTION_GATT_CONNECTED: connected to a GATT server.
	//   ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
	//   ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
	//   ACTION_DATA_AVAILABLE: received data from the device (read result or notification)
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			Log.d(TAG, "mGattUpdateReceiver->onReceive->action=" + action);
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				Log.i(TAG, "bluno ACTION_GATT_CONNECTED");
				if (mConnectionState != connectionStateEnum.isConnected) {
					setState(connectionStateEnum.isConnected);
					start();
					mHandler.removeCallbacks(mConnectingOverTimeRunnable);
				}
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
				Log.i(TAG, "bluno ACTION_GATT_DISCONNECTED");
				if (mConnectionState == connectionStateEnum.isConnected) {
					setState(connectionStateEnum.isToScan);
					mHandler.removeCallbacks(mDisonnectingOverTimeRunnable);
					mBluetoothLeService.close();
				}
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				// Show all the supported services and characteristics on the user interface.
				for (BluetoothGattService gattService : mBluetoothLeService.getSupportedGattServices()) {
					Log.i(TAG, "bluno ACTION_GATT_SERVICES_DISCOVERED  " +
							gattService.getUuid().toString());
				}
				getGattServices(mBluetoothLeService.getSupportedGattServices());
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				if (mSCharacteristic == mModelNumberCharacteristic) {
					if (intent.getStringExtra(BluetoothLeService.EXTRA_DATA).toUpperCase().startsWith("DF BLUNO")) {
						mBluetoothLeService.setCharacteristicNotification(mSCharacteristic, false);
						mSCharacteristic = mCommandCharacteristic;
						mSCharacteristic.setValue(mPassword);
						mBluetoothLeService.writeCharacteristic(mSCharacteristic);
						mSCharacteristic.setValue(mBaudrateBuffer);
						mBluetoothLeService.writeCharacteristic(mSCharacteristic);
						mSCharacteristic = mSerialPortCharacteristic;
						mBluetoothLeService.setCharacteristicNotification(mSCharacteristic, true);
						setState(connectionStateEnum.isConnected);
					} else {
						setState(connectionStateEnum.isToScan);
					}
				} else if (mSCharacteristic == mSerialPortCharacteristic) {
					onSerialReceived(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
				}
			}
		}
	};

	// Code to manage Service lifecycle.
	ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			Log.i(TAG, "mServiceConnection onServiceConnected");
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
			}
			connect();
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			Log.i(TAG, "mServiceConnection onServiceDisconnected");
			mBluetoothLeService.disconnect();
			mBluetoothLeService = null;
		}
	};

	private void getGattServices(List<BluetoothGattService> gattServices) {
		if (gattServices == null)
			return;
		String uuid;
		mModelNumberCharacteristic = null;
		mSerialPortCharacteristic = null;
		mCommandCharacteristic = null;
		mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

		// Loops through available GATT Services.
		for (BluetoothGattService gattService : gattServices) {
			uuid = gattService.getUuid().toString();
			Log.d(TAG, "displayGattServices + uuid=" + uuid);

			List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
			ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

			// Loops through available Characteristics.
			for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
				charas.add(gattCharacteristic);
				uuid = gattCharacteristic.getUuid().toString();
				if (uuid.equals(ModelNumberStringUUID)) {
					mModelNumberCharacteristic = gattCharacteristic;
				} else if (uuid.equals(SerialPortUUID)) {
					mSerialPortCharacteristic = gattCharacteristic;
				} else if (uuid.equals(CommandUUID)) {
					mCommandCharacteristic = gattCharacteristic;
				}
			}
			mGattCharacteristics.add(charas);
		}

		if (mModelNumberCharacteristic == null || mSerialPortCharacteristic == null || mCommandCharacteristic == null) {
			setState(connectionStateEnum.isToScan);
		} else {
			mSCharacteristic = mModelNumberCharacteristic;
			mBluetoothLeService.setCharacteristicNotification(mSCharacteristic, true);
			mBluetoothLeService.readCharacteristic(mSCharacteristic);
		}
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		return intentFilter;
	}

	public synchronized int connect() {
		Log.d(TAG, "connect " + toString());
		return (mBluetoothLeService.connect(getAddress()) ? 0 : 1);
	}

	public synchronized void disconnect() {
		Log.d(TAG, "disconnect " + toString());
		mBluetoothLeService.disconnect();
	}
}