package com.createchance.simplevideoeditor;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 03/05/2018
 */
public class VideoEditCallback {
    private static final String TAG = VideoEditCallback.class.getSimpleName();

    public void onStart(String action) {
        Logger.d(TAG, "Action: " + action + " started.");
    }

    public void onProgress(String action, float progress) {
        Logger.d(TAG, "Action: " + action + " is going, progress: " + progress);
    }

    public void onSucceeded(String action) {
        Logger.d(TAG, "Action: " + action + " succeeded.");
    }

    public void onFailed(String action) {
        Logger.d(TAG, "Action: " + action + " failed.");
    }
}
