package com.tharvey.blocklybot;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
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

        // getActionBar() returns null when using blocklyTheme
//        getActionBar().setTitle(mDeviceName + ":" + mDeviceAddress);
//        getActionBar().setDisplayHomeAsUpEnabled(true);
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
}
