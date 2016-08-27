/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.tharvey.blocklybot;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.blockly.android.AbstractBlocklyActivity;
import com.google.blockly.android.codegen.CodeGenerationRequest;
import com.google.blockly.android.ui.BlockViewFactory;
import com.google.blockly.android.ui.WorkspaceHelper;
import com.google.blockly.android.ui.vertical.VerticalBlockViewFactory;

import org.liquidplayer.webkit.javascriptcore.JSContext;
import org.liquidplayer.webkit.javascriptcore.JSFunction;

import java.util.Arrays;
import java.util.List;

/**
 * Simplest implementation of AbstractBlocklyActivity.
 */
public class BlocklyActivity extends AbstractBlocklyActivity {
    private static final String TAG = "BlocklyActivity";

    static private Mobbob mRobot;
    static private JSParser mParser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        mParser = new JSParser();
        mRobot = Mobbob.getMobob();
    }

    public static final String SAVED_WORKSPACE_FILENAME = "robot_workspace.xml";

    private static final List<String> BLOCK_DEFINITIONS = Arrays.asList(new String[]{
            "robot_blocks.json",
    });
    private static final List<String> JAVASCRIPT_GENERATORS = Arrays.asList(new String[]{
            "robot_generators.js"
    });

    private final CodeGenerationRequest.CodeGeneratorCallback mCodeGeneratorCallback =
            new CodeGenerationRequest.CodeGeneratorCallback() {
        @Override
        public void onFinishCodeGeneration(final String generatedCode) {
            Log.i(TAG, "generatedCode:\n" + generatedCode);
            Toast.makeText(getApplicationContext(), generatedCode, Toast.LENGTH_LONG).show();
            mParser.parseCode(mRobot, generatedCode);
        }
    };

    @Override
    public void onLoadWorkspace() {
        loadWorkspaceFromAppDir(SAVED_WORKSPACE_FILENAME);
    }

    @Override
    public void onSaveWorkspace() {
        saveWorkspaceToAppDir(SAVED_WORKSPACE_FILENAME);
    }

    // override restoreActionBar to set Title (setting it in OnCreate is too early)
    @Override
    protected void restoreActionBar() {
        super.restoreActionBar();
        ActionBar action = getSupportActionBar();
        if (action != null) {
            if (mRobot != null)
                action.setTitle("Blockly: " + mRobot.getName());
            else
                action.setTitle("Blockly: not connected");
        }
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
        } else {
            return super.onOptionsItemSelected(item);
        }
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
        // Initialize variable names.
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        if (mRobot != null)
            mRobot.disconnect();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        if (mRobot != null)
            mRobot.connect();
    }
}
