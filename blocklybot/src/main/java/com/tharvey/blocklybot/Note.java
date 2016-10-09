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

import android.util.Log;

/**
 * Class to represent musical note
 * see http://www.musicdsp.org/showone.php?id=103
 */
public class Note {
	private final static String TAG = Note.class.getSimpleName();
	private static final double FACTOR = 12D / Math.log(2D);
	private static final String NOTE_SYMBOL[] = {
			"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A",
			"A#", "B"
	};
	public static float frequencyOfA4 = 440F;

	public static final float getFrequency(double d) {
		return (float) (Math.exp((d - 57D) / FACTOR) * (double) frequencyOfA4);
	}

	public static final String makeNoteSymbol(double d) {
		int i = (int) (d + 120.5D);
		StringBuffer stringbuffer = new StringBuffer(NOTE_SYMBOL[i % 12]);
		stringbuffer.append(Integer.toString(i / 12 - 10));
		return new String(stringbuffer);
	}

	public static float valueOf(String s)
			throws IllegalArgumentException {
		try {
			return getFrequency(parseNoteSymbol(s));
		} catch (IllegalArgumentException _ex) {
			throw new IllegalArgumentException("Note a valid note symbol.");
		}
	}

	private static final int parseNoteSymbol(String s)
			throws IllegalArgumentException {
		s = s.trim().toUpperCase();
		Log.i(TAG, "parseNoteSymbol:" + s);
		for (int i = NOTE_SYMBOL.length - 1; i >= 0; i--) {
			if (!s.startsWith(NOTE_SYMBOL[i]))
				continue;
			try {
				return i + 12 * Integer.parseInt(s.substring(NOTE_SYMBOL[i].length()).trim());
			} catch (NumberFormatException _ex) {
			}
			break;
		}

		throw new IllegalArgumentException("not valid note symbol.");
	}
}
