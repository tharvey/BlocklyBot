package com.tharvey.blocklybot;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

// Adapter for holding devices found through scanning.
public class DeviceListAdapter extends BaseAdapter {
    private ArrayList<BluetoothDevice> mDevices;
    private LayoutInflater mInflator;

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    public DeviceListAdapter(Activity context) {
        super();
        mDevices = new ArrayList<BluetoothDevice>();
        mInflator = context.getLayoutInflater();
    }

    public void addDevice(BluetoothDevice device) {
        if(!mDevices.contains(device)) {
            mDevices.add(device);
            notifyDataSetChanged();
        }
    }

    public boolean contains(BluetoothDevice device) {
        return mDevices.contains(device);
    }

    public BluetoothDevice getDevice(int position) {
        return mDevices.get(position);
    }

    public void clear() {
        mDevices.clear();
        notifyDataSetChanged();
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
//            viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
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
//        viewHolder.deviceAddress.setText(device.getAddress());

        return view;
    }
}
