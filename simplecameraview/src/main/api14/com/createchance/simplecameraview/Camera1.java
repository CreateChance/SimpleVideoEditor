package com.createchance.simplecameraview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Camera v1 api实现类
 * api < 21平台使用
 *
 * @author gaochao1-iri
 */

@SuppressWarnings("deprecation")
final class Camera1 extends CameraImpl {

    private static final String TAG = "Camera1";

    private static final int INVALID_CAMERA_ID = -1;

    private static final SparseArray<String> FLASH_MODES = new SparseArray<>();
    private static final SparseIntArray CAMERA_IDS = new SparseIntArray();
    private static final SparseArray<String> AWB_MODES = new SparseArray();

    static {
        FLASH_MODES.put(Constants.FLASH_LIGHT_OFF, Camera.Parameters.FLASH_MODE_OFF);
        FLASH_MODES.put(Constants.FLASH_LIGHT_ON, Camera.Parameters.FLASH_MODE_ON);
        FLASH_MODES.put(Constants.FLASH_LIGHT_TORCH, Camera.Parameters.FLASH_MODE_TORCH);
        FLASH_MODES.put(Constants.FLASH_LIGHT_AUTO, Camera.Parameters.FLASH_MODE_AUTO);
        FLASH_MODES.put(Constants.FLASH_LIGHT_RED_EYE, Camera.Parameters.FLASH_MODE_RED_EYE);

        CAMERA_IDS.put(Constants.CAMERA_FACING_FRONT, Camera.CameraInfo.CAMERA_FACING_FRONT);
        CAMERA_IDS.put(Constants.CAMERA_FACING_BACK, Camera.CameraInfo.CAMERA_FACING_BACK);

        AWB_MODES.put(Constants.AWB_MODE_AUTO, Camera.Parameters.WHITE_BALANCE_AUTO);
        AWB_MODES.put(Constants.AWB_MODE_CLOUDY_DAYLIGHT, Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT);
        AWB_MODES.put(Constants.AWB_MODE_DAYLIGHT, Camera.Parameters.WHITE_BALANCE_DAYLIGHT);
        AWB_MODES.put(Constants.AWB_MODE_FLUORESCENT, Camera.Parameters.WHITE_BALANCE_FLUORESCENT);
        AWB_MODES.put(Constants.AWB_MODE_INCANDESCENT, Camera.Parameters.WHITE_BALANCE_INCANDESCENT);
        AWB_MODES.put(Constants.AWB_MODE_SHADE, Camera.Parameters.WHITE_BALANCE_SHADE);
        AWB_MODES.put(Constants.AWB_MODE_TWILIGHT, Camera.Parameters.WHITE_BALANCE_TWILIGHT);
        AWB_MODES.put(Constants.AWB_MODE_WARM_FLUORESCENT, Camera.Parameters.WHITE_BALANCE_WARM_FLUORESCENT);
    }

    private final AtomicBoolean isPictureCaptureInProgress = new AtomicBoolean(false);

    private Camera mCamera;

    private Camera.Parameters mCameraParameters;

    private final Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();

    private final SizeMap mPreviewSizes = new SizeMap();

    private final SizeMap mPictureSizes = new SizeMap();

    private final SizeMap mVideoSizes = new SizeMap();

    private Size mExpectedSize;
    private Size mActualPictureSize;
    private Size mActualVideoSize;

    private boolean mShowingPreview;

    private int mDisplayOrientation;

    private MediaRecorder mMediaRecorder;

    Camera1(ICameraCallback cameraCallback, PreviewImpl preview, CameraConfig config, Context context) {
        super(cameraCallback, preview, config, context);

        preview.setCallback(new PreviewImpl.Callback() {
            @Override
            public void onSurfaceChanged() {
                if (mCamera != null) {
                    setUpPreview();
                    adjustCameraParameters();
                }
            }
        });
    }

