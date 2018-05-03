package com.createchance.simplevideoeditor;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 03/05/2018
 */
public class VideoEditCallback {
    private static final String TAG = VideoEditCallback.class.getSimpleName();

    public void onStart(int stage) {
        Logger.d(TAG, "Stage: " + stage + " started.");
    }

    public void onProgress(int stage, float progress) {
        Logger.d(TAG, "Stage: " + stage + " is going, progress: " + progress);
    }

    public void onSucceeded(int stage) {
        Logger.d(TAG, "Stage: " + stage + " succeeded.");
    }

    public void onFialed(int stage) {
        Logger.d(TAG, "Stage: " + stage + " failed.");
    }
}
