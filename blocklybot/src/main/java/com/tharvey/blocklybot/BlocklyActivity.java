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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.google.blockly.android.AbstractBlocklyActivity;
import com.google.blockly.android.codegen.CodeGenerationRequest;
import com.google.blockly.android.control.BlocklyController;
import com.google.blockly.android.ui.BlockViewFactory;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.vertical.VerticalBlockViewFactory;
import com.google.blockly.model.BlocklyParserException;
import com.google.blockly.model.BlocklySerializerException;
import com.google.blockly.model.Workspace;

import java.io.File;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Simplest implementation of AbstractBlocklyActivity.
 */
public class BlocklyActivity extends AbstractBlocklyActivity implements IConnection {
	private static final String TAG = "BlocklyActivity";
	public static final String SAVED_WORKSPACE_FILENAME_DEFAULT = "workspace.xml";
	private static final List<String> BLOCK_DEFINITIONS = Arrays.asList(new String[]{
			"default/loop_blocks.json",
			"default/logic_blocks.json",
			"default/math_blocks.json",
			"variable_blocks.json",
			"control_blocks.json",
			"robot_blocks.json",
			"speech_blocks.json",
			"audio_blocks.json"
	});
	private static final List<String> JAVASCRIPT_GENERATORS = Arrays.asList(new String[]{
			"robot_generators.js"
	});
	private String[] mVariables = {
			"apple",
			"orange",
			"bannana",
			"coconut",
			"carrot",
	};

	private ActionBar action;
	private File FILE_DIR;
	private AlertDialog alertNew, alertRename;
	private EditText editTextNew, editTextRename;
	private String workspaceName;
	static private Mobbob mRobot;
	static private JSParser mParser;
	SharedPreferences mPreferences;

