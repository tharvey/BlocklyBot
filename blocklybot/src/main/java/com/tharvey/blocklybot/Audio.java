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

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Environment;
import android.util.Log;

/**
 * Manage audio playback
 */
public class Audio implements IFunction {
	private final static String TAG = Audio.class.getSimpleName();

	private Context mContext;
	private MediaPlayer mPlayer;
	String mPlaying;

	public Audio(Activity activity) {
		mContext = activity;
	}

	public boolean isBusy() {
		return (mPlaying != null);
	}

	public boolean doFunction(String resource, int p2, int p3) {
		Log.i(TAG, "Dir:" + Environment.getExternalStorageDirectory());
		Log.i(TAG, "Playing '" + resource + "'");
		int id;
		if (resource.equals("laser"))
			id = R.raw.laser;
		else if (resource.equals("laser1"))
			id = R.raw.laser1;
		else if (resource.equals("laser2"))
			id = R.raw.laser2;
		else if (resource.equals("laser1"))
			id = R.raw.laser1;
		else if (resource.equals("beep"))
			id = R.raw.beep;
		else if (resource.equals("beep1"))
			id = R.raw.beep1;
		else if (resource.equals("beep2"))
			id = R.raw.beep2;
		else if (resource.equals("beep3"))
			id = R.raw.beep3;
		else if (resource.equals("beep4"))
			id = R.raw.beep4;
		else if (resource.equals("cheer"))
			id = R.raw.cheer;
		else if (resource.equals("cheer1"))
			id = R.raw.cheer1;
		else if (resource.equals("goal"))
			id = R.raw.goal;
		else if (resource.equals("yawn"))
			id = R.raw.yawn;
		else
			return false;
		mPlaying = resource;
		mPlayer = MediaPlayer.create(mContext, id);
		mPlayer.setLooping(false);
		mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				Log.d(TAG, "complete: " + mPlaying);
				mPlaying = null;
				mp.release();
			}
		});
		mPlayer.start();
		return true;
	}
}
