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
import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.blockly.android.AbstractBlocklyActivity;
import com.google.blockly.android.codegen.CodeGenerationRequest;
import com.google.blockly.android.control.BlocklyController;
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

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceName;
    private String mDeviceAddress;
    private class Robot extends Bluno {

        public Robot(Activity activity, String name, String address) {
            super(activity, name, address);
        }

        @Override
        public void onConectionStateChange(connectionStateEnum theConnectionState) {
            switch (theConnectionState) {
                case isConnected:
                    System.out.println("Connected");
                    break;
                case isConnecting:
                    System.out.println("Connecting");
                    break;
                case isToScan:
                    System.out.println("Scan");
                    break;
                case isScanning:
                    System.out.println("Scanning");
                    break;
                case isDisconnecting:
                    System.out.println("isDisconnecting");
                    break;
                default:
                    break;
            }
        }
    }

    static private Robot mRobot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        System.out.println("RobotControl Connecting to '" + mDeviceName + "':'" + mDeviceAddress + "'");

        mRobot = new Robot(this, mDeviceName, mDeviceAddress);

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
        JSContext context = new JSContext();
        JSFunction Robot = new JSFunction(context,"Robot") {
            public Integer Robot(String cmd, Integer val) {
                if (val < 1)
                    val = 1;
                switch(cmd.toUpperCase()) {
                    case "MOVEFORWARD":
                        mRobot.sendCommand(Bluno.commands.MOVE_FORWARD.ordinal(), val);
                        break;
                    case "MOVEBACKWARD":
                        mRobot.sendCommand(Bluno.commands.MOVE_BACKWARD.ordinal(), val);
                        break;
                    case "TURNRIGHT":
                        mRobot.sendCommand(Bluno.commands.TURN_RIGHT.ordinal(), val);
                        break;
                    case "TURNLEFT":
                        mRobot.sendCommand(Bluno.commands.TURN_LEFT.ordinal(), val);
                        break;
                    case "SHAKEHEAD":
                        mRobot.sendCommand(Bluno.commands.SHAKE_HEAD.ordinal(), val);
                        break;
                    case "BOUNCE":
                        mRobot.sendCommand(Bluno.commands.BOUNCE.ordinal(), val);
                        break;
                    case "WOBBLE":
                        mRobot.sendCommand(Bluno.commands.WOBBLE.ordinal(), val);
                        break;
                    case "WOBBLELEFT":
                        mRobot.sendCommand(Bluno.commands.WOBBLE_LEFT.ordinal(), val);
                        break;
                    case "WOBBLERIGHT":
                        mRobot.sendCommand(Bluno.commands.WOBBLE_RIGHT.ordinal(), val);
                        break;
                    case "TAPFEET":
                        mRobot.sendCommand(Bluno.commands.TAP_FEET.ordinal(), val);
                        break;
                    case "TAPFOOTLEFT":
                        mRobot.sendCommand(Bluno.commands.TAP_FOOT_LEFT.ordinal(), val);
                        break;
                    case "TAPFOOTRIGHT":
                        mRobot.sendCommand(Bluno.commands.TAP_FOOT_RIGHT.ordinal(), val);
                        break;
                    case "SHAKELEGS":
                        mRobot.sendCommand(Bluno.commands.SHAKE_LEGS.ordinal(), val);
                        break;
                    case "SHAKELEGLEFT":
                        mRobot.sendCommand(Bluno.commands.SHAKE_LEG_LEFT.ordinal(), val);
                        break;
                    case "SHAKELEGRIGHT":
                        mRobot.sendCommand(Bluno.commands.SHAKE_LEG_RIGHT.ordinal(), val);
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