    @Override
    void start(CameraView.MODE mode) {
        mConfig.setMode(mode);

        initCameraDevice();

        if (!mConfig.isCameraSupported()) {
            mCallback.onError(ICameraCallback.ERROR_CODE_NO_CAMERA_DEVICES);
            return;
        }

        chooseCamera();
        openCamera();
        if (mPreview.isReady()) {
            setUpPreview();
        }
        mShowingPreview = true;
        mCamera.startPreview();
    }

    @Override
    void stop(CameraView.MODE mode) {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
        mShowingPreview = false;
        releaseCamera();
    }

    @Override
    int getFlashLightState() {
        return mConfig.getCurrentFlashlightState();
    }

    @Override
    void setFlashLightState(int state) {
        if (state == mConfig.getCurrentFlashlightState()) {
            return;
        }

        if (mConfig.getMode() == CameraView.MODE.VIDEO) {
            if (state != Constants.FLASH_LIGHT_OFF && state != Constants.FLASH_LIGHT_TORCH) {
                mCallback.onError(ICameraCallback.ERROR_CODE_OPERATION_NOT_SUPPORTED);
                return;
            }
        } else {
            if (state == Constants.FLASH_LIGHT_TORCH) {
                mCallback.onError(ICameraCallback.ERROR_CODE_OPERATION_NOT_SUPPORTED);
                return;
            }
        }

        mConfig.setCurrentFlashlightState(state);

        if (setFlashInternal(state)) {
            mCamera.setParameters(mCameraParameters);
        } else {
            mCallback.onError(ICameraCallback.ERROR_CODE_NO_FLASH_LIGTH);
        }
    }

    @Override
    boolean setAspectRatio(AspectRatio ratio) {
        if (mConfig.getAspectRatio() == null || !isCameraOpened()) {
            // Handle this later when camera is opened
            mConfig.setAspectRatio(ratio);
            return true;
        } else if (!mConfig.getAspectRatio().equals(ratio)) {
            final Set<Size> sizes = mPreviewSizes.sizes(ratio);
            if (sizes == null) {
                throw new UnsupportedOperationException(ratio + " is not supported");
            } else {
                mConfig.setAspectRatio(ratio);
                adjustCameraParameters();
                return true;
            }
        }
        return false;
    }

    @Override
    AspectRatio getAspectRatio() {
        return mConfig.getAspectRatio();
    }

    @Override
    void setAutoFocus(boolean autoFocus) {
        if (mConfig.isAutoFocus() == autoFocus) {
            return;
        }

        if (setAutoFocusInternal(autoFocus)) {
            mCamera.setParameters(mCameraParameters);
            mConfig.setAutoFocus(autoFocus);
        } else {
            mCallback.onError(ICameraCallback.ERROR_CODE_OPERATION_NOT_SUPPORTED);
        }
    }

    @Override
    boolean getAutoFocus() {
        return mConfig.isAutoFocus();
    }

