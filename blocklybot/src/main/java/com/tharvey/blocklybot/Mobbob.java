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

import android.os.Handler;
import android.util.Log;

public abstract class Mobbob extends Robot implements IFunction {
	private final static String TAG = Mobbob.class.getSimpleName();

	private String mPlaying;
	static Mobbob mContext;
	private String mLastRX;

	static Mobbob getMobob() {
		return mContext;
	}

	// Standard Walk Commands
	public enum commands {
		STOP,
		MOVE_FORWARD,
		MOVE_BACKWARD,
		TURN_RIGHT,
		TURN_LEFT,
		SHAKE_HEAD,
		BOUNCE,
		WOBBLE,
		WOBBLE_LEFT,
		WOBBLE_RIGHT,
		TAP_FEET,
		TAP_FOOT_LEFT,
		TAP_FOOT_RIGHT,
		SHAKE_LEGS,
		SHAKE_LEG_LEFT,
		SHAKE_LEG_RIGHT,
		CMD_MAX,
	};

	private String[] command_str = {"ST", "FW", "BW", "RT", "LT", "SX", "BX",
			"WX", "WY", "WZ", "TX", "TY", "TZ",
			"LX", "LY", "LZ"};

	public Mobbob(Handler handler, String name, String address) {
		super(name, address);
		mContext = this;
		mLastRX = "";
	}

	static public int command(String str) {
		int cmd = Mobbob.commands.STOP.ordinal();
		switch (str.toUpperCase()) {
			case "MOVEFORWARD":
				cmd = Mobbob.commands.MOVE_FORWARD.ordinal();
				break;
			case "MOVEBACKWARD":
				cmd = Mobbob.commands.MOVE_BACKWARD.ordinal();
				break;
			case "TURNRIGHT":
				cmd = Mobbob.commands.TURN_RIGHT.ordinal();
				break;
			case "TURNLEFT":
				cmd = Mobbob.commands.TURN_LEFT.ordinal();
				break;
			case "SHAKEHEAD":
				cmd = Mobbob.commands.SHAKE_HEAD.ordinal();
				break;
			case "BOUNCE":
				cmd = Mobbob.commands.BOUNCE.ordinal();
				break;
			case "WOBBLE":
				cmd = Mobbob.commands.WOBBLE.ordinal();
				break;
			case "WOBBLELEFT":
				cmd = Mobbob.commands.WOBBLE_LEFT.ordinal();
				break;
			case "WOBBLERIGHT":
				cmd = Mobbob.commands.WOBBLE_RIGHT.ordinal();
				break;
			case "TAPFEET":
				cmd = Mobbob.commands.TAP_FEET.ordinal();
				break;
			case "TAPFOOTLEFT":
				cmd = Mobbob.commands.TAP_FOOT_LEFT.ordinal();
				break;
			case "TAPFOOTRIGHT":
				cmd = Mobbob.commands.TAP_FOOT_RIGHT.ordinal();
				break;
			case "SHAKELEGS":
				cmd = Mobbob.commands.SHAKE_LEGS.ordinal();
				break;
			case "SHAKELEGLEFT":
				cmd = Mobbob.commands.SHAKE_LEG_LEFT.ordinal();
				break;
			case "SHAKELEGRIGHT":
				cmd = Mobbob.commands.SHAKE_LEG_RIGHT.ordinal();
				break;
			case "STOP":
				cmd = Mobbob.commands.STOP.ordinal();
				break;
			default:
				Log.e(TAG, "Unrecognized cmd:" + cmd);
				break;
		}
		return cmd;
	}

	protected void onSerialReceived(String str) {
		str = str.replace("\r\n", "");
		mLastRX = str;
		Log.i(TAG, "<< " + str);
		if (mPlaying != null && mLastRX.equals("<" + mPlaying + ">")) {
			Log.i(TAG, "command " + mPlaying + " complete");
			mPlaying = null;
		}
	}


	public boolean isBusy() {
		return (mPlaying != null);
	}

	public boolean doFunction(String p1, int nCmd, int nVal) {
		if (p1 != null)
			nCmd = command(p1);
		if (!isBusy() && nCmd < commands.CMD_MAX.ordinal()) {
			mPlaying = command_str[nCmd];
			Log.i(TAG, "Mobob cmd:" + mPlaying + " val:" + nVal);
			mLastRX = "";
			serialSend("<" + mPlaying + "," + nVal + ">");
			return true;
		}
		return false;
	}
}
