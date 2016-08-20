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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        mRobot = Mobbob.getMobob();
        if (mRobot != null) {
            System.out.println("Blockly connected to " + mRobot.getName() + ":" + mRobot.getAddress());
        }

        // getActionBar() returns null when using blocklyTheme
//        getActionBar().setTitle(mDeviceName + ":" + mDeviceAddress);
//        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public static final String SAVED_WORKSPACE_FILENAME = "robot_workspace.xml";

    private static final List<String> BLOCK_DEFINITIONS = Arrays.asList(new String[]{
            "robot_blocks.json",
    });
    private static final List<String> JAVASCRIPT_GENERATORS = Arrays.asList(new String[]{
            "robot_generators.js"
    });


    private final int parseCode(String generatedCode) {
        if (mRobot == null)
            return 0;
        JSContext context = new JSContext();
        JSFunction Robot = new JSFunction(context,"Robot") {
            public Integer Robot(String cmd, Integer val) {
                if (val < 1)
                    val = 1;
                switch(cmd.toUpperCase()) {
                    case "MOVEFORWARD":
                        mRobot.sendCommand(Mobbob.commands.MOVE_FORWARD.ordinal(), val);
                        break;
                    case "MOVEBACKWARD":
                        mRobot.sendCommand(Mobbob.commands.MOVE_BACKWARD.ordinal(), val);
                        break;
                    case "TURNRIGHT":
                        mRobot.sendCommand(Mobbob.commands.TURN_RIGHT.ordinal(), val);
                        break;
                    case "TURNLEFT":
                        mRobot.sendCommand(Mobbob.commands.TURN_LEFT.ordinal(), val);
                        break;
                    case "SHAKEHEAD":
                        mRobot.sendCommand(Mobbob.commands.SHAKE_HEAD.ordinal(), val);
                        break;
                    case "BOUNCE":
                        mRobot.sendCommand(Mobbob.commands.BOUNCE.ordinal(), val);
                        break;
                    case "WOBBLE":
                        mRobot.sendCommand(Mobbob.commands.WOBBLE.ordinal(), val);
                        break;
                    case "WOBBLELEFT":
                        mRobot.sendCommand(Mobbob.commands.WOBBLE_LEFT.ordinal(), val);
                        break;
                    case "WOBBLERIGHT":
                        mRobot.sendCommand(Mobbob.commands.WOBBLE_RIGHT.ordinal(), val);
                        break;
                    case "TAPFEET":
                        mRobot.sendCommand(Mobbob.commands.TAP_FEET.ordinal(), val);
                        break;
                    case "TAPFOOTLEFT":
                        mRobot.sendCommand(Mobbob.commands.TAP_FOOT_LEFT.ordinal(), val);
                        break;
                    case "TAPFOOTRIGHT":
                        mRobot.sendCommand(Mobbob.commands.TAP_FOOT_RIGHT.ordinal(), val);
                        break;
                    case "SHAKELEGS":
                        mRobot.sendCommand(Mobbob.commands.SHAKE_LEGS.ordinal(), val);
                        break;
                    case "SHAKELEGLEFT":
                        mRobot.sendCommand(Mobbob.commands.SHAKE_LEG_LEFT.ordinal(), val);
                        break;
                    case "SHAKELEGRIGHT":
                        mRobot.sendCommand(Mobbob.commands.SHAKE_LEG_RIGHT.ordinal(), val);
                        break;
                    default:
                        System.err.println("Unrecognized cmd:" + cmd);
                        break;
                }
                return 0;
            }
        };
        context.property("Robot", Robot);
        context.evaluateScript(generatedCode);
        mRobot.sendCommand(Mobbob.commands.STOP.ordinal(), 0);
        return 0;
    }

    private final CodeGenerationRequest.CodeGeneratorCallback mCodeGeneratorCallback =
            new CodeGenerationRequest.CodeGeneratorCallback() {
        @Override
        public void onFinishCodeGeneration(final String generatedCode) {
            Log.i(TAG, "generatedCode:\n" + generatedCode);
            Toast.makeText(getApplicationContext(), generatedCode, Toast.LENGTH_LONG).show();
            parseCode(generatedCode);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            final Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
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
}
