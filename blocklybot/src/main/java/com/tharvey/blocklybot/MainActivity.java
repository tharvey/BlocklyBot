package com.tharvey.blocklybot;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
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
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
	private final static String TAG = MainActivity.class.getSimpleName();

	private SharedPreferences mSharedPref;
	private List<String> mPermissionsWaiting = new ArrayList<>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		checkPermissions(this);

		mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		if (!mSharedPref.getBoolean("installed", false)) {
			Log.i(TAG, "Copying example files on first run...");
			mSharedPref.edit().putBoolean("installed", true).commit();
			copyAssetFolder(getAssets(), "files", getFilesDir().getPath() /*+ "/samples"*/);
		}

		Log.d(TAG, "waiting for " + mPermissionsWaiting.size() + " responses");
		if (mPermissionsWaiting.size() == 0) {
			launchPreferredActivity();
		}
	}

	protected void launchPreferredActivity() {
		String controller = mSharedPref.getString("pref_defaultView", "blockly");
		Log.i(TAG, "Controller:" + controller);
		if (controller.equals("panel")) {
			final Intent intent = new Intent(this, RobotControlActivity.class);
			startActivity(intent);
		} else {
			final Intent intent = new Intent(this, BlocklyActivity.class);
			startActivity(intent);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		Log.d(TAG, "onRequestPermissionsResult: code=" + requestCode);
		for (int i = 0; i < permissions.length; i++) {
			Log.d(TAG, i + ":" + permissions[i] + ":" + grantResults[i]);
			mPermissionsWaiting.remove(permissions[i]);
		}
		Log.d(TAG, "now waiting for " + mPermissionsWaiting.size() + " responses");
		for (int i = 0; i < mPermissionsWaiting.size(); i++)
			Log.d(TAG, i + ":" + mPermissionsWaiting.get(i));
		if (mPermissionsWaiting.size() == 0) {
			launchPreferredActivity();
		}
	}

	public void checkPermissions(final Activity activity) {
		// Android M Permission checks
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			String[] allPermissionNeeded = {
					Manifest.permission.RECORD_AUDIO,
					Manifest.permission.ACCESS_COARSE_LOCATION,
			};
			String[] permissionNames = {
					"audio recording",
					"location access"
			};
			String[] permissionExplanations = {
					"listen for voice commands",
					"detect Bluetooth Low Energy devices"
			};
			String message = "";

			Log.d(TAG, "Checking Android M permissions");
			for (int i = 0; i < allPermissionNeeded.length; i++) {
				String permission = allPermissionNeeded[i];
				String name = permissionNames[i];
				String explanation = permissionExplanations[i];
				if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
					if (activity.shouldShowRequestPermissionRationale(permission))
						message += "Please grant " + name + " so that it can " + explanation + "." + "\r\n\r\n";
					mPermissionsWaiting.add(permission);
				}
			}

			if (mPermissionsWaiting.size() > 0) {
				if (!message.equals("")) {
					final android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(activity);
					builder.setTitle("This app requires the following permissions");
					builder.setMessage(message);
					builder.setPositiveButton(android.R.string.ok, null);
					builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
						@Override
						@TargetApi(23)
						public void onDismiss(DialogInterface dialog) {
							requestPermissions(mPermissionsWaiting.toArray(new String[0]), 0);
						}
					});
					builder.show();
				} else {
					requestPermissions(mPermissionsWaiting.toArray(new String[0]), 0);
				}
			}
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
