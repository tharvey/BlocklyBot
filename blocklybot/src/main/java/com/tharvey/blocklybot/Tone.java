package com.tharvey.blocklybot;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.SystemClock;
import android.util.Log;

/**
 * Created by tharvey on 9/17/16.
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
