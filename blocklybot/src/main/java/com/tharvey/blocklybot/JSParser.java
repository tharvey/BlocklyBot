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

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.debug.DebugFrame;
import org.mozilla.javascript.debug.DebuggableScript;
import org.mozilla.javascript.debug.Debugger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for parsing and executing Javascript code
 */
public class JSParser {
	private final static String TAG = JSParser.class.getSimpleName();

	private Activity mActivity;
	private Listen mListen;
	private Speak mSpeak;
	private Audio mAudio;
	private Tone mTone;
	private Display mDisplay;
	private boolean mRunning;
	private Mobbob mRobot;
	private HashMap<String, String> mEventMap;
	private List<String> mEventPendingList;
	private Scriptable mScope;
	private ObservingDebugger mDebugger;
	private String[] mCodeLines;

	public JSParser(Activity activity) {
		mActivity = activity;
		mSpeak = new Speak(mActivity);
		mAudio = new Audio(mActivity);
		mDisplay = new Display(mActivity);
		mTone = new Tone();
		mListen = null;
	}

	public boolean isBusy() {
		return mRunning;
	}

	/* perform async function and block until its complete */
	private void doFunction(final IFunction f, final String p1, final int p2, final int p3) {
		if (mListen != null)
			mListen.pause();
		f.doFunction(p1, p2, p3);
		while (f.isBusy())
			SystemClock.sleep(10);
		if (mListen != null)
			mListen.resume();
	}

	/* Javascript debugger - currently this is used to check for a stop execution request
	 *     every line of code for script termination, however it could
	 *     be used in future for feedback of blockId
	 */
	class ObservingDebugger implements Debugger
	{
		boolean mStop = false;

		private DebugFrame debugFrame = null;

		/* stack frame class */
		class ObservingDebugFrame implements DebugFrame
		{
			boolean mStop = false;

			public void stop() {
				this.mStop = true;
			}

			ObservingDebugFrame(boolean state)
			{
				this.mStop = state;
			}

			public void onEnter(Context cx, Scriptable activation,
			                    Scriptable thisObj, Object[] args)
			{
				Log.i(TAG, "Debugger:enter");
			}

			public void onLineChange(Context cx, int n)
			{
				if(mStop){
					throw new RuntimeException("Script Execution terminaed");
				}
				Log.d(TAG, String.format("Debugger: % 3d: %s", n, mCodeLines[n-1]));
			}

			public void onExceptionThrown(Context cx, Throwable ex)
			{
				Log.i(TAG, "Debugger:onExceptionThrown:" + ex);
			}

			public void onExit(Context cx, boolean byThrow,
			                   Object resultOrException)
			{
				Log.i(TAG, "Debugger:exit");
			}

			@Override
			public void onDebuggerStatement(Context arg0) {
				Log.i(TAG, "Debugger:onDebuggerStatement");
			}
		}

		public void stop() {
			this.mStop = true;
			Log.i(TAG, "Debugger:stop");
			if(debugFrame != null){
				((ObservingDebugFrame)debugFrame).stop();
			}
		}

		public DebugFrame getFrame(Context cx, DebuggableScript fnOrScript)
		{
			if(debugFrame == null){
				debugFrame = new ObservingDebugFrame(mStop);
			}
			return debugFrame;
		}

		@Override
		public void handleCompilationDone(Context arg0, DebuggableScript arg1, String arg2) {
		}
	}

