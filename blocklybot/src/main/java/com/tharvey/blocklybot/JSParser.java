package com.tharvey.blocklybot;

import android.app.Activity;
import android.os.SystemClock;
import android.util.Log;

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
 * Created by tharvey on 8/27/16.
 */
public class JSParser {
	private final static String TAG = JSParser.class.getSimpleName();

	private Activity mActivity;
	private Listen mListen;

	public JSParser(Activity activity) {
		mActivity = activity;
	}

	public final int parseCode(final Mobbob mobbob, String generatedCode) {
		final Mobbob robot = mobbob;
		final Speak speak = new Speak(mActivity);
		final Audio audio = new Audio(mActivity);
		final Tone tone = new Tone();
		final HashMap<String, JSFunction> listenMap = new HashMap<String, JSFunction>();
		final List<String> phrases = new ArrayList<String>();
		final Display display = new Display(mActivity);

		display.showFace("default");

        /* Preparse code:
         *  - remove any root blocks that are not start blocks (TODO: get this done by blockly)
         *  - capture all Listen("<phrase>",*) calls to add phrase to keyword search list
         *  - add a footer: call start()
         */
		BufferedReader bufReader = new BufferedReader(new StringReader(generatedCode));
		String line = null;
		String newCode = "";
		try {
			while ((line = bufReader.readLine()) != null) {
				if (line.startsWith("function start()")
						|| line.startsWith("  ")
						|| line.startsWith("}")
						|| line.startsWith("Listen(")
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

		mListen = new Listen(mActivity, phrases, new IListen() {
			@Override
			public void onResult(String text) {
				JSFunction func = listenMap.get(text);
				Log.i(TAG, "Listen result for '" + text + "'");
				if (func != null) {
					Log.i(TAG, "Calling JSFunction");
					func.call();
				}
			}
		});

		Thread thread = new Thread() {
			@Override
			public void run() {
				/* wait for Listen class */
				while (mListen != null && mListen.isSetup())
					SystemClock.sleep(100);
				JSContext context = new JSContext();
				JSFunction Robot = new JSFunction(context, "Robot") {
					public Integer Robot(String str, Integer val) {
						int cmd = -1;
						if (val < 1)
							val = 1;
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
						Log.i(TAG, "robot(" + cmd + "," + val + ")");
						if (cmd != -1 && robot != null) {
							robot.doCommand(cmd, val);
						}
						return 0;
					}
				};
				context.property("Robot", Robot);

				JSFunction Speak = new JSFunction(context, "Speak") {
					public Integer Speak(String text) {
						Log.i(TAG, "speak(" + text + ")");
						mListen.pause();
						display.setSpeaking(true);
						speak.doCommand(text);
						display.setSpeaking(false);
						mListen.resume();
						return 0;
					}
				};
				context.property("Speak", Speak);

				JSFunction Audio = new JSFunction(context, "Audio") {
					public Integer Audio(String text) {
						Log.i(TAG, "audio(" + text + ")");
						mListen.pause();
						audio.doCommand(text);
						mListen.resume();
						return 0;
					}
				};
				context.property("Audio", Audio);

				JSFunction Tone = new JSFunction(context, "Tone") {
					public Integer Tone(int freq, int secs) {
						Log.i(TAG, "tone: freq=" + freq + "secs=" + secs);
						mListen.pause();
						tone.Tone(freq, secs * 1000);
						mListen.resume();
						return 0;
					}
				};
				context.property("Tone", Tone);

				JSFunction MusicNote = new JSFunction(context, "Note") {
					public Integer Note(String note, int timems) {
						int octave = 3; /* 3rd octave */
						Log.i(TAG, "note(" + note + ")");
						mListen.pause();
						tone.Tone(Note.valueOf(note + octave), timems);
						mListen.resume();
						return 0;
					}
				};
				context.property("Note", MusicNote);

				JSFunction Listen = new JSFunction(context, "Listen") {
					public Integer Listen(String text, JSFunction func) {
						Log.i(TAG, "listen(" + text + ")");
						listenMap.put(text, func);
						return 0;
					}
				};
				context.property("Listen", Listen);

				JSFunction Wait = new JSFunction(context, "Wait") {
					public Integer Wait(int times) {
						Log.i(TAG, "Wait(" + times + ")");
						SystemClock.sleep(1000 * times);
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
				if (robot != null)
					robot.doCommand(Mobbob.commands.STOP.ordinal(), 0);
				display.hideFace();
			}
		};
		thread.start();
		return 0;
	}

	public void stop() {
		Log.i(TAG, "stop()");
		// TODO: how to stop script?
		if (mListen != null)
			mListen.close();
	}
}
