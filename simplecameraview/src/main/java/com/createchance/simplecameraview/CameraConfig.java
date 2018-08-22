package com.createchance.simplecameraview;

import java.io.File;

/**
 * 针对camera v1 & v2 api的配置javabean类
 * 用于保存相机当前的所有配置
 *
 * @author gaochao1-iri
 */

class CameraConfig {
    private boolean isCameraSupported = true;

    // 默认是拍照模式
    private CameraView.MODE mode = CameraView.MODE.PICTURE;

    private boolean isCameraOpened = false;

    private int currentFacing = Constants.CAMERA_FACING_BACK;

    // camera v1 中的id是int型的，而camera v2中的id是String型的，因此这里统一为object，使用时候自行转换
    private Object currentCameraId = null;

    private int currentFlashlightState = Constants.FLASH_LIGHT_NOT_AVALIABLE;

    private int currentExposureCompensation;

    private boolean isAutoFocus = true;

    private int currentAWBMode = Constants.AWB_MODE_AUTO;

    private int currentAEMode;

    private File resultFile = null;

    private AspectRatio aspectRatio;

    private boolean needToStop = false;

    boolean isCameraSupported() {
        return isCameraSupported;
    }

    void setCameraSupported(boolean cameraSupported) {
        isCameraSupported = cameraSupported;
    }

    boolean isCameraOpened() {
        return isCameraOpened;
    }

    void setCameraOpened(boolean cameraOpened) {
        isCameraOpened = cameraOpened;
    }

    int getCurrentFacing() {
        return currentFacing;
    }

    void setCurrentFacing(int currentFacing) {
        this.currentFacing = currentFacing;
    }

    Object getCurrentCameraId() {
        return currentCameraId;
    }

    void setCurrentCameraId(Object currentCameraId) {
        this.currentCameraId = currentCameraId;
    }

    int getCurrentFlashlightState() {
        return currentFlashlightState;
    }

    void setCurrentFlashlightState(int currentFlashlightState) {
        this.currentFlashlightState = currentFlashlightState;
    }

    int getCurrentExposureCompensation() {
        return currentExposureCompensation;
    }

    void setCurrentExposureCompensation(int currentExposureCompensation) {
        this.currentExposureCompensation = currentExposureCompensation;
    }

    boolean isAutoFocus() {
        return isAutoFocus;
    }

    void setAutoFocus(boolean autoFocus) {
        isAutoFocus = autoFocus;
    }

    int getCurrentAWBMode() {
        return currentAWBMode;
    }

    void setCurrentAWBMode(int currentAWBMode) {
        this.currentAWBMode = currentAWBMode;
    }

    int getCurrentAEMode() {
        return currentAEMode;
    }

    void setCurrentAEMode(int currentAEMode) {
        this.currentAEMode = currentAEMode;
    }

    File getResultFile() {
        return resultFile;
    }

    void setResultFile(File resultFile) {
        this.resultFile = resultFile;
    }

    CameraView.MODE getMode() {
        return mode;
    }

    void setMode(CameraView.MODE mode) {
        this.mode = mode;
    }

    AspectRatio getAspectRatio() {
        return aspectRatio;
    }

    void setAspectRatio(AspectRatio aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public boolean isNeedToStop() {
        return needToStop;
    }

    public void setNeedToStop(boolean needToStop) {
        this.needToStop = needToStop;
    }
}
