/*
 * Copyright 2016 Tim Harvey <harvey.tim@gmail.com>
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

import android.util.Log;

public abstract class Robot {
	private final static String TAG = Robot.class.getSimpleName();

	static Robot mContext;

	static Robot getRobot() {
		return mContext;
	}

	private String mDeviceName;
	private String mDeviceAddress;
	private IConnection.connectionStateEnum mConnectionState = IConnection.connectionStateEnum.isNull;
	private IConnection mConnectionListener = null;

	public abstract void serialSend(String theString);

	public abstract void disconnect();

	public abstract int connect();

	public Robot() {
		mContext = this;
	}

	public Robot(String name, String address) {
		mContext = this;
		mDeviceAddress = address;
		mDeviceName = name;
	}

	public IConnection.connectionStateEnum getConnectionState() {
		return mConnectionState;
	}

	protected void setConnectionListener(IConnection listener) {
		Log.i(TAG, "setConnectionListener");
		mConnectionListener = listener;
	}

	protected void setState(IConnection.connectionStateEnum state) {
		Log.i(TAG, "setState:" + state);
		mConnectionState = state;
		if (mConnectionListener != null)
			mConnectionListener.connectionStateChanged(state);
	}

	public String getAddress() {
		return mDeviceAddress;
	}

	public String getName() {
		return mDeviceName;
	}

	public String toString() {
		return mDeviceName + ":" + mDeviceAddress;
	}
}

