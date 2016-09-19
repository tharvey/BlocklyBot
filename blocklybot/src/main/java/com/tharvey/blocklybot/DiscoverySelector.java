package com.tharvey.blocklybot;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Show a dialog with scan results
 */
public class DiscoverySelector {
	private final String TAG = getClass().getSimpleName();

	private Activity mActivity;
	private BLEScan mBLEScan;
	private BluetoothScan mBTScan;
	private Handler mHandler;
	private DeviceListAdapter mDeviceListAdapter;
	private SharedPreferences mPreferences;
	private Dialog mDialog;
	private ProgressBar mProgress;
	private static final String COMPATDEVS_PREF = "pref_knowncompatibledevs";
	private Map<String, Boolean> mKnownDevs;
	private IConnection mListener;
	private String mPhase;
	private int mCount;

	public DiscoverySelector(Activity activity, IConnection listener) {
		mActivity = activity;
		mListener = listener;
		mHandler = new Handler();
		mPhase = "";
		mCount = 0;
		mPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);

		// Initializes list view adapter.
		mDeviceListAdapter = new DeviceListAdapter(mActivity);
		Boolean compatOnly = mPreferences.getBoolean("pref_filterincompatible", true);
		Boolean scanBT = mPreferences.getBoolean("pref_scanBT", true);
		Boolean scanBLE = mPreferences.getBoolean("pref_scanBLE", true);

		// see if we have BLE
		if (scanBLE && !mActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Log.i(TAG, "BLE not supported");
			scanBLE = false;
		}

		// read list of known devices cached from previous scans
		mKnownDevs = new HashMap<String, Boolean>();
		try {
			JSONObject json = new JSONObject(mPreferences.getString(COMPATDEVS_PREF, ""));
			Iterator<String> keyItr = json.keys();
			while (keyItr.hasNext()) {
				String k = keyItr.next();
				Boolean v = (Boolean) json.get(k);
				mKnownDevs.put(k, v);
			}
		} catch (Exception e) {
			Log.e(TAG, "Error: " + e);
		}
		Log.d(TAG, "Read list of known devs:");
		for (String s : mKnownDevs.keySet())
			Log.d(TAG, s + ":" + mKnownDevs.get(s));

		// BluetoothScanner
		if (scanBT) {
			mBTScan = new BluetoothScan(mActivity, mDeviceListAdapter, mKnownDevs, compatOnly, new IDiscover() {
				@Override
				public void onDiscover(BluetoothDevice device, Boolean compatible) {
					cacheDevice(device, compatible);
				}

				@Override
				public void onQuery(BluetoothDevice device) {
					mCount++;
					updateTitle();
				}

				@Override
				public void onDiscoveryComplete() {
					Log.i(TAG, "BT Discovery Complete");
					if (mBLEScan != null) {
						mPhase = "Bluetooth LE";
						updateTitle();
						mBLEScan.start();
					}
				}
			});
		}