	@Override
	public void onBackPressed() {
		Log.i(TAG, "onBackPressed()");
		Display display = Display.getDisplay();
		if (display != null && display.isVisible()) {
			display.hideFace();
		} else {
			super.onBackPressed();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		action = getSupportActionBar();
		FILE_DIR = getFilesDir();
		editTextNew = new EditText(this);
		editTextRename = new EditText(this);

		// Set audio stream for volume control
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

        /* Text input alertBuilder for rename/new workspace actions */
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
		alertBuilder.setView(editTextRename);
		alertBuilder.setCancelable(true);
		alertBuilder.setTitle("Rename Current Workspace");
		alertBuilder.setPositiveButton("OK", renameWorkspace);
		alertRename = alertBuilder.create();

		alertBuilder.setView(editTextNew);
		alertBuilder.setTitle("Enter New Workspace Name");
		alertBuilder.setPositiveButton("OK", newWorkspace);
		alertNew = alertBuilder.create();

		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		workspaceName = mPreferences.getString("pref_lastWorkspace", SAVED_WORKSPACE_FILENAME_DEFAULT);

		mParser = new JSParser(this);
		mRobot = Mobbob.getMobob();

        /* Autoload last workspace */
		onLoadWorkspace();
	}

	private final CodeGenerationRequest.CodeGeneratorCallback mCodeGeneratorCallback =
			new CodeGenerationRequest.CodeGeneratorCallback() {
				@Override
				public void onFinishCodeGeneration(final String generatedCode) {
					Log.i(TAG, "generatedCode:\n" + generatedCode);
					mParser.parseCode(mRobot, generatedCode, mVariables);
				}
			};

	@Override
	public void connectionStateChanged(connectionStateEnum state) {
		Log.i(TAG, "connection state changed:" + state);
		mRobot = Mobbob.getMobob();
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				updateTitlebar();
			}
		});
	}

	private void updateTitlebar() {
		if (mRobot != null && mRobot.getConnectionState() == connectionStateEnum.isConnected)
			action.setTitle(mRobot.getName() + " : " + workspaceName.replace(".xml", ""));
		else
			action.setTitle("Not connected : " + workspaceName.replace(".xml", ""));
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putString("pref_lastWorkspace", workspaceName);
		editor.commit();
	}

	@Override
	public void onLoadWorkspace() {
		try {
			loadWorkspaceFromAppDir(workspaceName);
		} catch (BlocklyParserException e) {
			// failed to parse xml
		}
		updateTitlebar();
	}

	@Override
	public void onSaveWorkspace() {
		OutputStream output = new OutputStream() {
			private StringBuilder string = new StringBuilder();
			@Override
			public void write(int b) {
				this.string.append((char) b );
			}
			public String toString() {
				return this.string.toString();
			}
		};
		Workspace workspace = mWorkspaceFragment.getWorkspace();
		try {
			workspace.serializeToXml(output);
			Log.i(TAG, "saving:" + workspaceName + ":\n" + output);
		} catch (BlocklySerializerException e) {
			Log.e(TAG, "error:" + e);
		}
		saveWorkspaceToAppDir(workspaceName);
	}

	// override restoreActionBar to set Title (setting it in OnCreate is too early)
	@Override
	protected void restoreActionBar() {
		super.restoreActionBar();
		updateTitlebar();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			final Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		} else if (id == R.id.action_about) {
			AboutDialog about = new AboutDialog(this);
			about.setTitle("About this app");
			about.show();
			return true;
		} else if (id == R.id.action_load) {
		    /* Assign workspace selector dialogue for load action */
			WorkspaceSelector dialog = new WorkspaceSelector(this, FILE_DIR, ".xml");
			dialog.addFileListener(new WorkspaceSelector.FileSelectedListener() {
				public void fileSelected(File file) {
					Log.d(TAG, "loading workspace: " + file.toString());
					workspaceName = file.getName();
					onLoadWorkspace();
				}
			});
			dialog.showDialog();
			return true;
		} else if (id == R.id.action_rename) {
			editTextRename.setText(workspaceName.replace(".xml", ""));
			alertRename.show();
			return true;
		} else if (id == R.id.action_new) {
			alertNew.show();
			return true;
		} else if (id == R.id.action_stop) {
			mParser.stop();
		} else if (id == R.id.action_connect) {
			DiscoverySelector dialog = new DiscoverySelector(this, this);
			dialog.showDialog();
			return true;
		} else if (id == R.id.action_panel) {
			final Intent intent = new Intent(this, RobotControlActivity.class);
			startActivity(intent);
		}

		return super.onOptionsItemSelected(item);
	}

	@NonNull
	@Override
	protected List<String> getBlockDefinitionsJsonPaths() {
		return BLOCK_DEFINITIONS;
	}

	@Override
	protected int getActionBarMenuResId() {
		return R.menu.blockly_actionbar;
	}

	@NonNull
	@Override
	protected String getToolboxContentsXmlPath() {
		return "toolbox_basic.xml";
	}

	@NonNull
	@Override
	protected List<String> getGeneratorsJsPaths() {
		return JAVASCRIPT_GENERATORS;
	}

	@Override
	public BlockViewFactory onCreateBlockViewFactory(WorkspaceHelper helper) {
		return new VerticalBlockViewFactory(this, helper);
	}

	@NonNull
	@Override
	protected CodeGenerationRequest.CodeGeneratorCallback getCodeGenerationCallback() {
		// Uses the same callback for every generation call.
		return mCodeGeneratorCallback;
	}

	@Override
	protected void onInitBlankWorkspace() {
		BlocklyController controller = getController();
		controller.loadWorkspaceContents(
				"<xml xmlns='http://www.w3.org/1999/xhtml'>\n" +
						"  <block type='start' id='startblock' " +
						"    x='0' y='5' " +
						"    inline='false' " +
						"    deletable='false' >" +
						"  </block>" +
						"</xml>"
		);

		// TODO: (#22) Remove this override when variables are supported properly
		for (int i = 0; i < mVariables.length; i++)
			controller.addVariable(mVariables[i]);
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "onStop()");
		super.onStop();
		if (mRobot != null)
			mRobot.disconnect();
	}

	@Override
	protected void onStart() {
		Log.d(TAG, "onStart()");
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putString("pref_defaultView", "blockly");
		editor.commit();
		super.onStart();
		if (mRobot != null)
			mRobot.connect();
	}

	private DialogInterface.OnClickListener newWorkspace = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialogInterface, int i) {
			workspaceName = editTextNew.getText().toString() + ".xml";
			updateTitlebar();

			View view = getCurrentFocus();
			if (view != null) {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
			}
		}
	};

	private DialogInterface.OnClickListener renameWorkspace = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialogInterface, int i) {
			String newName = editTextRename.getText().toString() + ".xml";
			File from = new File(FILE_DIR, workspaceName);
			File to = new File(FILE_DIR, newName);

			if (from.renameTo(to)) {
				from.delete();
				workspaceName = newName;
				updateTitlebar();
			}

			View view = getCurrentFocus();
			if (view != null) {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
			}
		}
	};

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == DiscoverySelector.REQUEST_ENABLE_BT) {
			if (resultCode == RESULT_OK) {
				DiscoverySelector dialog = new DiscoverySelector(this, this);
				dialog.showDialog();
			}
		}
	}
}
