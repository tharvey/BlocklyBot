package com.tharvey.blocklybot;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
	private final static String TAG = MainActivity.class.getSimpleName();

	private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1;
	private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
	private static final int REQUEST_ENABLE_BT = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			// Android M Permission checks
			Log.d(TAG, "Checking Android M permissions");

			// Audio recording required for voice recognition
			if (this.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
				final android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
				builder.setTitle("This app needs audio recording permission");
				builder.setMessage("Please grant audio recording permsision so this app can listen for voice commands.");
				builder.setPositiveButton(android.R.string.ok, null);
				builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
					}
				});
				builder.show();
			}

			// Location required for BLE scanning (yes, really it is... starting in Android 6.0)
			if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				final android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
				builder.setTitle("This app needs location access");
				builder.setMessage("Please grant location access so this app can detect Bluetooth Low Energy devices.");
				builder.setPositiveButton(android.R.string.ok, null);
				builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
					}
				});
				builder.show();
			}
		}

		// Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
		// fire an intent to display a dialog asking the user to grant permission to enable it.
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if (!adapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		if (sharedPref.getBoolean("installed", false)) {
			Log.i(TAG, "Copying example files on first run...");
			sharedPref.edit().putBoolean("installed", true).commit();
			copyAssetFolder(getAssets(), "files", getFilesDir().getPath() /*+ "/samples"*/);
		}

		String controller = sharedPref.getString("pref_defaultView", "blockly");
		Log.i(TAG, "Controller:" + controller);
		if (controller.equals("panel")) {
			final Intent intent = new Intent(this, RobotControlActivity.class);
			startActivity(intent);
		} else {
			final Intent intent = new Intent(this, BlocklyActivity.class);
			startActivity(intent);
		}
	}

	private static boolean copyAssetFolder(AssetManager assetManager,
	                                       String fromAssetPath, String toPath) {
		try {
			String[] files = assetManager.list(fromAssetPath);
			new File(toPath).mkdirs();
			boolean res = true;
			for (String file : files) {
				Log.i(TAG, file);
				if (file.contains("."))
					res &= copyAsset(assetManager,
							fromAssetPath + "/" + file,
							toPath + "/" + file);
				else
					res &= copyAssetFolder(assetManager,
							fromAssetPath + "/" + file,
							toPath + "/" + file);
			}
			return res;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private static boolean copyAsset(AssetManager assetManager,
	                                 String fromAssetPath, String toPath) {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = assetManager.open(fromAssetPath);
			new File(toPath).createNewFile();
			out = new FileOutputStream(toPath);
			copyFile(in, out);
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private static void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while ((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}
	}
}
