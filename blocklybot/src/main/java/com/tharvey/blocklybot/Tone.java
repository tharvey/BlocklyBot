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

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * Class for playback of a Tone
 */
public class Tone implements IFunction {
	private final static String TAG = Tone.class.getSimpleName();

	static private boolean mPlaying;

	public boolean doFunction(String p1, int freqHz, int durationMs) {
		Log.i(TAG, "Tone: " + freqHz + "Hz " + durationMs + "ms");
		AudioTrack tone = generateTone(freqHz, durationMs);
		tone.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
			@Override
			public void onPeriodicNotification(AudioTrack track) {
			}

			@Override
			public void onMarkerReached(AudioTrack track) {
				mPlaying = false;
				track.release();
			}
		});
		mPlaying = true;
		tone.play();
		return true;
	}

	public boolean isBusy() {
		return mPlaying;
	}

	static private AudioTrack generateTone(double freqHz, int durationMs) {
		int count = (int) (44100.0 * 2.0 * (durationMs / 1000.0)) & ~1;
		short[] samples = new short[count];
		int size = count * (Short.SIZE / 8);
		Log.d(TAG, freqHz + "Hz for " + durationMs + "ms = " + count + " samples at 44.1Khz 2ch = " + size + " bytes");
		for (int i = 0; i < count; i += 2) {
			short sample = (short) (Math.sin(2 * Math.PI * i / (44100.0 / freqHz)) * 0x7FFF * .75);
			samples[i + 0] = sample;
			samples[i + 1] = sample;
		}
		AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
				AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
				size, AudioTrack.MODE_STATIC);
		track.setNotificationMarkerPosition(count / 2);
		track.write(samples, 0, count);
		return track;
	}
}
