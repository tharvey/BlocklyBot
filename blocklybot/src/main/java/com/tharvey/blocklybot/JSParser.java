package com.tharvey.blocklybot;

import android.app.Activity;
import android.util.Log;

import org.liquidplayer.webkit.javascriptcore.JSContext;
import org.liquidplayer.webkit.javascriptcore.JSFunction;

/**
 * Created by tharvey on 8/27/16.
 */
public class JSParser {
    private final static String TAG = JSParser.class.getSimpleName();

    private Speak mSpeak;

    public JSParser(Activity activity) {
        mSpeak = new Speak(activity);
    }

    public final int parseCode(final Mobbob mobbob, String generatedCode) {
        final Mobbob robot = mobbob;
        final String code = generatedCode;

        Thread thread = new Thread() {
            @Override
            public void run() {
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
                            default:
                                Log.e(TAG, "Unrecognized cmd:" + cmd);
                                break;
                        }
                        if (cmd != -1 && robot != null) {
                            Log.i(TAG, "robot(" + cmd + "," + val + ")");
                            robot.doCommand(cmd, val);
                        }
                        return 0;
                    }
                };
                context.property("Robot", Robot);

                JSFunction Speak = new JSFunction(context, "Speak") {
                    public Integer Speak(String text) {
                        Log.i(TAG, "speak(" + text + ")");
                        mSpeak.doCommand(text);
                        return 0;
                    }
                };
                context.property("Speak", Speak);

                context.evaluateScript(code);
                if (robot != null)
                    robot.doCommand(Mobbob.commands.STOP.ordinal(), 0);
            }
        };
        thread.start();
        return 0;
    }
}
