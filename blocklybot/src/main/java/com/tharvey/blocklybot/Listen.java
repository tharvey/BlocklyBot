package com.tharvey.blocklybot;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
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

	/* Used to handle permission request */
	private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

	private SpeechRecognizer recognizer;
	private HashMap<String, Integer> captions;
	private Activity mActivity;
	private IListen mCallback;

	public Listen(Activity activity, final List<String> phrases, IListen callback) {
		mActivity = activity;
		mCallback = callback;

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
					return e;
				}
				return null;
			}

			@Override
			protected void onPostExecute(Exception result) {
				if (result != null) {
					Log.e(TAG, "Failed to init recognizer " + result);
				} else {
					Log.i(TAG, "Setup ok - initializing search");
					recognizer.startListening("robot");
				}
			}
		}.execute();
	}

	public void pause() {
		Log.i(TAG, "pause");
		if (recognizer != null) {
			recognizer.stop();
		}
	}

	public void resume() {
		Log.i(TAG, "resume");
		if (recognizer != null) {
			recognizer.startListening("robot");
		}
	}

	public void close() {
		Log.i(TAG, "close");
		if (recognizer != null) {
			recognizer.cancel();
			recognizer.shutdown();
		}
	}

	/**
	 * In partial result we get quick updates about current hypothesis. In
	 * keyword spotting mode we can react here, in other modes we need to wait
	 * for final result in onResult.
	 */
	@Override
	public void onPartialResult(Hypothesis hypothesis) {
		if (hypothesis == null)
			return;
		String text = hypothesis.getHypstr().trim();
		Log.d(TAG, "onPartialResult:" + text);
		mCallback.onResult(text);
	}

	/**
	 * This callback is called when we stop the recognizer.
	 */
	@Override
	public void onResult(Hypothesis hypothesis) {
		if (hypothesis == null)
			return;
		Log.i(TAG, "onResult:" + hypothesis.getHypstr());
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
		recognizer.stop();
		recognizer.startListening("robot");
	}

	private void setupRecognizer(File assetsDir, List<String> phrases) throws IOException {
		// The recognizer can be configured to perform multiple searches
		// of different kind and switch between them

		recognizer = SpeechRecognizerSetup.defaultSetup()
				.setAcousticModel(new File(assetsDir, "en-us-ptm"))
		        /* The dictionary is a text file with words and their phonetic pronounciation */
				.setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
//                .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
				.setKeywordThreshold(1e-45f) // Threshold to tune for keyphrase to balance between false alarms and misses
				.setBoolean("-allphone_ci", true)  // Use context-independent phonetic search, context-dependent is too slow for mobile
				.getRecognizer();

		recognizer.addListener(this);

        /* Build the Language Model using keyword search */
		File file = new File(mActivity.getCacheDir(), "custom.gram");
		Log.i(TAG, "writing to:" + file.getAbsolutePath());
		try {
			PrintWriter writer = new PrintWriter(file);
			for (int i = 0; i < phrases.size(); i++) {
				writer.println(phrases.get(i) + " /1e-1/");
			}
			writer.close();
			recognizer.addKeywordSearch("robot", file);
		} catch (Exception e) {
			Log.e(TAG, "error: " + e);
		}
		Log.i(TAG, "setup complete");
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
