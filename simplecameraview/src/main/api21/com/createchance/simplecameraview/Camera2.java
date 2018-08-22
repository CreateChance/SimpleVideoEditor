package com.createchance.simplecameraview;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

/**
 * Camera v2 实现类
 * <p>
 * API >= 21平台使用
 */
@SuppressWarnings("MissingPermission")
@TargetApi(21)
final class Camera2 extends CameraImpl {

    private static final String TAG = "Camera2";

    /**
     * Camera2 API能够保证的最大预览宽度
     */
    private final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Camera2 API能够保证的最大预览高度
     */
    private final int MAX_PREVIEW_HEIGHT = 1080;

    private static final SparseIntArray AWB_MODES = new SparseIntArray();

    static {
        AWB_MODES.put(Constants.AWB_MODE_OFF, CameraCharacteristics.CONTROL_AWB_MODE_OFF);
        AWB_MODES.put(Constants.AWB_MODE_CLOUDY_DAYLIGHT, CameraCharacteristics.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT);
        AWB_MODES.put(Constants.AWB_MODE_DAYLIGHT, CameraCharacteristics.CONTROL_AWB_MODE_DAYLIGHT);
        AWB_MODES.put(Constants.AWB_MODE_FLUORESCENT, CameraCharacteristics.CONTROL_AWB_MODE_FLUORESCENT);
        AWB_MODES.put(Constants.AWB_MODE_INCANDESCENT, CameraCharacteristics.CONTROL_AWB_MODE_INCANDESCENT);
        AWB_MODES.put(Constants.AWB_MODE_SHADE, CameraCharacteristics.CONTROL_AWB_MODE_SHADE);
        AWB_MODES.put(Constants.AWB_MODE_TWILIGHT, CameraCharacteristics.CONTROL_AWB_MODE_TWILIGHT);
        AWB_MODES.put(Constants.AWB_MODE_WARM_FLUORESCENT, CameraCharacteristics.CONTROL_AWB_MODE_WARM_FLUORESCENT);
        AWB_MODES.put(Constants.AWB_MODE_AUTO, CameraCharacteristics.CONTROL_AWB_MODE_AUTO);
    }

    private final Camera2DeviceInfo mDeviceInfo = new Camera2DeviceInfo();

    private final CameraManager mCameraManager;
    private CameraDevice mCamera;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraCharacteristics mCameraCharacteristics;

    private MediaRecorder mMediaRecorder;
    private ImageReader mImageReader;

    private final SizeMap mPreviewSizes = new SizeMap();
    private final SizeMap mPictureSizes = new SizeMap();
    private AspectRatio mAspectRatio = Constants.DEFAULT_ASPECT_RATIO;

    private int mDisplayOrientation;

