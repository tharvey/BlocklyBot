package com.tharvey.blocklybot;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

/**
 * Manage voice recognition service allowing for registring a set of phrases and a callback
 */
public class Listen implements RecognitionListener {
	private final static String TAG = Listen.class.getSimpleName();

	private SpeechRecognizer mRecognizer;
	private Activity mActivity;
	private IEventListener mEventListener;
	private boolean mSetup;
	private boolean mListening;

	public Listen(Activity activity, final List<String> phrases, IEventListener callback) {
		mActivity = activity;
		mEventListener = callback;
		mSetup = false;
		mListening = false;

		// Recognizer initialization is a time-consuming and it involves IO,
		// so we execute it in async task
		new AsyncTask<Void, Void, Exception>() {
			@Override
			protected Exception doInBackground(Void... params) {
				try {
					Assets assets = new Assets(mActivity);
					File assetDir = assets.syncAssets();
					setupRecognizer(assetDir, phrases);
				} catch (IOException e) {
					Log.e(TAG, "Error setting up recognizer:" + e);
					return e;
				}
				return null;
			}

			@Override
			protected void onPostExecute(Exception result) {
				Log.i(TAG, "onPostExecute:" + result);
				if (result != null) {
					Log.e(TAG, "Failed to init recognizer " + result);
				} else {
					Log.i(TAG, "Setup ok - initializing search");
					mListening = true;
					mRecognizer.startListening("robot");
				}
			}
		}.execute();
	}

	public boolean isSetup() {
		return mSetup;
	}

	public void pause() {
		Log.i(TAG, "pause");
		if (mListening) {
			mListening = false;
			if (mRecognizer != null) {
				mRecognizer.stop();
			}
		}
	}

	public void resume() {
		Log.i(TAG, "resume");
		if (!mListening) {
			mListening = true;
			if (mRecognizer != null) {
				mRecognizer.startListening("robot");
			}
		}
	}

	public void close() {
		Log.i(TAG, "close");
		if (mRecognizer != null) {
			mRecognizer.cancel();
			mRecognizer.shutdown();
		}
	}

	/**
	 * In partial result we get quick updates about current hypothesis. In
	 * keyword spotting mode we can react here, in other modes we need to wait
	 * for final result in onResult.
	 */
	@Override
	public void onPartialResult(Hypothesis hypothesis) {
		if (!mListening || hypothesis == null)
			return;
		String text = hypothesis.getHypstr().trim();
		Log.d(TAG, "onPartialResult:" + text);
		mEventListener.onEvent("listen", text);
	}

	/**
	 * This callback is called when we stop the recognizer.
	 */
	@Override
	public void onResult(Hypothesis hypothesis) {
		if (!mListening || hypothesis == null)
			return;
		String text = hypothesis.getHypstr().trim();
		Log.d(TAG, "onResult:" + text);
		mEventListener.onEvent("listen", text);
	}

	@Override
	public void onBeginningOfSpeech() {
		Log.d(TAG, "onBeginningOfSpeech");
	}

	/**
	 * We stop recognizer here to get a final result
	 */
	@Override
	public void onEndOfSpeech() {
		Log.d(TAG, "onEndOfSpeech");
		if (mListening) {
			mRecognizer.stop();
			mRecognizer.startListening("robot");
		}
	}

	private void setupRecognizer(File assetsDir, List<String> phrases) throws IOException {
		Log.i(TAG, "setupRecognizer");
		// The recognizer can be configured to perform multiple searches
		// of different kind and switch between them
		mRecognizer = SpeechRecognizerSetup.defaultSetup()
				.setAcousticModel(new File(assetsDir, "en-us-ptm"))
		        /* The dictionary is a text file with words and their phonetic pronounciation */
				.setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
//                .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
				.setKeywordThreshold(1e-45f) // Threshold to tune for keyphrase to balance between false alarms and misses
				.setBoolean("-allphone_ci", true)  // Use context-independent phonetic search, context-dependent is too slow for mobile
				.getRecognizer();

		mRecognizer.addListener(this);

        /* Build the Language Model using keyword search */
		File file = new File(mActivity.getCacheDir(), "custom.gram");
		Log.i(TAG, "writing to:" + file.getAbsolutePath());
		try {
			PrintWriter writer = new PrintWriter(file);
			for (int i = 0; i < phrases.size(); i++) {
				writer.println(phrases.get(i) + " /1e-1/");
			}
			writer.close();
			mRecognizer.addKeywordSearch("robot", file);
		} catch (Exception e) {
			Log.e(TAG, "error: " + e);
		}
		Log.i(TAG, "setup complete");
		mSetup = true;
	}

	@Override
	public void onError(Exception error) {
		Log.e(TAG, "onError:" + error.getMessage());
	}

	@Override
	public void onTimeout() {
		Log.i(TAG, "onTimeout");
	}
}
