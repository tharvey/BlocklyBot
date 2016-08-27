package com.tharvey.blocklybot;

import android.util.Log;

import org.liquidplayer.webkit.javascriptcore.JSContext;
import org.liquidplayer.webkit.javascriptcore.JSFunction;

/**
 * Created by tharvey on 8/27/16.
 */
public class JSParser {
    private final static String TAG = JSParser.class.getSimpleName();

    public final int parseCode(final Mobbob robot, String generatedCode) {
        JSContext context = new JSContext();
        JSFunction Robot = new JSFunction(context,"Robot") {
            public Integer Robot(String str, Integer val) {
                int cmd = -1;
                if (val < 1)
                    val = 1;
                switch(str.toUpperCase()) {
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
                if (cmd != -1 && robot != null)
                    robot.sendCommand(cmd, val);

                return 0;
            }
        };
        context.property("Robot", Robot);
        context.evaluateScript(generatedCode);
        robot.sendCommand(Mobbob.commands.STOP.ordinal(), 0);
        return 0;
    }

}
