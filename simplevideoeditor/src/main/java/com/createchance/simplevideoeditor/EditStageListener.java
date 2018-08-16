package com.createchance.simplevideoeditor;

/**
 * ${DESC}
 *
 * @author createchance
 * @date 2018/6/10
 */
public class EditStageListener {
    private static final String TAG = "EditStageListener";

    public void onStart(String action) {
//        Logger.d(TAG, "Action: " + action + " started.");
    }

    public void onProgress(String action, float progress) {
//        Logger.d(TAG, "Action: " + action + " is going, progress: " + progress);
    }

    public void onSucceeded(String action) {
//        Logger.d(TAG, "Action: " + action + " succeeded.");
    }

    public void onFailed(String action) {
//        Logger.d(TAG, "Action: " + action + " failed.");
    }
}
