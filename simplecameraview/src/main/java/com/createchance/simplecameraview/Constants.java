package com.createchance.simplecameraview;

/**
 * 常量定义类
 */

public class Constants {

    // 这代表前置摄像头
    public static final int CAMERA_FACING_FRONT = 0;
    // 这代表后置摄像头
    public static final int CAMERA_FACING_BACK = 1;

    // 开启闪光灯
    public static final int FLASH_LIGHT_ON = 100;
    // 关闭闪光灯
    public static final int FLASH_LIGHT_OFF = 101;
    // 自动闪光灯
    public static final int FLASH_LIGHT_AUTO = 102;
    // 闪光灯常亮，手电筒模式
    public static final int FLASH_LIGHT_TORCH = 103;
    // 防止红眼模式
    public static final int FLASH_LIGHT_RED_EYE = 104;
    // 这表示当前闪光灯不可用，可能是没有闪光灯设备
    public static final int FLASH_LIGHT_NOT_AVALIABLE = 199;

    // 白平衡模式定义
    public static final int AWB_MODE_OFF = 200;
    public static final int AWB_MODE_CLOUDY_DAYLIGHT = 201;
    public static final int AWB_MODE_DAYLIGHT = 202;
    public static final int AWB_MODE_FLUORESCENT = 203;
    public static final int AWB_MODE_INCANDESCENT = 204;
    public static final int AWB_MODE_SHADE = 205;
    public static final int AWB_MODE_TWILIGHT = 206;
    public static final int AWB_MODE_WARM_FLUORESCENT = 207;
    public static final int AWB_MODE_AUTO = 208;

    // 默认纵横比为16:9，宽屏
    public static AspectRatio DEFAULT_ASPECT_RATIO = AspectRatio.of(16, 9);

    // 角度定义
    public static final int LANDSCAPE_90 = 90;
    public static final int LANDSCAPE_270 = 270;

    // 错误代码
    public static final int FATAL_ERROR = 0xffffffff;
}
