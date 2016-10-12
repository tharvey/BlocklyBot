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
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import org.liquidplayer.webkit.javascriptcore.JSContext;
import org.liquidplayer.webkit.javascriptcore.JSFunction;

import java.io.BufferedReader;
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

	public JSParser(Activity activity) {
		mActivity = activity;
		mSpeak = new Speak(mActivity);
		mAudio = new Audio(mActivity);
		mDisplay = new Display(mActivity);
		mTone = new Tone();
		mListen = null;
	}

	private int doFunction(IFunction f, String p1, int p2, int p3)
	{
		if (mListen != null)
			mListen.pause();
		f.doFunction(p1, p2, p3);
		while (f.isBusy())
			SystemClock.sleep(10);
		if (mListen != null)
			mListen.resume();
		return 0;
	}

	public final int parseCode(final Mobbob mobbob, String generatedCode, String[] vars) {
		final Mobbob robot = mobbob;
		final HashMap<String, JSFunction> eventMap = new HashMap<String, JSFunction>();
		final List<String> phrases = new ArrayList<String>();

		mDisplay.showFace("default");
		if (robot == null) {
			mDisplay.showMessage("No robot connected", Toast.LENGTH_LONG);
			SystemClock.sleep(500);
		}

        /* Preparse code:
         *  - remove any root blocks that are not start blocks (TODO: get this done by blockly)
         *  - capture all Listen("<phrase>",*) calls to add phrase to keyword search list
         *  - capture all Wait("<event>",*) calls to add event to function mapping
         *  - add a footer: call start()
         */
		BufferedReader bufReader = new BufferedReader(new StringReader(generatedCode));
		String line = null;
		String newCode = "";
		for (int i = 0; i < vars.length; i++)
			newCode += "var " + vars[i] + " = 0;\n";
		newCode += "\n";
		try {
			while ((line = bufReader.readLine()) != null) {
				if (line.startsWith("function start()")
						|| line.startsWith("  ")
						|| line.startsWith("}")
						|| line.startsWith("Listen(")
						|| line.startsWith("Wait(")
						|| (line.length() == 0)
						) {
					Pattern p = Pattern.compile("Listen\\(\"([^\"]*)\",.*");
					Matcher m = p.matcher(line);
					if (m.find())
						phrases.add(m.group(1));
					newCode += line + "\n";
				}
			}
		} catch (Exception e) {
		}
		newCode += "\nstart();\n";
		Log.i(TAG, "newCode:\n" + newCode);
		final String code = newCode;

		/* create and register eventListener for various event generator s*/
		IEventListener eventListener = new IEventListener() {
			@Override
			public boolean onEvent(String type, String param) {
				final String map = type + ":" + param;
				final JSFunction func = eventMap.get(map);
				Log.i(TAG, "Event: " + map);
				if (func != null) {
					// these come in on UI thread which we must not block on, so use a thread
					Thread thread = new Thread() {
						@Override
						public void run() {
							func.call();
						}
					};
					thread.start();
					return true;
				}
				Log.e(TAG, "unhandled: " + map);
				return false;
			}
		};
		mDisplay.setListener(eventListener);
		if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
		    && phrases.size() > 0)
			mListen = new Listen(mActivity, phrases, eventListener);
		else
			mListen = null;

		Thread thread = new Thread() {
			@Override
			public void run() {
				/* wait for Listen class */
				while (mListen != null && mListen.isSetup())
					SystemClock.sleep(100);
				JSContext context = new JSContext();
				JSFunction Robot = new JSFunction(context, "Robot") {
					public Integer Robot(String str, Integer val) {
						if (val == null || val < 1)
							val = 1;
						Log.i(TAG, "robot(" + str + "," + val + ")");
						if (robot != null)
							return doFunction(robot, str, 0, val);
						else {
							mDisplay.showMessage(str, Toast.LENGTH_SHORT);
							SystemClock.sleep(1000);
						}
						return 0;
					}
				};
				context.property("Robot", Robot);

				JSFunction Speak = new JSFunction(context, "Speak") {
					public Integer Speak(String text) {
						Log.i(TAG, "speak(" + text + ")");
						int ret;
						mDisplay.setSpeaking(true);
						ret = doFunction(mSpeak, text, 0, 0);
						mDisplay.setSpeaking(false);
						return ret;
					}
				};
				context.property("Speak", Speak);

				JSFunction Audio = new JSFunction(context, "Audio") {
					public Integer Audio(String text) {
						Log.i(TAG, "audio(" + text + ")");
						return doFunction(mAudio, text, 0, 0);
					}
				};
				context.property("Audio", Audio);

				JSFunction Tone = new JSFunction(context, "Tone") {
					public Integer Tone(int freq, int secs) {
						Log.i(TAG, "tone: freq=" + freq + "secs=" + secs);
						return doFunction(mTone, null, freq, secs * 1000);
					}
				};
				context.property("Tone", Tone);

				JSFunction MusicNote = new JSFunction(context, "Note") {
					public Integer Note(String note, int timems) {
						int octave = 3; /* 3rd octave */
						Log.i(TAG, "note(" + note + ")");
						return doFunction(mTone, null, (int) Note.valueOf(note + octave), timems);
					}
				};
				context.property("Note", MusicNote);

				JSFunction Listen = new JSFunction(context, "Listen") {
					public Integer Listen(String text, JSFunction func) {
						Log.i(TAG, "listen(" + text + ")");
						eventMap.put("listen:" + text, func);
						return 0;
					}
				};
				context.property("Listen", Listen);

				JSFunction Sleep = new JSFunction(context, "Sleep") {
					public Integer Sleep(int times) {
						Log.i(TAG, "Sleep(" + times + ")");
						SystemClock.sleep(1000 * times);
						return 0;
					}
				};
				context.property("Sleep", Sleep);

				JSFunction Wait = new JSFunction(context, "Wait") {
					public Integer Wait(String event, JSFunction func) {
						Log.i(TAG, "Wait(" + event + ")");
						eventMap.put(event, func);
						return 0;
					}
				};
				context.property("Wait", Wait);

				try {
					context.evaluateScript(code);
					Log.i(TAG, "Code generation complete");
				} catch (Exception e) {
					Log.e(TAG, "Error evaluating script: " + e);
				}

				/* wait until Display is no longer showing */
				mDisplay.showMessage("Select Back to exit", Toast.LENGTH_LONG);
				while (mDisplay.isVisible())
					SystemClock.sleep(100);
				Log.i(TAG, "display no longer visible - cleanup");
				if (robot != null)
					doFunction(robot, null, Mobbob.commands.STOP.ordinal(), 0);
				mDisplay.hideFace();
				if (mListen != null)
					mListen.close();
			}
		};
		thread.start();
		return 0;
	}

	public void stop() {
		Log.i(TAG, "stop()");
		// TODO: how to stop
		if (mListen != null)
			mListen.close();
	}
}