	public final int parseCode(final Mobbob mobbob, String generatedCode, String[] vars) {
		mRobot = mobbob;
		mListen = null;
		mEventMap = new HashMap<String, String>();
		mEventPendingList = new ArrayList<String>();
		final List<String> phrases = new ArrayList<String>();
		int i;

		mDisplay.showFace("default");
		if (mRobot == null)
			mDisplay.showMessage("No robot connected", Toast.LENGTH_LONG);

        /* Preparse code:
         *  - remove any root blocks that are not start blocks (TODO: get this done by blockly)
         *  - capture all Listen("<phrase>",*) calls to add phrase to keyword search list
         *  - capture all Wait("<event>",*) calls to add event to function mapping
         *  - add a footer: call start()
         */
		BufferedReader bufReader = new BufferedReader(new StringReader(generatedCode));
		String line;
		String newCode = "";
		// declare vars
		for (i = 0; i < vars.length; i++)
			newCode += "var " + vars[i] + " = 0;\n";
		newCode += "\n";
		try {
			while ((line = bufReader.readLine()) != null) {
				boolean skip = false;

				if (line.startsWith("BlocklyBot.Wait")) {
					Matcher m = Pattern.compile("BlocklyBot.Wait\\(\"([^\"]*)\",.*").matcher(line);
					if (m.find()) {
						String map = m.group(1);
						Log.d(TAG, "wait:" + map);
						if (map.startsWith("listen:"))
							phrases.add(map.substring(7));
					}
				}
				else if (line.contains("BlocklyBot.EventTest")) {
					Matcher m = Pattern.compile(".*BlocklyBot.EventTest\\(\"([^\"]*)\".*").matcher(line);
					if (m.find()) {
						String map = m.group(1);
						Log.d(TAG, "eventtest:" + map);
						if (map.startsWith("listen:"))
							phrases.add(map.substring(7));
					}
				}
				else if (line.startsWith("function ")) {
					Matcher m = Pattern.compile("function ([^\\)]*)\\(\\).*").matcher(line);
					if (m.find()) {
						String name = m.group(1);
						Log.d(TAG, "function:" + name);
					}
				}
				else if (line.startsWith("BlocklyBot")) {
					Log.d(TAG, "Skipping root block outside of start: " + line);
					skip = true;
				}
				if (!skip)
					newCode += line + '\n';
			}
		} catch (IOException e) {
		}

		mCodeLines = newCode.split("\\n");
		for (i = 0; i < mCodeLines.length; i++)
			Log.d(TAG, String.format("% 3d: %s", i+1, mCodeLines[i]));
		final String code = newCode;

		// create and register eventListener for various event generators
		IEventListener eventListener = new IEventListener() {
			@Override
			public boolean onEvent(String type, String param) {
				final String map = type + ":" + param;
				final String waitfunc = mEventMap.get(map);
				if (!mEventPendingList.contains(map))
					mEventPendingList.add(map);
				Log.i(TAG, "onEvent: " + map);
				if (waitfunc != null) {
					// these come in on UI thread which we must not block on, so use a thread
					Thread thread = new Thread() {
						@Override
						public void run() {
							Context context = Context.enter();
							context.setOptimizationLevel(-1);
							try {
								Function start = context.compileFunction(mScope, waitfunc, map, 1, null);
								start.call(context, mScope, context.newObject(mScope), new Object[0]);
							} catch (RhinoException e) {
								Log.e(TAG, "Exception in engine");
								Log.e(TAG, e.getMessage());
								Log.e(TAG, e.getScriptStackTrace());
							} catch (RuntimeException e) {
								Log.e(TAG, e.toString());
							} finally {
								Context.exit();
							}
						}
					};
					thread.start();
					return true;
				}
				return false;
			}
		};
		mDisplay.setListener(eventListener);

		// Start voice recognitiong engine
		if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
		    && phrases.size() > 0)
			mListen = new Listen(mActivity, phrases, eventListener);