		// BLEScanner
		if (scanBLE) {
			mBLEScan = new BLEScan(mActivity, mDeviceListAdapter, mKnownDevs, compatOnly, new IDiscover() {
				@Override
				public void onDiscover(BluetoothDevice device, Boolean compatible) {
					cacheDevice(device, compatible);
				}

				@Override
				public void onQuery(BluetoothDevice device) {
					mCount++;
					updateTitle();
				}

				@Override
				public void onDiscoveryComplete() {
					Log.i(TAG, "BLE Discovery Complete");
					mProgress.setVisibility(View.INVISIBLE);
					mPhase = "";
					mCount = 0;
					updateTitle();
				}
			});
		}
	}

	/* store device in preference cache with compatibility flag for later use */
	private void cacheDevice(BluetoothDevice device, Boolean compatible) {
		Log.i(TAG, "cacheDevice: " + device.getName() + ":" + device.getAddress() + ":" + compatible);
		mKnownDevs.put(device.getAddress(), compatible);
		JSONObject json = new JSONObject(mKnownDevs);
		Log.d(TAG, "Write list of known devs:");
		for (String s : mKnownDevs.keySet())
			Log.d(TAG, s + ":" + mKnownDevs.get(s));
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putString(COMPATDEVS_PREF, json.toString());
		editor.commit();
	}

	/* stop scanning */
	private void stop() {
		Log.i(TAG, "stop()");
		if (mBLEScan != null)
			mBLEScan.stop();
		if (mBTScan != null)
			mBTScan.stop();
		mPhase = "";
		mCount = 0;
		updateTitle();
		mProgress.setVisibility(View.INVISIBLE);
	}

	/* start scanning */
	private void start() {
		Log.i(TAG, "start()");
		mProgress.setVisibility(View.VISIBLE);
		/* if BT enabled, scan it first - BLE will be started when its complete */
		if (mBTScan != null) {
			mPhase = "Bluetooth";
			updateTitle();
			mBTScan.start();
		} else if (mBLEScan != null) {
			mPhase = "Bluetooth LE";
			updateTitle();
			mBLEScan.start();
		}
	}

	private void updateTitle() {
		String dots = "...............";
		mDialog.setTitle("Nearby Robots: " + mPhase + dots.substring(0, mCount));
	}

	/* popup dialog */
	public Dialog showDialog() {
		Log.i(TAG, "showDialog");
		// Disconnect from any currently connected Robot
		Robot robot = Mobbob.getRobot();
		if (robot != null)
			robot.disconnect();

		// make sure we have bluetooth
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter == null) {
			Toast.makeText(mActivity, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
			return null;
		}
		// make sure its enabled
		if (!adapter.isEnabled()) {
			Toast.makeText(mActivity, R.string.error_bluetooth_not_enabled, Toast.LENGTH_SHORT).show();
			return null;
		}

		// construct dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.AppCompatAlertDialogStyle);
		builder.setTitle("Nearby Robots:");
		final View view = LayoutInflater.from(mActivity.getApplicationContext()).inflate(R.layout.activity_devicelist, null);
		builder.setView(view);
		mDialog = builder.show();

		// set adapter
		ListView listView = (ListView) view.findViewById(R.id.listView);
		listView.setAdapter(mDeviceListAdapter);

		// progressbar
		mProgress = (ProgressBar) view.findViewById(R.id.progressBar);

		// install click handler
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				BluetoothDevice device = mDeviceListAdapter.getDevice(position);
				Log.i(TAG, "Selected:" + device.getName() + ":" + device.getAddress());
				connect(mDeviceListAdapter.getDevice(position));
			}
		});

		start();

		return mDialog;
	}

	/* Connect to a robot */
	private boolean connect(final BluetoothDevice device) {
		Log.i(TAG, "connecting to " + device.getName() + ":" + device.getAddress());
		Thread thread = new Thread() {
			@Override
			public void run() {
				Mobbob robot = (Mobbob) Mobbob.getRobot();
				if (robot != null)
					robot.disconnect();
				int waittimems = 0;
				// BLE device
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
						&& device.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
					robot = new Bluno(mActivity, mHandler, device);
				}
				// Bluetooth device
				else {
					robot = new Bluetooth(mActivity, mHandler, device);
				}
				robot.setConnectionListener(mListener);
				while (robot.getConnectionState() != IConnection.connectionStateEnum.isConnected) {
					if (waittimems > 5000) {
						Log.e(TAG, "Failed connectting to " + device.getName() + ":" + device.getAddress());
						// dismiss dialog
						mDialog.cancel();
						mDialog.dismiss();
						return;
					}
					try {
						sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					waittimems += 100;
				}
				Log.i(TAG, "Connected to " + device.getName() + ":" + device.getAddress());
				// save last connected robot in preferences
				SharedPreferences.Editor editor = mPreferences.edit();
				editor.putString("device_addr", device.getAddress());
				editor.putString("device_name", device.getName());
				editor.commit();
				Log.i(TAG, "saved " + device + " as autoconnect device");
				// dismiss dialog
				mDialog.cancel();
				mDialog.dismiss();
				robot.queueCommand(Mobbob.commands.BOUNCE.ordinal(), 1);
			}
		};
		stop();
		mProgress.setVisibility(View.VISIBLE);
		Toast.makeText(mActivity.getApplicationContext(),
				"Connecting to " + device.getName() + ":" + device.getAddress(),
				Toast.LENGTH_LONG).show();
		thread.start();
		return true;
	}
}
