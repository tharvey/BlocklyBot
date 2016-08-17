package com.tharvey.blocklybot;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class RobotControlActivity extends Activity {
    private final static String TAG = RobotControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceName;
    private String mDeviceAddress;
    private Button buttonScan;
    private Button buttonSerialSend;
    private EditText serialSendText;
    private TextView serialReceivedText;
    private Context mainContext=this;

    private class Robot extends Bluno {

        public Robot(Activity activity, String name, String address) {
            super(activity, name, address);
        }

        @Override
        public void onConectionStateChange(connectionStateEnum theConnectionState) {//Once connection state changes, this function will be called
            switch (theConnectionState) {                                            //Four connection state
                case isConnected:
                    System.out.println("Connected");
                    break;
                case isConnecting:
                    System.out.println("Connecting");
                    break;
                case isToScan:
                    System.out.println("Scan");
                    break;
                case isScanning:
                    System.out.println("Scanning");
                    break;
                case isDisconnecting:
                    System.out.println("isDisconnecting");
                    break;
                default:
                    break;
            }
        }
    }

    private Robot mRobot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_control);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        System.out.println("RobotControl Connecting to '" + mDeviceName + "':'" + mDeviceAddress + "'");

        mRobot = new Robot(this, mDeviceName, mDeviceAddress);

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
