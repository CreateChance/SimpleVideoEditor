package com.createchance;

import android.graphics.Matrix;
import android.util.Log;
import android.view.TextureView;

/**
 * 视频播放工具类
 *
 * @author createchance
 * @date 15/09/2017
 */

class Utils {

    static boolean DEBUG = true;

    static void log(String msg) {
        if (DEBUG) {
            Log.d(SimpleVideoPlayer.class.getSimpleName(), msg);
        }
    }

    static void adjustAspectRatio(TextureView textureView, int viewWidth, int viewHeight, int videoWidth, int videoHeight) {
        final double aspectRatio = (double) videoHeight / videoWidth;
        int newWidth, newHeight;

        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            // limited by narrow width; restrict height
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            // limited by short height; restrict width
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }

        final int xoff = (viewWidth - newWidth) / 2;
        final int yoff = (viewHeight - newHeight) / 2;

        final Matrix txform = new Matrix();
        textureView.getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        txform.postTranslate(xoff, yoff);
        textureView.setTransform(txform);
    }
}
