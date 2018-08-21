package com.createchance;

import android.view.MotionEvent;

/**
 * 视频播放view手势监听器
 *
 * @author createchance
 * @date 18/09/2017
 */

public interface IGestureListener {
    void onClick(MotionEvent event);

    void onMoving(MotionEvent from, MotionEvent to);

    void onMoveDone(MotionEvent from, MotionEvent to);
}
