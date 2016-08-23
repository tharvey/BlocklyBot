package com.tharvey.blocklybot;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class RobotControlActivity extends Activity {
    private final static String TAG = RobotControlActivity.class.getSimpleName();

    private Mobbob mRobot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_control);

        final Intent intent = getIntent();
        mRobot = Mobbob.getMobob();

        if (mRobot != null)
            getActionBar().setTitle("Control Panel: " + mRobot.getName());
        getActionBar().setDisplayHomeAsUpEnabled(true);
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
        mRobot.sendCommand(cmd, 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                final Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.menu_about:
                AboutDialog about = new AboutDialog(this);
                about.setTitle("About this app");
                about.show();
                break;
        }
        return true;
    }

}
