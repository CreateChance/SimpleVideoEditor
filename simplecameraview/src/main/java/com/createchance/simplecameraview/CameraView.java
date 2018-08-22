package com.createchance.simplecameraview;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.myway.video.maker.libcameraview.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * CameraView 接口类
 *
 * @author gaochao1-iri
 */

public class CameraView extends FrameLayout {

    private static final String TAG = "CameraView";

    // camera功能的实现类，所有功能的实现
    private CameraImpl mImpl;

    // camera操作回调，由cameraview中转给所有注册的组件
    private final CameraCallbackBridge mCallbackBridge;

    private boolean mAdjustViewBounds;

    // camera view当前的显示方向，根据用户显示中设置的结果确定，camera利用这个方向拍摄正方向照片或者视频
    private DisplayOrientationDetector mDisplayOrientationDetector;

    // cameraview所处于的模式，默认为拍照模式
    public enum MODE {
        // 拍照模式
        PICTURE,
        // 录像模式
        VIDEO
    }

    private boolean isVideoTaking = false;

    private boolean isPictureTaking = false;

    private Context mContext;

    private CameraConfig mConfig = new CameraConfig();

    // 两个手指的距离
    private float oldDist = 1f;

    private PreviewImpl mPreviewImpl;

    public CameraView(@NonNull Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mContext = context;

        if (isInEditMode()) {
            mCallbackBridge = null;
            mDisplayOrientationDetector = null;
            return;
        }

        mCallbackBridge = new CameraCallbackBridge();

        mPreviewImpl = createPreviewImpl(context);
        // 各个系统版本摄像头api兼容，目前兼容jb的camera v1和lp的camera v2
        if (Build.VERSION.SDK_INT >= 21) {
            mImpl = new Camera2(mCallbackBridge, mPreviewImpl, mConfig, context);
        } else {
            mImpl = new Camera1(mCallbackBridge, mPreviewImpl, mConfig, context);
        }
//        mImpl = new Camera1(mCallbackBridge, preview, mConfig, context);

        TypedArray typedArray = mContext.obtainStyledAttributes(attrs, R.styleable.CameraView, defStyleAttr, R.style.Widget_CameraView);
        mAdjustViewBounds = typedArray.getBoolean(R.styleable.CameraView_android_adjustViewBounds, false);
        typedArray.recycle();
        // Display orientation detector
        mDisplayOrientationDetector = new DisplayOrientationDetector(mContext) {
            @Override
            public void onDisplayOrientationChanged(int displayOrientation) {
                mImpl.setDisplayOrientation(displayOrientation);
            }
        };
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            mDisplayOrientationDetector.enable(ViewCompat.getDisplay(this));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            mDisplayOrientationDetector.disable();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isInEditMode()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        // Handle android:adjustViewBounds
        if (mAdjustViewBounds) {
            if (!isCameraOpened()) {
                mCallbackBridge.reserveRequestLayoutOnOpen();
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }
            final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
                final AspectRatio ratio = getAspectRatio();
                assert ratio != null;
                int height = (int) (MeasureSpec.getSize(widthMeasureSpec) * ratio.toFloat());
                if (heightMode == MeasureSpec.AT_MOST) {
                    height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));
                }
                super.onMeasure(widthMeasureSpec,
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            } else if (widthMode != MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
                final AspectRatio ratio = getAspectRatio();
                assert ratio != null;
                int width = (int) (MeasureSpec.getSize(heightMeasureSpec) * ratio.toFloat());
                if (widthMode == MeasureSpec.AT_MOST) {
                    width = Math.min(width, MeasureSpec.getSize(widthMeasureSpec));
                }
                super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                        heightMeasureSpec);
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
        // Measure the TextureView
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        AspectRatio ratio = getAspectRatio();
        if (mDisplayOrientationDetector.getLastKnownDisplayOrientation() % 180 == 0) {
            ratio = ratio.inverse();
        }
        assert ratio != null;
        if (height < width * ratio.getY() / ratio.getX()) {
            mImpl.getView().measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(width * ratio.getY() / ratio.getX(),
                            MeasureSpec.EXACTLY));
        } else {
            mImpl.getView().measure(
                    MeasureSpec.makeMeasureSpec(height * ratio.getX() / ratio.getY(),
                            MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isCameraOpened()) {
            if (event.getPointerCount() == 1) {
                if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                    mImpl.triggerFocus(event);
                }
            } else {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_POINTER_DOWN:
                        oldDist = getFingerSpacing(event);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float newDist = getFingerSpacing(event);
                        if (newDist > oldDist) {
                            mImpl.zoom(true);
                        } else if (newDist < oldDist) {
                            mImpl.zoom(false);
                        }
                        oldDist = newDist;
                        break;
                }
            }
        } else {
            return super.onTouchEvent(event);
        }

        return true;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.facing = getCamera();
        state.ratio = getAspectRatio();
        state.autoFocus = getAutoFocus();
        state.flash = getFlashLightState();
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setCamera(ss.facing);
        setAspectRatio(ss.ratio);
        setAutoFocus(ss.autoFocus);
        setFlashLightState(ss.flash);
    }

    /**
     * 设置当前模式，参数中的模式必须是{@link MODE}中的一种
     */
    public void setMode(MODE mode) {
        // rational check
        if (mode == null) {
            throw new IllegalStateException("错误的模式: null!");
        }

        if (mConfig.getMode() == mode) {
            return;
        }

        if (mode == MODE.VIDEO && isPictureTaking) {
            mCallbackBridge.onError(ICameraCallback.ERROR_CODE_OPERATION_NOT_SUPPORTED);
            return;
        }

        if (mode == MODE.PICTURE && isVideoTaking) {
            mCallbackBridge.onError(ICameraCallback.ERROR_CODE_OPERATION_NOT_SUPPORTED);
            return;
        }

        if (mImpl.isCameraOpened()) {
            stop();
            mConfig.setMode(mode);
            start();
        } else {
            mConfig.setMode(mode);
        }
    }

    /**
     * 设置当前模式，返回的模式必须是{@link MODE}中的一种
     *
     * @return mode
     */
    public MODE getMode() {
        return mConfig.getMode();
    }

    /**
     * 这是必须要调用的一个接口，用于开启相机并且开始预览
     * 注意，反复的调用start没有任何效果
     */
    public void start() {
        mImpl.start(mConfig.getMode());
    }

    /**
     * 每当结束使用camera view时候都要调用此方法，此方法是最后一个方法，用于关闭和相机的链接，并且回收资源
     */
    public void stop() {
        // rational check
        if (!isCameraOpened()) {
            return;
        }

        if (isVideoTaking || isPictureTaking) {
            mConfig.setNeedToStop(true);
            // 现在正在拍摄或者正在录像，因此不能关闭，保存下来，等到完成之后关闭
            return;
        }

        mImpl.stop(mConfig.getMode());
    }

    /**
     * 用于切换摄像头，参数必须是{@link Constants#CAMERA_FACING_BACK}表示后置摄像头
     * 或者{@link Constants#CAMERA_FACING_FRONT}表示前置摄像头
     *
     * @param cameraId 摄像头id
     */
    public void setCamera(int cameraId) {
        // rational check
        if (cameraId != Constants.CAMERA_FACING_BACK && cameraId != Constants.CAMERA_FACING_FRONT) {
            return;
        }

        if (isCameraOpened()) {
            mImpl.setFacing(cameraId);
        } else {
            mConfig.setCurrentFacing(cameraId);
        }
    }

    /**
     * 用于获得当前使用的摄像头id，返回值一定是{@link Constants#CAMERA_FACING_BACK}表示后置摄像头
     * 或者{@link Constants#CAMERA_FACING_FRONT}表示前置摄像头
     *
     * @return camera id
     */
    public int getCamera() {
        if (isCameraOpened()) {
            return mImpl.getFacing();
        } else {
            return Constants.FATAL_ERROR;
        }
    }

    /**
     * 设置相机白平衡，前提需要相机先打开，如果相机没有打开的话，先保存设置，带相机打开之后统一设置。
     *
     * @param value 白平衡的值定义在{@link Constants}类中
     */
    public void setWhiteBalance(int value) {
        // rational check
        if (value != Constants.AWB_MODE_AUTO &&
                value != Constants.AWB_MODE_CLOUDY_DAYLIGHT &&
                value != Constants.AWB_MODE_DAYLIGHT &&
                value != Constants.AWB_MODE_FLUORESCENT &&
                value != Constants.AWB_MODE_INCANDESCENT &&
                value != Constants.AWB_MODE_SHADE &&
                value != Constants.AWB_MODE_WARM_FLUORESCENT &&
                value != Constants.AWB_MODE_TWILIGHT &&
                value != Constants.AWB_MODE_OFF) {
            return;
        }

        if (isCameraOpened()) {
            mImpl.setWhiteBalance(value);
        } else {
            mConfig.setCurrentAWBMode(value);
        }
    }

    /**
     * 获得当前相机的白平衡的值，前提是相机打开，返回的值定义在{@link Constants}类中
     * 如果相机没有打开的话，返回一个错误值{@link Constants#FATAL_ERROR}
     *
     * @return 白平衡的值
     */
    public int getWhiteBalance() {
        if (isCameraOpened()) {
            return mImpl.getWhiteBalance();
        } else {
            return Constants.FATAL_ERROR;
        }
    }

    /**
     * 获得当前相机支持的白平衡列表，列表中的值定义在{@link Constants}类中
     * 调用改接口的前提是相机打开，如果没有打开，返回null。
     *
     * @return 支持的白平衡列表
     */
    public List<Integer> getSupportedWhiteBalance() {
        if (isCameraOpened()) {
            return mImpl.getSupportedWhiteBalance();
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * 设置当前相机的曝光补偿值，相机支持的补偿值范围通过{@link CameraView#getExposureCompensationRange}
     * 方法获取。
     *
     * @param value 曝光补偿值
     */
    public void setExposureCompensation(int value) {
        if (isCameraOpened()) {
            mImpl.setExposureCompensation(value);
        } else {
            mConfig.setCurrentExposureCompensation(value);
        }
    }

    /**
     * 获得当前相机的曝光补偿值。通过当前补偿值乘以通过{@link CameraView#getExposureCompensationStep()}
     * 获得的步长值，可以获得当前曝光补偿量，这个值是一般相机app使用的。
     *
     * @return 当前相机曝光补偿值
     */
    public int getExposureCompensation() {
        if (isCameraOpened()) {
            return mImpl.getExposureCompensation();
        } else {
            return Constants.FATAL_ERROR;
        }
    }

    /**
     * 获得当前相机支持的曝光补偿值范围，返回的列表一共有2个值，第一个值是最低曝光值，第二个是最大曝光值。
     * app需要首先调用本接口，然后才能调用{@link CameraView#setExposureCompensation(int)}
     *
     * @return 当前相机支持的曝光补偿值范，包含最大值和最小值
     */
    public List<Integer> getExposureCompensationRange() {
        if (isCameraOpened()) {
            return mImpl.getExposureCompensationRange();
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * 获得当前相机的曝光补偿值的步长，通过这个值和当前曝光值相乘以获得在-2～+2之间的一个浮点型曝光量。
     * 一般app会在界面上显示这个值给用户看。
     *
     * @return 当前相机的曝光补偿值的步长
     */
    public float getExposureCompensationStep() {
        if (isCameraOpened()) {
            return mImpl.getExposureCompensationStep();
        } else {
            return Constants.FATAL_ERROR;
        }
    }

    /**
     * 获得当前相机是不是已经打开了。
     *
     * @return true表示已经打开，false表示关闭
     */
    public boolean isCameraOpened() {
        return mImpl.isCameraOpened();
    }

    /**
     * 设置当前相机闪光灯状态，注意，如果当前相机没有打开的话会保存设置，如果打开了，但是相机不支持闪光灯，
     * 那么app会收到{@link ICameraCallback#ERROR_CODE_NO_FLASH_LIGTH}的错误回调。
     * 注意，当前如果是拍照模式{@link MODE#PICTURE}的话，能够使用的模式只有{@link Constants#FLASH_LIGHT_OFF},
     * {@link Constants#FLASH_LIGHT_ON},{@link Constants#FLASH_LIGHT_AUTO},{@link Constants#FLASH_LIGHT_RED_EYE}
     * 如果是录像模式{@link MODE#VIDEO}的话，能够使用的模式之后{@link Constants#FLASH_LIGHT_OFF},
     * {@link Constants#FLASH_LIGHT_TORCH}，错误使用的话，会收到{@link ICameraCallback#ERROR_CODE_OPERATION_NOT_SUPPORTED}
     * 的错误回调。
     *
     * @param state 闪光灯的状态，支持的所有状态定义在{@link Constants}类中
     */
    public void setFlashLightState(int state) {
        // rational check
        if (state != Constants.FLASH_LIGHT_ON &&
                state != Constants.FLASH_LIGHT_OFF &&
                state != Constants.FLASH_LIGHT_TORCH &&
                state != Constants.FLASH_LIGHT_AUTO &&
                state != Constants.FLASH_LIGHT_RED_EYE) {
            return;
        }

        if (isCameraOpened()) {
            mImpl.setFlashLightState(state);
        } else {
            mConfig.setCurrentFlashlightState(state);
        }
    }

    /**
     * 获得当前摄像头的闪光灯模式
     *
     * @return 当前摄像头闪光灯模式
     */
    public int getFlashLightState() {
        if (isCameraOpened()) {
            return mImpl.getFlashLightState();
        } else {
            return Constants.FATAL_ERROR;
        }
    }

    /**
     * 设置摄像头采集的数据纵横比，{@link AspectRatio}定义了关于纵横比的逻辑
     * 比如：AspectRatio.parse("16:9"), 可以获得16:9的比例对象
     *
     * @param ratio {@link AspectRatio}的实例
     */
    public void setAspectRatio(@NonNull AspectRatio ratio) {
        if (isCameraOpened()) {
            if (mImpl.setAspectRatio(ratio)) {
                // 重新调整view布局
                requestLayout();
            }
        } else {
            mConfig.setAspectRatio(ratio);
        }
    }

    /**
     * 获得当前摄像头的纵横比设置，如果相机没有开启，就使用默认纵横比
     *
     * @return 返回纵横比设置
     */
    public AspectRatio getAspectRatio() {
        if (isCameraOpened()) {
            return mImpl.getAspectRatio();
        } else {
            return Constants.DEFAULT_ASPECT_RATIO;
        }
    }

    /**
     * 设置当前相机是否自动聚集，默认自动聚焦
     * 内部实现会根据当前的模式是照相还是录像自动选择相机支持的最佳聚焦模式
     *
     * @param autoFocus 是否聚焦
     */
    public void setAutoFocus(boolean autoFocus) {
        if (isCameraOpened()) {
            mImpl.setAutoFocus(autoFocus);
        } else {
            mConfig.setAutoFocus(autoFocus);
        }
    }

    /**
     * 获得当前相机是否自动聚焦
     *
     * @return true自动聚焦，false没有自动聚焦
     */
    public boolean getAutoFocus() {
        if (isCameraOpened()) {
            return mImpl.getAutoFocus();
        } else {
            return mConfig.isAutoFocus();
        }
    }

    /**
     * 开始录像，注意参数不能为null，否则会触发空指针异常
     *
     * @param videoFile 指定的录制完成的文件，如果文件存在则覆盖，如果不存在则创建
     */
    public void startTakingVideo(File videoFile) {
        // rational check
        if (videoFile == null) {
            // Are you kidding me??!!
            throw new NullPointerException("Video file can not be null!");
        }

        if (!mImpl.isCameraOpened()) {
            throw new IllegalStateException("请先调用start()方法开启摄像头！");
        }

        if (mConfig.getMode() != MODE.VIDEO) {
            throw new IllegalStateException(
                    "错误的模式使用，当前模式：" + mConfig.getMode() + "， 需要的模式：" + MODE.VIDEO);
        }

        if (isVideoTaking) {
            // we are fucking busy, don't give a shit to this.
            return;
        }
        mImpl.startTakingVideo(videoFile);
        isVideoTaking = true;
    }

    /**
     * 停止录像，获得一个录制完成的video文件
     */
    public void stopTakingVideo() {
        if (!mImpl.isCameraOpened()) {
            throw new IllegalStateException("请先调用start()方法开启摄像头！");
        }

        if (mConfig.getMode() != MODE.VIDEO) {
            throw new IllegalStateException(
                    "错误的模式使用，当前模式：" + mConfig.getMode() + "， 需要的模式：" + MODE.VIDEO);
        }

        if (!isVideoTaking) {
            throw new IllegalStateException("当前没有录制，无法停止！");
        }

        mImpl.stopTakingVideo();
        isVideoTaking = false;
    }

    /**
     * 取消录制，删除录制文件
     */
    public void cancelTakingVideo() {
        Log.d(TAG, "cancel take video.");
        if (!mImpl.isCameraOpened()) {
            throw new IllegalStateException("请先调用start()方法开启摄像头！");
        }

        if (mConfig.getMode() != MODE.VIDEO) {
            throw new IllegalStateException(
                    "错误的模式使用，当前模式：" + mConfig.getMode() + "， 需要的模式：" + MODE.VIDEO);
        }

        if (!isVideoTaking) {
            throw new IllegalStateException("当前没有录制，无法取消！");
        }
        mImpl.cancelTakingVideo();
    }

    /**
     * 拍摄一张照片，注意参数不能为null，否则会触发空指针异常
     *
     * @param pictureFile 照片文件对象，如果文件存在则覆盖，如果不存在则创建
     */
    public void startTakingPicture(File pictureFile) {
        // rational check
        if (pictureFile == null) {
            // Are you kidding me??!!
            throw new NullPointerException("Video file can not be null!");
        }

        if (!mImpl.isCameraOpened()) {
            throw new IllegalStateException("请先调用start()方法开启摄像头！");
        }

        if (mConfig.getMode() != MODE.PICTURE) {
            throw new IllegalStateException(
                    "错误的模式使用，当前模式：" + mConfig.getMode() + "， 需要的模式：" + MODE.PICTURE);
        }
        mImpl.startTakingPicture(pictureFile);
        isPictureTaking = true;
    }

    /**
     * 添加app回调，camera的一些重要消息会回调，{@link ICameraCallback}
     *
     * @param cameraCallback callback
     */
    public void addCameraCallback(ICameraCallback cameraCallback) {
        mCallbackBridge.add(cameraCallback);
    }

    /**
     * 移除回调
     *
     * @param cameraCallback callback
     */
    public void removeCameraCallback(ICameraCallback cameraCallback) {
        mCallbackBridge.remove(cameraCallback);
    }

    @NonNull
    private PreviewImpl createPreviewImpl(Context context) {
        return new TextureViewPreview(context, this);
    }

    private class CameraCallbackBridge implements ICameraCallback {

        private final ArrayList<ICameraCallback> mCallbacks = new ArrayList<>();

        private boolean mRequestLayoutOnOpen;

        CameraCallbackBridge() {
            // do nothing for now.
        }

        public void add(ICameraCallback cameraCallback) {
            mCallbacks.add(cameraCallback);
        }

        public void remove(ICameraCallback cameraCallback) {
            mCallbacks.remove(cameraCallback);
        }

        @Override
        public void onCameraOpened() {
            if (mRequestLayoutOnOpen) {
                mRequestLayoutOnOpen = false;
                requestLayout();
            }
            for (ICameraCallback cameraCallback : mCallbacks) {
                cameraCallback.onCameraOpened();
            }
        }

        @Override
        public void onCameraClosed() {
            for (ICameraCallback cameraCallback : mCallbacks) {
                cameraCallback.onCameraClosed();
            }
        }

        @Override
        public void onVideoTaken(File videoFile) {
            for (ICameraCallback cameraCallback : mCallbacks) {
                cameraCallback.onVideoTaken(videoFile);
            }
            isVideoTaking = false;
            if (mConfig.isNeedToStop()) {
                mImpl.stop(mConfig.getMode());
            }
        }

        @Override
        public void onError(int code) {
            for (ICameraCallback cameraCallback : mCallbacks) {
                cameraCallback.onError(code);
            }

            // 保存失败，重置状态
            if (code == ICameraCallback.ERROR_CODE_SAVED_FAILED) {
                if (isPictureTaking) {
                    isPictureTaking = false;
                }

                if (isVideoTaking) {
                    isVideoTaking = false;
                }
            }
        }

        @Override
        public void onVideoTakeStarted() {
            for (ICameraCallback cameraCallback : mCallbacks) {
                cameraCallback.onVideoTakeStarted();
            }
        }

        @Override
        public void onVideoTakeCanceled() {
            for (ICameraCallback cameraCallback : mCallbacks) {
                cameraCallback.onVideoTakeCanceled();
            }
            isVideoTaking = false;
        }

        @Override
        public void onPictureTaken(File pictureFile) {
            for (ICameraCallback cameraCallback : mCallbacks) {
                cameraCallback.onPictureTaken(pictureFile);
            }
            isPictureTaking = false;
            if (mConfig.isNeedToStop()) {
                mImpl.stop(mConfig.getMode());
            }
        }

        public void reserveRequestLayoutOnOpen() {
            mRequestLayoutOnOpen = true;
        }
    }

    protected static class SavedState extends BaseSavedState {

        int facing;

        AspectRatio ratio;

        boolean autoFocus;

        int flash;

        @SuppressWarnings("WrongConstant")
        public SavedState(Parcel source, ClassLoader loader) {
            super(source);
            facing = source.readInt();
            ratio = source.readParcelable(loader);
            autoFocus = source.readByte() != 0;
            flash = source.readInt();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(facing);
            out.writeParcelable(ratio, 0);
            out.writeByte((byte) (autoFocus ? 1 : 0));
            out.writeInt(flash);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<SavedState>() {

            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }

        });

    }

    private static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    private int dip2px(float dpValue) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
