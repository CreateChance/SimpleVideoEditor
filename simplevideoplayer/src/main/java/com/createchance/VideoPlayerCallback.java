package com.createchance;

import android.net.Uri;

/**
 * 视频播放回调
 *
 * @author createchance
 * @date 18/09/2017
 */
public abstract class VideoPlayerCallback {

    public void onStarted(Uri uri) {
    }

    public void onPaused(Uri uri) {
    }

    public void onResumed(Uri uri) {
    }

    public void onStopped(Uri uri) {
    }

    public void onCompletion(Uri uri) {
    }

    public void onError(Exception e) {
    }

    public void onVideoProgressUpdate(int position, int duration) {
    }
}
