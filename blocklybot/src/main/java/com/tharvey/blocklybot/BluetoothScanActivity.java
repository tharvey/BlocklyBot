/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tharvey.blocklybot;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class BluetoothScanActivity extends AppCompatActivity {
    private final static String TAG = BluetoothScanActivity.class.getSimpleName();

    private DeviceListAdapter mDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private ListView m_listView;
    private Activity m_Activity;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"; /* RFComm Service */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devicelist);
        m_Activity = this;
        mHandler = new Handler();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
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

        m_listView = (ListView) findViewById(R.id.listView);
        m_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                connect(mDeviceListAdapter.getDevice(position));
            }
        });

        // Initializes list view adapter.
        mDeviceListAdapter = new DeviceListAdapter();
        m_listView.setAdapter(mDeviceListAdapter);
        if (false /* show paired devices */) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                Log.i(TAG, "Adding " + device);
                mDeviceListAdapter.addDevice(device);
                mDeviceListAdapter.notifyDataSetChanged();
            }
        }
    }

    // Connect to a robot
    private int connect(final BluetoothDevice device) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                Bluetooth mRobot = new Bluetooth(m_Activity, mHandler, device);
                while (mRobot.mConnectionState != Robot.connectionStateEnum.isConnected) {
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.i(TAG, "Connected to " + device.getName() + ":" + device.getAddress());
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(m_Activity);
                String controller = sharedPref.getString("pref_controlType", "");
                if (controller.equals("panel")) {
                    final Intent intent = new Intent(m_Activity, RobotControlActivity.class);
                    startActivity(intent);
                } else {
                    final Intent intent = new Intent(m_Activity, BlocklyActivity.class);
                    startActivity(intent);
                }
            }
        };

        mBluetoothAdapter.cancelDiscovery();
        Toast.makeText(getApplicationContext(),
                "Connecting to " + device.getName() + ":" + device.getAddress(),
                Toast.LENGTH_LONG).show();
        thread.start();
        return 0;
    }

    // Create a BroadcastReceiver for ACTION_FOUND and ACTION_DISCOVERY_FINISHED
    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                Log.d(TAG, "Device found: " + device.toString());
                if (false) {
                    /* show all discovered bluetooth devices */
                    Log.i(TAG, "clearing list");
                    mDeviceListAdapter.addDevice(device);
                    mDeviceListAdapter.notifyDataSetChanged();
                } else {
                    /* show only devices matching Rfcomm Service */
                    device.fetchUuidsWithSdp();
                }
            }
            // When discovery is completed
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.i(TAG, "Discovery finished");
                invalidateOptionsMenu();
            }
            // When SDP records are found from the call to fetchUuidsWithSdp()
            else if (BluetoothDevice.ACTION_UUID.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(TAG, "Services discovered for " + device);
                Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                if (uuidExtra != null) {
                    for (int i=0; i<uuidExtra.length; i++) {
                        Log.i(TAG, "Service:" + uuidExtra[i].toString());
                        // Well-known SPP UUID for RFComm
                        if (uuidExtra[i].toString().equalsIgnoreCase(SPP_UUID)) {
                            Log.i(TAG, "Found compatible device: Adding " + device);
                            mDeviceListAdapter.addDevice(device);
                            mDeviceListAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan, menu);
        if (!mBluetoothAdapter.isDiscovering()) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                if (!mBluetoothAdapter.isDiscovering()) {
                    Log.i(TAG, "clearing list");
                    mDeviceListAdapter.clear();
                    mDeviceListAdapter.notifyDataSetChanged();
                    mBluetoothAdapter.startDiscovery();
                }
                break;
            case R.id.menu_stop:
                mBluetoothAdapter.cancelDiscovery();
                break;
        }
        invalidateOptionsMenu();
        return true;
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop()");
        super.onStop();
        mBluetoothAdapter.cancelDiscovery();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart()");
        super.onStart();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_UUID);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);

/* lets leave the list populated
        Log.i(TAG, "clearing list");
        mDeviceListAdapter.clear();
        mDeviceListAdapter.notifyDataSetChanged();
*/
        mBluetoothAdapter.startDiscovery();
        invalidateOptionsMenu();
    }

    // handle result of enable bluetooth request (above)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Adapter for holding devices found through scanning.
    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
    private class DeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mDevices;
        private LayoutInflater mInflator;

        public DeviceListAdapter() {
            super();
            mDevices = new ArrayList<BluetoothDevice>();
            mInflator = BluetoothScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mDevices.contains(device)) {
                mDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mDevices.get(position);
        }

        public void clear() {
            mDevices.clear();
        }

        @Override
        public int getCount() {
            return mDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }
}
