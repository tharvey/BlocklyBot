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

        getActionBar().setTitle("Control Panel: " + mRobot.getName());
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void onClickLeft(View v) {
        mRobot.sendCommand(Bluno.commands.TURN_LEFT.ordinal(), 1);
    }
    public void onClickRight(View v) {
        mRobot.sendCommand(Bluno.commands.TURN_RIGHT.ordinal(), 1);
    }
    public void onClickUp(View v) {
        mRobot.sendCommand(Bluno.commands.MOVE_FORWARD.ordinal(), 1);
    }
    public void onClickDown(View v) {
        mRobot.sendCommand(Bluno.commands.MOVE_BACKWARD.ordinal(), 1);
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
