package com.tharvey.blocklybot;

import android.app.Activity;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

public class Speak {
	private final static String TAG = Speak.class.getSimpleName();

	Handler mHandler;
	TextToSpeech tts;
	String mSpeaking;

	public Speak(Activity context) {
		mSpeaking = null;
		tts = new TextToSpeech(context.getApplicationContext(), new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				if (status == TextToSpeech.ERROR) {
					Log.e(TAG, "TTS init failed");
				} else if (status == TextToSpeech.SUCCESS) {
					tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
						@Override
						public void onDone(String utteranceId) {
							Log.i(TAG, "TTS complete '" + mSpeaking + "'");
							tts.stop();
							mSpeaking = null;
						}

						@Override
						public void onError(String utteranceId) {
							Log.e(TAG, "TTS error");
						}

						@Override
						public void onStart(String utteranceId) {
							Log.i(TAG, "TTS start");
						}
					});

					// TODO: select language from preferences or can I get it from global?
					int result = tts.setLanguage(Locale.US);
					if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
						Log.e(TAG, "This Language is not supported");
					}
				}
			}
		});

		start();
	}

	/**
	 * synchronous - block until command complete
	 */
	public void doCommand(String txt) {
		Log.i(TAG, "Speaking '" + txt + "'");
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utteranceId");
		mSpeaking = txt;
		tts.speak(txt, TextToSpeech.QUEUE_ADD, params);
		while (mSpeaking != null)
			SystemClock.sleep(100);
		Log.d(TAG, "done " + txt);
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

	;
}