		Thread thread = new Thread() {
			@Override
			public void run() {
				// Wait for voice recognition engine
				if (mListen != null) {
					Log.d(TAG, "Waiting for listener....");
					while (!mListen.isSetup())
						SystemClock.sleep(100);
					Log.d(TAG, "Listener ready");
				}

				// setup Rhino
				Log.i(TAG, "Configuring Rhino");
				Context context = Context.enter();
				mDebugger = new ObservingDebugger();
				context.setDebugger(mDebugger, new Integer(0));
				context.setGeneratingDebug(true);
				context.setOptimizationLevel(-1);
				mScope = context.initStandardObjects();
				ScriptableObject.putProperty(mScope, "BlocklyBot", Context.javaToJS(JSParser.this, mScope));

				mRunning = true;
				try {
					Log.d(TAG, "Evaluating code");
					context.evaluateString(mScope, code, "code", 1, null);
					Log.d(TAG, "Calling start()");
					Function jsFunction = (Function) mScope.get("start", mScope);
					Object jsResult = jsFunction.call(context, mScope, mScope, new Object[0]);
					String result = Context.toString(jsResult);
					Log.i(TAG, "result:" + result);
				} catch (RhinoException e) {
					Log.e(TAG, "Exception in engine");
					Log.e(TAG, e.getMessage());
					Log.e(TAG, e.getScriptStackTrace());
				} catch (RuntimeException e) {
					Log.i(TAG, "Script terminated");
				} finally {
					Context.exit();
				}
				mRunning = false;
				Log.i(TAG, "execution done");

				// wait display to complete (ie script terminated or complete)
				if (mDisplay.isVisible()) {
					mDisplay.showMessage("Select Back to exit", Toast.LENGTH_LONG);
					while (mDisplay.isVisible())
						SystemClock.sleep(100);
				}

				// cleanup
				Log.i(TAG, "cleanup");
				if (mRobot != null)
					doFunction(mRobot, null, Mobbob.commands.STOP.ordinal(), 0);
				if (mListen != null)
					mListen.close();
			}
		};
		thread.start();
		return 0;
	}

	static public String threadId() {
		if (Thread.currentThread() == Looper.getMainLooper().getThread())
			return new String("UI");
		else
			return new String(Thread.currentThread().toString());

	}

	public void cancel() {
		Log.i(TAG, "cancel()");
		mDisplay.hideFace();
		mDebugger.stop();
	}

	/*
	 * Scriptables
	 */
	public void Robot(String str, int val) {
		if (Thread.currentThread() == Looper.getMainLooper().getThread())
			Log.i(TAG, "UI:robot(" + str + "," + val + ")");
		else
			Log.i(TAG, Thread.currentThread() + ":robot(" + str + "," + val + ")");
		if (val < 1)
			val = 1;
		if (mRobot != null)
			doFunction(mRobot, str, 0, val);
		else {
			mDisplay.showMessage(str, Toast.LENGTH_SHORT);
			SystemClock.sleep(1000);
		}
	}

	public void Speak(String text) {
		Log.i(TAG, "speak(" + text + ")");
		mDisplay.setSpeaking(true);
		doFunction(mSpeak, text, 0, 0);
		mDisplay.setSpeaking(false);
	}

	public void Audio(String text) {
		Log.i(TAG, "audio(" + text + ")");
		doFunction(mAudio, text, 0, 0);
	}

	public void Tone(int freq, int secs) {
		Log.i(TAG, "tone: freq=" + freq + "secs=" + secs);
		doFunction(mTone, null, freq, secs * 1000);
	}

	public void Note(String note, int timems) {
		int octave = 3; /* 3rd octave */
		Log.i(TAG, "note(" + note + ")");
		doFunction(mTone, null, (int) Note.valueOf(note + octave), timems);
	}

	public void Sleep(int times) {
		Log.i(TAG, "Sleep(" + times + ")");
		SystemClock.sleep(1000 * times);
	}

	public void Wait(String event, String func) {
		Log.i(TAG, "Wait(" + event + ")");
		mEventMap.put(event, func);
	}

	public boolean EventTest(String map) {
		Log.i(TAG, "EventTest(" + map + ")");
		if (mEventPendingList.contains(map)) {
			Log.i(TAG, "Event asserted");
			mEventPendingList.remove(map); // read-to-clear
			return true;
		}
		return false;
	}
}
