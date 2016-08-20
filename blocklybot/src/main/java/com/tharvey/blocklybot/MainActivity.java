package com.tharvey.blocklybot;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

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
