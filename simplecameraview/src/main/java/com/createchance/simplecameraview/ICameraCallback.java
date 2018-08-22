package com.createchance.simplecameraview;

import java.io.File;

/**
 * 相机回调接口，需要app实现来监听信息
 */

public interface ICameraCallback {

    /**
     * 摄像头操作错误码
     */
    // 没有摄像头设备
    int ERROR_CODE_NO_CAMERA_DEVICES = 100;
    // 没有闪光灯设备
    int ERROR_CODE_NO_FLASH_LIGTH = 101;
    // 没有权限
    int ERROR_CODE_NO_PERMISSION = 102;
    // 照片或者视频保存失败
    int ERROR_CODE_SAVED_FAILED = 103;
    // 不支持的操作
    int ERROR_CODE_OPERATION_NOT_SUPPORTED = 198;
    // 未知错误
    int ERROR_CODE_UNKNOWN = 199;

    void onCameraOpened();
    void onCameraClosed();
    void onVideoTakeStarted();
    void onVideoTakeCanceled();
    void onVideoTaken(File videoFile);
    void onPictureTaken(File pictureFile);
    void onError(int code);
}
