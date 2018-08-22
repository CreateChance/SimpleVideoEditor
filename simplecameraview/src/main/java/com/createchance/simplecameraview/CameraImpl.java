package com.createchance.simplecameraview;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;
import java.util.List;

/**
 * 相机抽象类，需要不同版本的相机去实现
 * 统一接口，屏蔽底层细节。
 *
 * @author gaochao1-iri
 */

abstract class CameraImpl {

    protected final ICameraCallback mCallback;

    protected final PreviewImpl mPreview;

    protected Handler mBackgroundHandler;

    protected CameraConfig mConfig;

    protected Context mContext;

    CameraImpl(ICameraCallback cameraCallback, PreviewImpl preview,
               CameraConfig config, Context context) {
        this.mCallback = cameraCallback;
        this.mPreview = preview;
        this.mConfig = config;
        this.mContext = context;

        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
    }

    View getView() {
        return mPreview.getView();
    }

    abstract void start(CameraView.MODE mode);

    abstract void stop(CameraView.MODE mode);

    abstract int getFlashLightState();

    abstract void setFlashLightState(int state);

    abstract boolean setAspectRatio(AspectRatio ratio);

    abstract AspectRatio getAspectRatio();

    abstract void setAutoFocus(boolean autoFocus);

    abstract boolean getAutoFocus();

    abstract void triggerFocus(MotionEvent event);

    abstract void zoom(boolean isZoomIn);

    abstract int getZoom();

    abstract int getMaxZoom();

    abstract boolean isCameraOpened();

    abstract void setFacing(int cameraId);

    abstract int getFacing();

    abstract void setWhiteBalance(int value);

    abstract int getWhiteBalance();

    abstract List<Integer> getSupportedWhiteBalance();

    abstract void setExposureCompensation(int value);

    abstract int getExposureCompensation();

    abstract List<Integer> getExposureCompensationRange();

    abstract float getExposureCompensationStep();

    abstract void startTakingVideo(File videoFile);

    abstract void stopTakingVideo();

    abstract void cancelTakingVideo();

    abstract void startTakingPicture(File pictureFile);

    abstract void setDisplayOrientation(int displayOrientation);
}
