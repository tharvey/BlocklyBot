package com.tharvey.blocklybot;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

/**
 * Manage audio playback
 */
public class Audio {
	private final static String TAG = Audio.class.getSimpleName();

	private Activity mActivity;
	private Context mContext;
	private MediaPlayer mPlayer;
	Handler mHandler;
	String mPlaying;

	public Audio(Activity activity) {
		mActivity = activity;
		mContext = activity;
		start();
	}

	/**
	 * synchronous - block until command complete
	 */
	public void doCommand(String resource) {
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
			return;
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
		while (mPlaying != null)
			SystemClock.sleep(100);
		Log.d(TAG, "done " + resource);
	}

	/**
	 * async - add command to the queue
	 */
	public void queueCommand(String msg) {
		Log.d(TAG, "Queueing '" + msg + "'");
		mHandler.sendMessage(mHandler.obtainMessage(0, msg));
	}

	/**
	 * create a background thread who's looper plays commands from the queue
	 */
	void start() {
		// Create a handler attached to the HandlerThread's Looper
		HandlerThread handlerThread = new HandlerThread("HandlerThread");
		handlerThread.start();
		mHandler = new Handler(handlerThread.getLooper()) {
			@Override
			public void handleMessage(Message msg) {
				doCommand((String) msg.obj);
			}
		};
	}

	public void pause() {
		Log.i(TAG, "pause");
		if (mPlayer != null)
			mPlayer.pause();
	}

	public void resume() {
		Log.i(TAG, "resume");
		if (mPlayer != null)
			mPlayer.start();
	}

	public void close() {
		Log.i(TAG, "close");
		if (mPlayer != null)
			mPlayer.stop();
	}
}
