package com.createchance.simplevideoeditor;

import java.io.File;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 03/05/2018
 */
public class EditListener {
    private static final String TAG = EditListener.class.getSimpleName();

    public void onStart(long token) {
        Logger.d(TAG, "Editor: " + token + " started.");
    }

    public void onProgress(long token, float progress) {
        Logger.d(TAG, "Editor: " + token + " is going, progress: " + progress);
    }

    public void onSucceeded(long token, File outputFile) {
        Logger.d(TAG, "Editor: " + token + " succeeded.");
    }

    public void onFailed(long token) {
        Logger.d(TAG, "Editor: " + token + " failed.");
    }
}
