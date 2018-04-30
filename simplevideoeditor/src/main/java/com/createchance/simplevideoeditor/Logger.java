package com.createchance.simplevideoeditor;

import android.util.Log;

import static com.createchance.simplevideoeditor.Constants.Log.DEBUG;
import static com.createchance.simplevideoeditor.Constants.Log.ERROR;
import static com.createchance.simplevideoeditor.Constants.Log.INFO;
import static com.createchance.simplevideoeditor.Constants.Log.VERBOSE;
import static com.createchance.simplevideoeditor.Constants.Log.WARN;

/**
 * Util to print logs.
 *
 * @author gaochao1-iri
 * @date 17/04/2018
 */

public class Logger {
    static int level = VERBOSE;

    static String availableLevel() {
        return "VERBOSE, DEBUG, INFO, WARN, ERROR, NOTHING";
    }

    static void v(String tag, String msg) {
        if (level <= VERBOSE) {
            Log.v(tag, msg);
        }
    }

    static void d(String tag, String msg) {
        if (level <= DEBUG) {
            Log.d(tag, msg);
        }
    }

    static void i(String tag, String msg) {
        if (level <= INFO) {
            Log.i(tag, msg);
        }
    }

    static void w(String tag, String msg) {
        if (level <= WARN) {
            Log.w(tag, msg);
        }
    }

    static void e(String tag, String msg) {
        if (level <= ERROR) {
            Log.e(tag, msg);
        }
    }
}