    private float currentZoomLevel = 1;

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            try (Image image = reader.acquireNextImage()) {
                Image.Plane[] planes = image.getPlanes();
                if (planes.length > 0) {
                    ByteBuffer buffer = planes[0].getBuffer();
                    final byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
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
                                mCallback.onError(ICameraCallback.ERROR_CODE_SAVED_FAILED);
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
                }
            }
        }

    };

    private final CameraDevice.StateCallback mCameraDeviceCallback
            = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCamera = camera;
            mConfig.setCameraOpened(true);
            mCallback.onCameraOpened();
            startPreviewSession();
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            mConfig.setCameraOpened(false);
            mCallback.onCameraClosed();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCamera = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "onError: " + camera.getId() + " (" + error + ")");
            mCamera = null;
        }

    };

    PictureCaptureCallback mCaptureCallback = new PictureCaptureCallback() {

        @Override
        public void onPrecaptureRequired() {
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            setState(STATE_PRECAPTURE);
            try {
                mCaptureSession.capture(mCaptureRequestBuilder.build(), this, null);
                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
            } catch (CameraAccessException e) {
                Log.e(TAG, "Failed to run precapture sequence.", e);
            }
        }

        @Override
        public void onReady() {
            captureStillPicture();
        }

    };

    Camera2(ICameraCallback cameraCallback, PreviewImpl preview, CameraConfig config, Context context) {
        super(cameraCallback, preview, config, context);
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        initCameraDevices();
        mPreview.setCallback(new PreviewImpl.Callback() {
            @Override
            public void onSurfaceChanged() {
                startPreviewSession();
            }
        });
    }

    @Override
    void start(CameraView.MODE mode) {
        if (mDeviceInfo.getDeviceByFacing(Constants.CAMERA_FACING_FRONT) == null &&
                mDeviceInfo.getDeviceByFacing(Constants.CAMERA_FACING_BACK) == null) {
            mCallback.onError(ICameraCallback.ERROR_CODE_OPERATION_NOT_SUPPORTED);
            return;
        }

        mConfig.setMode(mode);
        collectCameraInfo();

        if (mode == CameraView.MODE.PICTURE) {
            prepareImageReader();
        }

        startOpeningCamera();
    }

    @Override
    void stop(CameraView.MODE mode) {
        if (mCaptureSession != null) {
            stopCaptureSession();
            mCaptureSession = null;
        }

        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }

        if (mode == CameraView.MODE.PICTURE) {
            if (mImageReader != null) {
                mImageReader.close();
                mImageReader = null;
            }
        } else if (mode == CameraView.MODE.VIDEO) {
            if (mMediaRecorder != null) {
                releaseVideoRecorder();
            }
        }
    }

    @Override
    int getFlashLightState() {
        return mConfig.getCurrentFlashlightState();
    }

    @Override
    void setFlashLightState(int state) {
        if (state == mConfig.getCurrentFlashlightState()) {
            // 状态没有变化，无需设置
            return;
        }

        // 如果闪光灯不可用，回调通知
        if (mConfig.getCurrentFlashlightState() == Constants.FLASH_LIGHT_NOT_AVALIABLE) {
            mCallback.onError(ICameraCallback.ERROR_CODE_NO_FLASH_LIGTH);
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

        int saved = mConfig.getCurrentFlashlightState();
        mConfig.setCurrentFlashlightState(state);
        if (mCaptureRequestBuilder != null) {
            updateFlashLightState();
            if (mCaptureSession != null) {
                try {
                    mCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, null);
                } catch (CameraAccessException e) {
                    //e.printStackTrace();
                    // 因为出现了异常，所以要恢复之前的状态
                    mConfig.setCurrentFlashlightState(saved);
                }
            }
        }
    }

    @Override
    boolean isCameraOpened() {
        if (mConfig.getCurrentCameraId() == null) {
            mCallback.onError(ICameraCallback.ERROR_CODE_OPERATION_NOT_SUPPORTED);
            return false;
        }

        return mConfig.isCameraOpened();
    }

    @Override
    void setFacing(int facing) {
        if (facing == Constants.CAMERA_FACING_FRONT &&
                mDeviceInfo.getDeviceByFacing(Constants.CAMERA_FACING_FRONT) == null) {
            mCallback.onError(ICameraCallback.ERROR_CODE_OPERATION_NOT_SUPPORTED);
            return;
        }

        if (facing == Constants.CAMERA_FACING_BACK &&
                mDeviceInfo.getDeviceByFacing(Constants.CAMERA_FACING_BACK) == null) {
            mCallback.onError(ICameraCallback.ERROR_CODE_OPERATION_NOT_SUPPORTED);
            return;
        }

        if (mConfig.getCurrentFacing() == facing) {
            return;
        }
        mConfig.setCurrentFacing(facing);
        collectCameraInfo();
        if (isCameraOpened()) {
            // restart here
            stop(mConfig.getMode());
        }
        mConfig.setCurrentCameraId(mDeviceInfo.getDeviceByFacing(facing).getCameraId());
        start(mConfig.getMode());
    }

    @Override
    int getFacing() {
        return mConfig.getCurrentFacing();
    }

    @Override
    void setWhiteBalance(int value) {
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, AWB_MODES.get(value));

        if (mCaptureSession != null) {
            try {
                mCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, null);
            } catch (CameraAccessException e) {
                //e.printStackTrace();
            }
        }
    }

    @Override
    int getWhiteBalance() {
        return mConfig.getCurrentAWBMode();
    }

    @Override
    List<Integer> getSupportedWhiteBalance() {
        List<Integer> list = new ArrayList<>();
        int[] modes = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);

        if (modes != null && modes.length > 0) {
            for (int mode : modes) {
                list.add(AWB_MODES.keyAt(AWB_MODES.indexOfValue(mode)));
            }
        }

        return list;
    }

    @Override
    void setExposureCompensation(int value) {
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, value);
        if (mCaptureSession != null) {
            try {
                mCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, null);
                mConfig.setCurrentExposureCompensation(value);
            } catch (CameraAccessException e) {
                //e.printStackTrace();
            }
        }
    }

    @Override
    int getExposureCompensation() {
        return mConfig.getCurrentExposureCompensation();
    }

    @Override
    List<Integer> getExposureCompensationRange() {
        List<Integer> list = new ArrayList<>();

        list.add(mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE).getLower());
        list.add(mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE).getUpper());

        return list;
    }

    @Override
    float getExposureCompensationStep() {
        return mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP).floatValue();
    }

    @Override
    void startTakingVideo(File videoFile) {
        mConfig.setResultFile(videoFile);

        prepareMediaRecorder();
        startVideoRecording();
    }

    @Override
    void stopTakingVideo() {
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mMediaRecorder != null) {
                        mMediaRecorder.stop();
                        releaseVideoRecorder();
                        // 拍摄完成，回调通知
                        mCallback.onVideoTaken(mConfig.getResultFile());
                    }
                } catch (RuntimeException e) {
                    // do nothing for now, cause media recorder may be not prepared.
                }

                if (!mConfig.isNeedToStop()) {
                    // 当拍摄完成的时候，重新开始预页面
                    startPreviewSession();
                }
            }
        });
    }

    @Override
    void cancelTakingVideo() {
        stopTakingVideo();
        File file = mConfig.getResultFile();
        if (file != null) {
            file.delete();
        }
        // 取消拍摄，回调通知
        mCallback.onVideoTakeCanceled();
    }

    @Override
    void startTakingPicture(File pictureFile) {
        mConfig.setResultFile(pictureFile);

        if (mConfig.isAutoFocus()) {
            lockFocus();
        } else {
            captureStillPicture();
        }
    }

    @Override
    void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
        mPreview.setDisplayOrientation(mDisplayOrientation);
    }

    @Override
    boolean setAspectRatio(AspectRatio ratio) {
        if (ratio == null || ratio.equals(mAspectRatio) ||
                !mPreviewSizes.ratios().contains(ratio)) {
            // TODO: Better error handling
            return false;
        }

        mAspectRatio = ratio;

        if (mCaptureSession != null) {
            stopCaptureSession();
            startPreviewSession();
        }

        return true;
    }

    @Override
    AspectRatio getAspectRatio() {
        return mAspectRatio;
    }

    @Override
    void setAutoFocus(boolean autoFocus) {
        if (!mDeviceInfo.getDeviceById((String) mConfig.getCurrentCameraId()).isAFSupported()) {
            // 因为本地系统不支持auto focus，所以我们只能回调通知
            mCallback.onError(ICameraCallback.ERROR_CODE_OPERATION_NOT_SUPPORTED);
            return;
        }

        if (autoFocus == mConfig.isAutoFocus()) {
            return;
        }

        mConfig.setAutoFocus(autoFocus);

        if (mCaptureRequestBuilder != null) {
            updateAutoFocus();
            if (mCaptureSession != null) {
                try {
                    mCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, null);
                } catch (CameraAccessException e) {
                    // 此时我们回退到初始状态
                    mConfig.setAutoFocus(!autoFocus);
                }
            }
        }
    }

    @Override
    boolean getAutoFocus() {
        return mConfig.isAutoFocus();
    }

    @Override
    void triggerFocus(MotionEvent event) {
        if (mCameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) == 0) {
            mCallback.onError(ICameraCallback.ERROR_CODE_OPERATION_NOT_SUPPORTED);
            return;
        }

        int viewWidth = mPreview.getWidth();
        int viewHeight = mPreview.getHeight();
        Rect focusRect = calculateTapArea(event.getX(), event.getY(), 3f, viewWidth, viewHeight);
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_MACRO);
        MeteringRectangle[] meteringRectangles = new MeteringRectangle[1];
        meteringRectangles[0] = new MeteringRectangle(focusRect, 1);
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, meteringRectangles);
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);

        if (mCaptureSession != null) {
            try {
                mCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                        // 聚焦完成之后，回到之前的设置状态
                        updateAutoFocus();
                        try {
                            mCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, null);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    void zoom(boolean isZoomIn) {
        float maxZoom = mCameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) * 10;
        if (maxZoom == 0) {
            mCallback.onError(ICameraCallback.ERROR_CODE_OPERATION_NOT_SUPPORTED);
            return;
        }

        Rect rect = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

        float old = currentZoomLevel;
        if (isZoomIn && currentZoomLevel < maxZoom) {
            currentZoomLevel += 0.5;
        } else if (!isZoomIn && currentZoomLevel > 1) {
            currentZoomLevel -= 0.5;
        }
        if (old == currentZoomLevel) {
            // when max and min, no change
            return;
        }
        int minW = (int) (rect.width() / maxZoom);
        int minH = (int) (rect.height() / maxZoom);
        int difW = rect.width() - minW;
        int difH = rect.height() - minH;
        int cropW = difW / 100 * (int) currentZoomLevel;
        int cropH = difH / 100 * (int) currentZoomLevel;
        cropW -= cropW & 3;
        cropH -= cropH & 3;
        Rect zoom = new Rect(cropW, cropH, rect.width() - cropW, rect.height() - cropH);
        mCaptureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);

        if (mCaptureSession != null) {
            try {
                mCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    int getZoom() {
        return (int) currentZoomLevel;
    }

    @Override
    int getMaxZoom() {
        return mCameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM).intValue();
    }

    /**
     * Locks the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            mCaptureCallback.setState(PictureCaptureCallback.STATE_LOCKING);
            mCaptureSession.capture(mCaptureRequestBuilder.build(), mCaptureCallback, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to lock focus.", e);
        }
    }

    /**
     * Unlocks the auto-focus and restart camera preview. This is supposed to be called after
     * capturing a still picture.
     */
    void unlockFocus() {
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        try {
            mCaptureSession.capture(mCaptureRequestBuilder.build(), mCaptureCallback, null);
            updateAutoFocus();
            updateFlashLightState();
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
            mCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), mCaptureCallback,
                    null);
            mCaptureCallback.setState(PictureCaptureCallback.STATE_PREVIEW);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to restart camera preview.", e);
        }
    }

    /**
     * Captures a still picture.
     */
    void captureStillPicture() {
        try {
            CaptureRequest.Builder captureRequestBuilder = mCamera.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(mImageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    mCaptureRequestBuilder.get(CaptureRequest.CONTROL_AF_MODE));
            updateFlashLightState();
            // Calculate JPEG orientation.
            @SuppressWarnings("ConstantConditions")
            int sensorOrientation = mCameraCharacteristics.get(
                    CameraCharacteristics.SENSOR_ORIENTATION);
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                    (sensorOrientation +
                            mDisplayOrientation * (mConfig.getCurrentFacing() == Constants.CAMERA_FACING_FRONT ? 1 : -1) +
                            360) % 360);
            // Stop preview and capture a still picture.
            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureRequestBuilder.build(),
                    new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                       @NonNull CaptureRequest request,
                                                       @NonNull TotalCaptureResult result) {
                            unlockFocus();
                        }
                    }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Cannot capture a still picture.", e);
        }
    }

    // 根据当前状态设置闪光灯
    private void updateFlashLightState() {
        switch (mConfig.getCurrentFlashlightState()) {
            case Constants.FLASH_LIGHT_OFF:
                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON);
                mCaptureRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
            case Constants.FLASH_LIGHT_ON:
                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                break;
            case Constants.FLASH_LIGHT_TORCH:
                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON);
                mCaptureRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_TORCH);
                break;
            case Constants.FLASH_LIGHT_AUTO:
                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                break;
            case Constants.FLASH_LIGHT_RED_EYE:
                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE);
                break;
        }

    }

    private void updateWhiteBalanceState() {
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, mConfig.getCurrentAWBMode());
    }

    private void updateExposureCompensation() {
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, mConfig.getCurrentExposureCompensation());
    }

    private void collectCameraInfo() {
        try {
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics((String) mConfig.getCurrentCameraId());

            StreamConfigurationMap map = mCameraCharacteristics.
                    get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            if (map == null) {
                // TODO: handle error here!!!
                throw new IllegalStateException("Failed to get configuration map: " + mConfig.getCurrentCameraId());
            }

            mPreviewSizes.clear();
            for (android.util.Size size : map.getOutputSizes(mPreview.getOutputClass())) {
                int width = size.getWidth();
                int height = size.getHeight();
                if (width <= MAX_PREVIEW_WIDTH && height <= MAX_PREVIEW_HEIGHT) {
                    mPreviewSizes.add(new Size(width, height));
                }
            }

            mPictureSizes.clear();
            collectPictureSizes(mPictureSizes, map);
//            for (AspectRatio ratio : mPreviewSizes.ratios()) {
//                if (!mPictureSizes.ratios().contains(ratio)) {
//                    mPreviewSizes.remove(ratio);
//                }
//            }

            if (!mPreviewSizes.ratios().contains(mAspectRatio)) {
                mAspectRatio = mPreviewSizes.ratios().iterator().next();
            }

            // 检查当前相机是否支持auto focus
            int[] afmodes = mCameraCharacteristics.get(
                    CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            // Auto focus不支持
            if (afmodes == null || afmodes.length == 0 ||
                    (afmodes.length == 1 && afmodes[0] == CameraCharacteristics.CONTROL_AF_MODE_OFF)) {
                mDeviceInfo.getDeviceById((String) mConfig.getCurrentCameraId()).setAFSupported(false);
            } else {
                mDeviceInfo.getDeviceById((String) mConfig.getCurrentCameraId()).setAFSupported(true);
            }

            // 检查当前相机是否支持自动白平衡
            int[] awbmodes = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
            if (awbmodes == null || awbmodes.length == 0 ||
                    (awbmodes.length == 1 && awbmodes[0] == CameraCharacteristics.CONTROL_AWB_MODE_OFF)) {
                mDeviceInfo.getDeviceById((String) mConfig.getCurrentCameraId()).setAWBSupported(false);
            } else {
                mDeviceInfo.getDeviceById((String) mConfig.getCurrentCameraId()).setAWBSupported(true);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            // TODO: handle error here!!!
        }
    }

    // 初始化摄像头设备，获取设备信息，包括摄像头设备的数量，特性等等信息
    void initCameraDevices() {
        try {
            String[] ids = mCameraManager.getCameraIdList();
            if (ids.length == 0) {
                // 这表示这台机器上没有任何摄像头设备
                mConfig.setCameraSupported(false);
                mCallback.onError(ICameraCallback.ERROR_CODE_NO_CAMERA_DEVICES);
                return;
            }
            for (String id : ids) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);
                Integer internal = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (internal == null) {
                    mCallback.onError(ICameraCallback.ERROR_CODE_UNKNOWN);
                    continue;
                }
                // 目前仅支持前置和后置摄像头，其他外接摄像头暂不支持
                if (internal == CameraCharacteristics.LENS_FACING_FRONT) {
                    mDeviceInfo.addInfo(new Camera2DeviceInfo.Device(id, Constants.CAMERA_FACING_FRONT));
                } else if (internal == CameraCharacteristics.LENS_FACING_BACK) {
                    mDeviceInfo.addInfo(new Camera2DeviceInfo.Device(id, Constants.CAMERA_FACING_BACK));
                    // 目前仅考虑后置摄像头上的闪光灯
                    if (characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
                        // 在闪光灯可用的情况下，默认关闭闪光灯
                        mConfig.setCurrentFlashlightState(Constants.FLASH_LIGHT_OFF);
                    }
                }
            }

            // 优先使用后置摄像头
            if (mDeviceInfo.getDeviceByFacing(Constants.CAMERA_FACING_BACK) != null) {
                mConfig.setCurrentFacing(Constants.CAMERA_FACING_BACK);
            } else if (mDeviceInfo.getDeviceByFacing(Constants.CAMERA_FACING_FRONT) != null) {
                mConfig.setCurrentFacing(Constants.CAMERA_FACING_FRONT);
            }

            mConfig.setCurrentCameraId(mDeviceInfo.getDeviceByFacing(mConfig.getCurrentFacing()).getCameraId());
        } catch (CameraAccessException e) {
            //e.printStackTrace();
            // 获取摄像头设备列表失败，一般都是没有权限
            mCallback.onError(ICameraCallback.ERROR_CODE_NO_PERMISSION);
        }
    }

    private void updateAutoFocus() {
        if (mConfig.isAutoFocus()) {
            CameraView.MODE mode = mConfig.getMode();
            if (mode == CameraView.MODE.PICTURE) {
                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            } else if (mode == CameraView.MODE.VIDEO) {
                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            }
        } else {
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_MACRO);
        }
    }

    private void startVideoRecording() {
        if (!mConfig.isCameraOpened() || !mPreview.isReady() || mMediaRecorder == null) {
            return;
        }

        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Size previewSize = chooseOptimalSize();
                    mPreview.setBufferSize(previewSize.getWidth(), previewSize.getHeight());

                    mCaptureRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                    final List<Surface> surfaces = new ArrayList<>();
                    surfaces.add(mPreview.getSurface());
                    mCaptureRequestBuilder.addTarget(mPreview.getSurface());
                    surfaces.add(mMediaRecorder.getSurface());
                    mCaptureRequestBuilder.addTarget(mMediaRecorder.getSurface());

                    Log.d(TAG, "Starting taking video.");
                    mCamera.createCaptureSession(getSurfaceListByMode(), new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            try {
                                mCaptureSession = session;

                                updateAutoFocus();
                                updateFlashLightState();
                                updateWhiteBalanceState();
                                updateExposureCompensation();
                                mCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
                                mMediaRecorder.start();
                                // 回调通知
                                mCallback.onVideoTakeStarted();
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                                // TODO: handle error here!!!
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                        }
                    }, mBackgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private void startPreviewSession() {
        if (!isCameraOpened() || !mPreview.isReady()) {
            return;
        }
        Size previewSize = chooseOptimalSize();
        mPreview.setBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface surface = mPreview.getSurface();
        try {
            if (mCamera == null) {
                return;
            }
            mCaptureRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(surface);
            mCamera.createCaptureSession(getSurfaceListByMode(),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            if (mCamera == null) {
                                return;
                            }
                            mCaptureSession = session;
                            updateAutoFocus();
                            updateFlashLightState();
                            updateWhiteBalanceState();
                            updateExposureCompensation();
                            try {
                                mCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(),
                                        null, null);
                            } catch (CameraAccessException e) {
                                Log.e(TAG, "Failed to start camera preview because it couldn't access camera", e);
                            } catch (IllegalStateException e) {
                                Log.e(TAG, "Failed to start camera preview.", e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                        }

                        @Override
                        public void onClosed(@NonNull CameraCaptureSession session) {
                            if (mCaptureSession != null && mCaptureSession.equals(session)) {
                                mCaptureSession = null;
                            }
                        }
                    }, null);
        } catch (CameraAccessException e) {
            // Failed to start camera session
        } catch (IllegalStateException e) {
            // Maybe camera is already close.
        }
    }

    private void stopCaptureSession() {
        if (mCaptureSession != null) {
            try {
                mCaptureSession.abortCaptures();
            } catch (CameraAccessException e) {
                // camera access error
            } catch (IllegalStateException e) {
                // camera device is already closed.
            } finally {
                mCaptureSession.close();
                mCaptureSession = null;
            }
        }
    }

    private Size chooseOptimalSize() {
        int surfaceLonger, surfaceShorter;
        final int surfaceWidth = mPreview.getWidth();
        final int surfaceHeight = mPreview.getHeight();
        if (surfaceWidth < surfaceHeight) {
            surfaceLonger = surfaceHeight;
            surfaceShorter = surfaceWidth;
        } else {
            surfaceLonger = surfaceWidth;
            surfaceShorter = surfaceHeight;
        }
        SortedSet<Size> candidates = mPreviewSizes.sizes(mAspectRatio);

        // Pick the smallest of those big enough
        for (Size size : candidates) {
            if (size.getWidth() >= surfaceLonger && size.getHeight() >= surfaceShorter) {
                return size;
            }
        }
        // If no size is big enough, pick the largest one.
        return candidates.last();
    }

    private void collectPictureSizes(SizeMap sizes, StreamConfigurationMap map) {
        for (android.util.Size size : map.getOutputSizes(ImageFormat.JPEG)) {
            mPictureSizes.add(new Size(size.getWidth(), size.getHeight()));
        }
    }

    private List<Surface> getSurfaceListByMode() {
        List<Surface> surfaceList = new ArrayList<>();
        Surface surface;
        surfaceList.add(mPreview.getSurface());
        if (mConfig.getMode() == CameraView.MODE.PICTURE && mImageReader != null) {
            surfaceList.add(mImageReader.getSurface());
        } else if (mConfig.getMode() == CameraView.MODE.VIDEO && mMediaRecorder != null) {
            try {
                surface = mMediaRecorder.getSurface();
                surfaceList.add(surface);
            } catch (IllegalStateException e) {
                // called before prepare(), after stop(), or is called when VideoSource is not set to SURFACE.
                // so we just ignore it.
            }
        }

        return surfaceList;
    }

    private void prepareImageReader() {
        if (mImageReader != null) {
            mImageReader.close();
        }
        Size largest = mPictureSizes.sizes(mAspectRatio).last();
        mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                ImageFormat.JPEG, /* maxImages */ 2);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);
    }

    private boolean prepareMediaRecorder() {
        // 默认使用最高质量拍摄
        CamcorderProfile profile =
                CamcorderProfile.
                        get(Integer.valueOf((String) mConfig.getCurrentCameraId()), CamcorderProfile.QUALITY_720P);

        Size previewSize = chooseOptimalSize();

        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);

        mMediaRecorder.setOutputFormat(profile.fileFormat);

        mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
