package com.tharvey.blocklybot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class RobotControlActivity extends AppCompatActivity implements IConnection {
	private final static String TAG = RobotControlActivity.class.getSimpleName();

	private Mobbob mRobot;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_robot_control);
		mRobot = Mobbob.getMobob();
		if (mRobot != null) {
			mRobot.setConnectionListener(this);
			updateStatus();
		}
	}

	@Override
	public void connectionStateChanged(IConnection.connectionStateEnum state) {
		Log.i(TAG, "connection state changed:" + state);
		mRobot = Mobbob.getMobob();
		updateStatus();
	}

	private void updateStatus() {
		Log.i(TAG, "updateStatus");
		final ActionBar action = getSupportActionBar();
		if (action != null) {
			if (mRobot != null && mRobot.getConnectionState() == IConnection.connectionStateEnum.isConnected)
				action.setTitle("Remote Control: " + mRobot.getName());
			else
				action.setTitle("Remote Control: not connected");
		}
	}

	public void onClick(View v) {
		int cmd;
		switch (v.getId()) {
			case R.id.up:
				cmd = Mobbob.commands.MOVE_FORWARD.ordinal();
				break;
			case R.id.right:
				cmd = Mobbob.commands.TURN_RIGHT.ordinal();
				break;
			case R.id.left:
				cmd = Mobbob.commands.TURN_LEFT.ordinal();
				break;
			case R.id.down:
				cmd = Mobbob.commands.MOVE_BACKWARD.ordinal();
				break;
			case R.id.shake:
				cmd = Mobbob.commands.SHAKE_HEAD.ordinal();
				break;
			case R.id.bounce:
				cmd = Mobbob.commands.BOUNCE.ordinal();
				break;
			case R.id.wobble:
				cmd = Mobbob.commands.WOBBLE.ordinal();
				break;
			case R.id.wobble_right:
				cmd = Mobbob.commands.WOBBLE_RIGHT.ordinal();
				break;
			case R.id.wobble_left:
				cmd = Mobbob.commands.WOBBLE_LEFT.ordinal();
				break;
			case R.id.tap_feet:
				cmd = Mobbob.commands.TAP_FEET.ordinal();
				break;
			case R.id.tap_right:
				cmd = Mobbob.commands.TAP_FOOT_RIGHT.ordinal();
				break;
			case R.id.tap_left:
				cmd = Mobbob.commands.TAP_FOOT_LEFT.ordinal();
				break;
			case R.id.shake_legs:
				cmd = Mobbob.commands.SHAKE_LEGS.ordinal();
				break;
			case R.id.shake_right:
				cmd = Mobbob.commands.SHAKE_LEG_RIGHT.ordinal();
				break;
			case R.id.shake_left:
				cmd = Mobbob.commands.SHAKE_LEG_LEFT.ordinal();
				break;
			default:
				cmd = Mobbob.commands.STOP.ordinal();
				break;
		}
		if (mRobot != null)
			mRobot.doFunction(null, cmd, 1);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;

		switch (item.getItemId()) {
			case R.id.menu_settings:
				intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				break;
			case R.id.menu_about:
				AboutDialog about = new AboutDialog(this);
				about.setTitle("About this app");
				about.show();
				break;
			case R.id.menu_connect:
				DiscoverySelector dialog = new DiscoverySelector(this, this);
				dialog.showDialog();
				break;
			case R.id.menu_blockly:
				intent = new Intent(this, BlocklyActivity.class);
				startActivity(intent);
				break;
		}
		return true;
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "onStop()");
		super.onStop();
		if (mRobot != null)
			mRobot.disconnect();
	}

	@Override
	protected void onStart() {
		Log.d(TAG, "onStart()");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("pref_defaultView", "panel");
		editor.commit();
		super.onStart();
		if (mRobot != null)
			mRobot.connect();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == DiscoverySelector.REQUEST_ENABLE_BT) {
			if (resultCode == RESULT_OK) {
				DiscoverySelector dialog = new DiscoverySelector(this, this);
				dialog.showDialog();
			}
		}
	}
}
