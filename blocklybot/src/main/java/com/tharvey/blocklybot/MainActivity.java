package com.tharvey.blocklybot;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;

import com.tharvey.blocklybot.R;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClickBLE(View v) {
        final Intent intent = new Intent(this, BLEScanActivity.class);
        startActivity(intent);
    }

    public void onClickBT(View v) {
        final Intent intent = new Intent(this, BluetoothScanActivity.class);
        startActivity(intent);
    }

    public void onClickNC(View v) {
        final Intent intent = new Intent(this, BlocklyActivity.class);
        startActivity(intent);
    }

}