//        mMediaRecorder.setVideoSize(mAspectRatio.getX(), mAspectRatio.getY());
//        mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mMediaRecorder.setVideoSize(previewSize.getWidth(), previewSize.getHeight());
        @SuppressWarnings("ConstantConditions")
        int sensorOrientation = mCameraCharacteristics.get(
                CameraCharacteristics.SENSOR_ORIENTATION);
        int rotation = (sensorOrientation +
                mDisplayOrientation * (mConfig.getCurrentFacing() == Constants.CAMERA_FACING_FRONT ? 1 : -1) +
                360) % 360;
        mMediaRecorder.setOrientationHint(rotation);
        mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        mMediaRecorder.setVideoEncoder(profile.videoCodec);

        mMediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
        mMediaRecorder.setAudioChannels(profile.audioChannels);
        mMediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
        mMediaRecorder.setAudioEncoder(profile.audioCodec);

        mMediaRecorder.setOutputFile(mConfig.getResultFile().getAbsolutePath());

        // 如果针对拍摄的文件大小和拍摄时间有设置的话，也可以在这里设置

        try {
            mMediaRecorder.prepare();

            return true;
        } catch (Exception e) {
            Log.d(TAG, "prepareMediaRecorder: " + e);
        }

        // 这个时候出现了错误，应该释放有关资源
        releaseVideoRecorder();
        return false;
    }

    private void releaseVideoRecorder() {
        try {
            if (mMediaRecorder != null) {
                mMediaRecorder.reset();
                mMediaRecorder.release();
            }
        } catch (Exception ignore) {

        } finally {
            mMediaRecorder = null;
        }
    }

    private void startOpeningCamera() {
        try {
            mCameraManager.openCamera(
                    mDeviceInfo.getDeviceByFacing(mConfig.getCurrentFacing()).getCameraId(), mCameraDeviceCallback, null);
        } catch (CameraAccessException e) {
            throw new RuntimeException("Failed to open camera: " + mConfig.getCurrentFacing(), e);
        }
    }

    /**
     * A {@link CameraCaptureSession.CaptureCallback} for capturing a still picture.
     */
    private static abstract class PictureCaptureCallback
            extends CameraCaptureSession.CaptureCallback {

        static final int STATE_PREVIEW = 0;
        static final int STATE_LOCKING = 1;
        static final int STATE_LOCKED = 2;
        static final int STATE_PRECAPTURE = 3;
        static final int STATE_WAITING = 4;
        static final int STATE_CAPTURING = 5;

        private int mState;

        PictureCaptureCallback() {
        }

        void setState(int state) {
            mState = state;
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            process(result);
        }

        private void process(@NonNull CaptureResult result) {
            switch (mState) {
                case STATE_LOCKING: {
                    Integer af = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (af == null) {
                        break;
                    }
                    if (af == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                            af == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                        Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (ae == null || ae == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            setState(STATE_CAPTURING);
                            onReady();
                        } else {
                            setState(STATE_LOCKED);
                            onPrecaptureRequired();
                        }
                    }
                    break;
                }
                case STATE_PRECAPTURE: {
                    Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (ae == null || ae == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            ae == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED ||
                            ae == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                        setState(STATE_WAITING);
                    }
                    break;
                }
                case STATE_WAITING: {
                    Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (ae == null || ae != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        setState(STATE_CAPTURING);
                        onReady();
                    }
                    break;
                }
            }
        }

        /**
         * Called when it is ready to take a still picture.
         */
        public abstract void onReady();

        /**
         * Called when it is necessary to run the precapture sequence.
         */
        public abstract void onPrecaptureRequired();

    }

    private Rect calculateTapArea(float x, float y, float coefficient, int width, int height) {
        float baseAreaSize = 100;

        Rect cameraArrayRect = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

        int areaSize = Float.valueOf(baseAreaSize * coefficient).intValue();
        int centerX = (int) (x / width * cameraArrayRect.right);
        int centerY = (int) (y / height * cameraArrayRect.bottom);

        int halfAreaSize = areaSize / 2;
        // 点击的矩形区域
        RectF rectF = new RectF(
                clamp(centerX - halfAreaSize, 0, cameraArrayRect.width() - 1),
                clamp(centerY - halfAreaSize, 0, cameraArrayRect.height() - 1),
                clamp(centerX + halfAreaSize, 0, cameraArrayRect.width() - 1),
                clamp(centerY + halfAreaSize, 0, cameraArrayRect.height() - 1));
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