    @Override
    void triggerFocus(MotionEvent event) {
        int viewWidth = mPreview.getWidth();
        int viewHeight = mPreview.getHeight();
        Rect focusRect = calculateTapArea(event.getX(), event.getY(), 3f, viewWidth, viewHeight);

        if (mCameraParameters.getMaxNumFocusAreas() > 0) {
            // 首先取消自动聚焦
            mCamera.cancelAutoFocus();
            mCameraParameters.setFocusAreas(Collections.singletonList(new Camera.Area(focusRect, 1)));
            mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
            mCamera.setParameters(mCameraParameters);
            // 当手动聚焦设置完成时，重新设置之前的设置
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    setAutoFocusInternal(mConfig.isAutoFocus());
                    mCamera.setParameters(mCameraParameters);
                }
            });
        } else {
            mCallback.onError(ICameraCallback.ERROR_CODE_OPERATION_NOT_SUPPORTED);
            return;
        }
    }

    @Override
    void zoom(boolean isZoomIn) {
        if (mCameraParameters.isZoomSupported()) {
            int maxZoom = mCameraParameters.getMaxZoom();
            int currentZoom = mCameraParameters.getZoom();
            if (isZoomIn && currentZoom < maxZoom) {
                currentZoom++;
            } else if (!isZoomIn && currentZoom > 0) {
                currentZoom--;
            }

            if (mCameraParameters.getZoom() == currentZoom) {
                return;
            }

            mCameraParameters.setZoom(currentZoom);
            mCamera.setParameters(mCameraParameters);
        } else {
            mCallback.onError(ICameraCallback.ERROR_CODE_OPERATION_NOT_SUPPORTED);
        }
    }

    @Override
    int getZoom() {
        return mCameraParameters.getZoom();
    }

    @Override
    int getMaxZoom() {
        return mCameraParameters.getMaxZoom();
    }

    @Override
    boolean isCameraOpened() {
        return mConfig.isCameraOpened();
    }

    @Override
    void setFacing(int cameraId) {
        if (mConfig.getCurrentFacing() == cameraId) {
            return;
        }
        mConfig.setCurrentFacing(cameraId);
        if (isCameraOpened()) {
            stop(mConfig.getMode());
            start(mConfig.getMode());
        }
    }

    @Override
    int getFacing() {
        return mConfig.getCurrentFacing();
    }

    @Override
    void setWhiteBalance(int value) {
        setWhiteBalanceInternal(value);

        mCamera.setParameters(mCameraParameters);
    }

    @Override
    int getWhiteBalance() {
        return AWB_MODES.keyAt(AWB_MODES.indexOfValue(mCameraParameters.getWhiteBalance().intern()));
    }

    @Override
    List<Integer> getSupportedWhiteBalance() {
        List<Integer> list = new ArrayList<>();
        List<String> modes = mCameraParameters.getSupportedWhiteBalance();

        for (String mode : modes) {
            list.add(AWB_MODES.keyAt(AWB_MODES.indexOfValue(mode.intern())));
        }

        return list;
    }

    @Override
    void setExposureCompensation(int value) {
        setExposureCompensationInternal(value);

        mCamera.setParameters(mCameraParameters);
    }

    @Override
    int getExposureCompensation() {
        return mCameraParameters.getExposureCompensation();
    }

    @Override
    List<Integer> getExposureCompensationRange() {
        List<Integer> list = new ArrayList<>();

        list.add(mCameraParameters.getMinExposureCompensation());
        list.add(mCameraParameters.getMaxExposureCompensation());

        return list;
    }

    @Override
    float getExposureCompensationStep() {
        return mCameraParameters.getExposureCompensationStep();
    }

    @Override
    void startTakingVideo(File videoFile) {
        mConfig.setResultFile(videoFile);
        if (prepareVideoRecorder()) {
            mMediaRecorder.start();
            // 回调通知
            mCallback.onVideoTakeStarted();
        } else {
            releaseMediaRecorder();
        }
    }

    @Override
    void stopTakingVideo() {
        try {
            if (mMediaRecorder != null) {
                mMediaRecorder.stop();
            }
        } catch (RuntimeException e) {
            // do nothing for now, cause media recorder may be not prepared.
        }
        releaseMediaRecorder();
        // 拍摄完成，回调通知
        mCallback.onVideoTaken(mConfig.getResultFile());
    }

    @Override
    void cancelTakingVideo() {
        stopTakingVideo();
        mConfig.getResultFile().delete();
    }

    @Override
    void startTakingPicture(File pictureFile) {
        mConfig.setResultFile(pictureFile);
        if (getAutoFocus()) {
            mCamera.cancelAutoFocus();
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    takePictureInternal();
                }
            });
        } else {
            takePictureInternal();
        }
    }

    @Override
    void setDisplayOrientation(int displayOrientation) {
        if (mDisplayOrientation == displayOrientation) {
            return;
        }
        mDisplayOrientation = displayOrientation;
        if (isCameraOpened()) {
            mCameraParameters.setRotation(calcCameraRotation(displayOrientation));
            mCamera.setParameters(mCameraParameters);
            final boolean needsToStopPreview = mShowingPreview && Build.VERSION.SDK_INT < 14;
            if (needsToStopPreview) {
                mCamera.stopPreview();
            }
            mCamera.setDisplayOrientation(calcDisplayOrientation(displayOrientation));
            if (needsToStopPreview) {
                mCamera.startPreview();
            }
        }
    }

    private void initCameraDevice() {
        int cameraNumbers = Camera.getNumberOfCameras();
        if (cameraNumbers <= 0) {
            // 没有摄像头设备
            mConfig.setCameraSupported(false);
            return;
        }

        mConfig.setCameraSupported(true);
    }

    /**
     * This rewrites {@link #mConfig} and {@link #mCameraInfo}.
     * <p>
     * TODO: 如果没有目标摄像头设备怎么办?
     */
    private void chooseCamera() {
        for (int i = 0, count = Camera.getNumberOfCameras(); i < count; i++) {
            Camera.getCameraInfo(i, mCameraInfo);
            if (mCameraInfo.facing == CAMERA_IDS.get(mConfig.getCurrentFacing())) {
                mConfig.setCurrentCameraId(CAMERA_IDS.get(mConfig.getCurrentFacing()));
                return;
            }
        }
    }

    private void openCamera() {
        if (mCamera != null) {
            releaseCamera();
        }
        mCamera = Camera.open((int) mConfig.getCurrentCameraId());
        mCameraParameters = mCamera.getParameters();
        // Supported preview sizes
        mPreviewSizes.clear();
        for (Camera.Size size : mCameraParameters.getSupportedPreviewSizes()) {
            mPreviewSizes.add(new Size(size.width, size.height));
        }
        // Supported picture sizes;
        mPictureSizes.clear();
        for (Camera.Size size : mCameraParameters.getSupportedPictureSizes()) {
            mPictureSizes.add(new Size(size.width, size.height));
        }
        // Supported video sizes
        mVideoSizes.clear();
        for (Camera.Size size : mCameraParameters.getSupportedVideoSizes()) {
            mVideoSizes.add(new Size(size.width, size.height));
            Log.d(TAG, "size: " + size.width + " x " + size.height);
        }
        // AspectRatio
        if (mConfig.getAspectRatio() == null) {
            mConfig.setAspectRatio(Constants.DEFAULT_ASPECT_RATIO);
        }
        adjustCameraParameters();
        mCamera.setDisplayOrientation(calcDisplayOrientation(mDisplayOrientation));
        mConfig.setCameraOpened(true);
        mCallback.onCameraOpened();
    }

    private AspectRatio chooseAspectRatio() {
        AspectRatio r = null;
        for (AspectRatio ratio : mPreviewSizes.ratios()) {
            r = ratio;
            if (ratio.equals(Constants.DEFAULT_ASPECT_RATIO)) {
                return ratio;
            }
        }
        return r;
    }

    private void adjustCameraParameters() {
        SortedSet<Size> sizes = mPreviewSizes.sizes(mConfig.getAspectRatio());
        if (sizes == null) { // Not supported
            mConfig.setAspectRatio(chooseAspectRatio());
            sizes = mPreviewSizes.sizes(mConfig.getAspectRatio());
        }
        Size size = chooseOptimalSize(sizes);

        // Always re-apply camera parameters
        // Largest picture size in this ratio
        final Size pictureSize = mPictureSizes.sizes(mConfig.getAspectRatio()).last();
        if (mShowingPreview) {
            mCamera.stopPreview();
        }
        mCameraParameters.setPreviewSize(size.getWidth(), size.getHeight());
        if (mConfig.getMode() == CameraView.MODE.PICTURE) {
            mCameraParameters.setPictureSize(pictureSize.getWidth(), pictureSize.getHeight());
        }
        mCameraParameters.setRotation(calcCameraRotation(mDisplayOrientation));
        setAutoFocusInternal(mConfig.isAutoFocus());
        setFlashInternal(mConfig.getCurrentFlashlightState());
        setWhiteBalanceInternal(mConfig.getCurrentAWBMode());
        setExposureCompensationInternal(mConfig.getCurrentExposureCompensation());
        mCamera.setParameters(mCameraParameters);
        if (mShowingPreview) {
            mCamera.startPreview();
        }
    }

    // Suppresses Camera#setPreviewTexture
    @SuppressLint("NewApi")
    private void setUpPreview() {
        try {
            if (mPreview.getOutputClass() == SurfaceHolder.class) {
                final boolean needsToStopPreview = mShowingPreview && Build.VERSION.SDK_INT < 14;
                if (needsToStopPreview) {
                    mCamera.stopPreview();
                }
                mCamera.setPreviewDisplay(mPreview.getSurfaceHolder());
                if (needsToStopPreview) {
                    mCamera.startPreview();
                }
            } else {
                mCamera.setPreviewTexture((SurfaceTexture) mPreview.getSurfaceTexture());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private Size chooseOptimalSize(SortedSet<Size> sizes) {
        if (!mPreview.isReady()) { // Not yet laid out
            return sizes.first(); // Return the smallest size
        }
        int desiredWidth;
        int desiredHeight;
        final int surfaceWidth = mPreview.getWidth();
        final int surfaceHeight = mPreview.getHeight();
        if (isLandscape(mDisplayOrientation)) {
            desiredWidth = surfaceHeight;
            desiredHeight = surfaceWidth;
        } else {
            desiredWidth = surfaceWidth;
            desiredHeight = surfaceHeight;
        }
        Size result = null;
        for (Size size : sizes) { // Iterate from small to large
            if (desiredWidth <= size.getWidth() && desiredHeight <= size.getHeight()) {
                return size;

            }
            result = size;
        }
        return result;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
            mConfig.setCameraOpened(false);
            mCallback.onCameraClosed();
        }
    }

    /**
     * Calculate display orientation
     * https://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation(int)
     * <p>
     * This calculation is used for orienting the preview
     * <p>
     * Note: This is not the same calculation as the camera rotation
     *
     * @param screenOrientationDegrees Screen orientation in degrees
     * @return Number of degrees required to rotate preview
     */
    private int calcDisplayOrientation(int screenOrientationDegrees) {
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (360 - (mCameraInfo.orientation + screenOrientationDegrees) % 360) % 360;
        } else {  // back-facing
            return (mCameraInfo.orientation - screenOrientationDegrees + 360) % 360;
        }
    }

    /**
     * Calculate camera rotation
     * <p>
     * This calculation is applied to the output JPEG either via Exif Orientation tag
     * or by actually transforming the bitmap. (Determined by vendor camera API implementation)
     * <p>
     * Note: This is not the same calculation as the display orientation
     *
     * @param screenOrientationDegrees Screen orientation in degrees
     * @return Number of degrees to rotate image in order for it to view correctly.
     */
    private int calcCameraRotation(int screenOrientationDegrees) {
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (mCameraInfo.orientation + screenOrientationDegrees) % 360;
        } else {  // back-facing
            final int landscapeFlip = isLandscape(screenOrientationDegrees) ? 180 : 0;
            return (mCameraInfo.orientation + screenOrientationDegrees + landscapeFlip) % 360;
        }
    }

    /**
     * Test if the supplied orientation is in landscape.
     *
     * @param orientationDegrees Orientation in degrees (0,90,180,270)
     * @return True if in landscape, false if portrait
     */
    private boolean isLandscape(int orientationDegrees) {
        return (orientationDegrees == Constants.LANDSCAPE_90 ||
                orientationDegrees == Constants.LANDSCAPE_270);
    }

    /**
     * @return {@code true} if {@link #mCameraParameters} was modified.
     */
    private boolean setAutoFocusInternal(boolean autoFocus) {
        final List<String> modes = mCameraParameters.getSupportedFocusModes();

        if (modes == null || modes.size() == 0) {
            return false;
        }

        if (mConfig.getMode() == CameraView.MODE.PICTURE) {
            if (autoFocus) {
                if (modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                } else {
                    return false;
                }
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
            } else {
                mCameraParameters.setFocusMode(modes.get(0));
            }
        } else if (mConfig.getMode() == CameraView.MODE.VIDEO) {
            if (autoFocus) {
                if (modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                } else {
                    return false;
                }
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
            } else {
                mCameraParameters.setFocusMode(modes.get(0));
            }
        }

        return true;
    }

    /**
     * @return {@code true} if {@link #mCameraParameters} was modified.
     */
    private boolean setFlashInternal(int flash) {
        List<String> modes = mCameraParameters.getSupportedFlashModes();
        String mode = FLASH_MODES.get(flash);
        if (modes == null || !modes.contains(mode)) {
            Log.d(TAG, "mode " + mode + " does not supported!");
            return false;
        }
        if (mConfig.getMode() == CameraView.MODE.PICTURE) {
            mCameraParameters.setFlashMode(mode);
            mConfig.setCurrentFlashlightState(flash);
        } else if (mConfig.getMode() == CameraView.MODE.VIDEO) {
            // 拍摄视频模式的情况下，只有手电和关闭模式支持
            if (flash == Constants.FLASH_LIGHT_TORCH || flash == Constants.FLASH_LIGHT_OFF) {
                mCameraParameters.setFlashMode(mode);
                mConfig.setCurrentFlashlightState(flash);
            }
        }

        return true;
    }

    private void setWhiteBalanceInternal(int mode) {
        mCameraParameters.setWhiteBalance(AWB_MODES.get(mode));
    }

    private void setExposureCompensationInternal(int value) {
        mCameraParameters.setExposureCompensation(value);
    }

    private void takePictureInternal() {
        if (!isPictureCaptureInProgress.getAndSet(true)) {
            mCamera.takePicture(null, null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(final byte[] data, Camera camera) {
                    isPictureCaptureInProgress.set(false);
                    mBackgroundHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            OutputStream os = null;
                            try {
                                os = new FileOutputStream(mConfig.getResultFile());
                                os.write(data);
                                os.close();
                                mCallback.onPictureTaken(mConfig.getResultFile());
                            } catch (IOException e) {
                                Log.w(TAG, "Cannot write to " + mConfig.getResultFile(), e);
                            } finally {
                                if (os != null) {
                                    try {
                                        os.close();
                                    } catch (IOException e) {
                                        // Ignore
                                    }
                                }
                            }
                        }
                    });
                    camera.cancelAutoFocus();
                    camera.startPreview();
                }
            });
        }
    }

    private boolean prepareVideoRecorder() {
        mMediaRecorder = new MediaRecorder();

        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));

        // Always re-apply camera parameters
        // Largest picture size in this ratio
        final Size videoSize = mVideoSizes.sizes(mConfig.getAspectRatio()).last();
        mMediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());

        mMediaRecorder.setOutputFile(mConfig.getResultFile().getAbsolutePath());

        mMediaRecorder.setPreviewDisplay(mPreview.getSurface());

        mMediaRecorder.setOrientationHint(calcCameraRotation(mDisplayOrientation));

        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            mCamera.lock();
        }
    }

    private Rect calculateTapArea(float x, float y, float coefficient, int width, int height) {
        float baseAreaSize = 100;

        int areaSize = Float.valueOf(baseAreaSize * coefficient).intValue();
        int centerX = (int) (x / width * 2000 - 1000);
        int centerY = (int) (y / height * 2000 - 1000);

        int halfAreaSize = areaSize / 2;
        // 点击的矩形区域
        RectF rectF = new RectF(
                clamp(centerX - halfAreaSize, -1000, 1000),
                clamp(centerY - halfAreaSize, -1000, 1000),
                clamp(centerX + halfAreaSize, -1000, 1000),
                clamp(centerY + halfAreaSize, -1000, 1000));
        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }
}
