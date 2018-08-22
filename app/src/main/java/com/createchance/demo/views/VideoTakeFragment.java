package com.createchance.demo.views;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.createchance.demo.BuildConfig;
import com.createchance.demo.R;
import com.createchance.demo.model.Scene;
import com.createchance.simplecameraview.CameraView;
import com.createchance.simplecameraview.Constants;
import com.createchance.simplecameraview.ICameraCallback;

import java.io.File;

public class VideoTakeFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "VideoTakeFragment";

    private CameraView mCameraView;

    private Callback mCallback;

    private RoundProgressbar mCountDownView;
    private long mCountDownTime = 9999;
    private long mStartTime;
    private final int MSG_UPDATE_COUNTER = 100;
    private final int MSG_START_COUNTER = 101;
    private final int MSG_FINISH_TAKING_VIDEO = 102;

    private Scene mScene;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case MSG_UPDATE_COUNTER:
                    updateCounter();
                    break;
                case MSG_START_COUNTER:
                    mStartTime = System.currentTimeMillis();
                    mHandler.sendEmptyMessage(MSG_UPDATE_COUNTER);
                    break;
                case MSG_FINISH_TAKING_VIDEO:
                    mCameraView.stopTakingVideo();
                    break;
                default:
                    break;
            }
        }
    };

    public VideoTakeFragment() {

    }

    public void setScene(Scene scene) {
        this.mScene = scene;
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_video_take, container, false);

        mCameraView = root.findViewById(R.id.vw_camera_view);
        mCameraView.addCameraCallback(new ICameraCallback() {
            @Override
            public void onCameraOpened() {
                Log.d(TAG, "onCameraOpened: ");
            }

            @Override
            public void onCameraClosed() {
                Log.d(TAG, "onCameraClosed: ");
            }

            @Override
            public void onVideoTakeStarted() {
                Log.d(TAG, "onVideoTakeStarted: ");
            }

            @Override
            public void onVideoTakeCanceled() {
                Log.d(TAG, "onVideoTakeCanceled: ");
            }

            @Override
            public void onVideoTaken(File videoFile) {
                Log.d(TAG, "onVideoTaken: " + videoFile);
                if (mCallback != null) {
                    mCallback.onVideoToken();
                }
            }

            @Override
            public void onPictureTaken(File pictureFile) {
                Log.d(TAG, "onPictureTaken: " + pictureFile);
            }

            @Override
            public void onError(int code) {
                Log.d(TAG, "onError: " + code);
            }
        });
        mCameraView.setCamera(Constants.CAMERA_FACING_BACK);
        mCameraView.setMode(CameraView.MODE.VIDEO);
        mCameraView.setAutoFocus(true);
        mCountDownView = root.findViewById(R.id.vw_start_taking);
        mCountDownView.setOnClickListener(this);
        root.findViewById(R.id.iv_switch_camera).setOnClickListener(this);

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        mCameraView.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        mCameraView.stop();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_switch_camera:
                if (mCameraView.getCamera() == Constants.CAMERA_FACING_BACK) {
                    mCameraView.setCamera(Constants.CAMERA_FACING_FRONT);
                } else {
                    mCameraView.setCamera(Constants.CAMERA_FACING_BACK);
                }
                break;
            case R.id.vw_start_taking:
                if (mCameraView.isCameraOpened()) {
                    mCameraView.startTakingVideo(mScene.video);
                    mHandler.sendEmptyMessage(MSG_START_COUNTER);
                    mHandler.sendEmptyMessageDelayed(MSG_FINISH_TAKING_VIDEO, mCountDownTime);
                }
                break;
            default:
                break;
        }
    }

    private void updateCounter() {
        if (getActivity().isFinishing() || getActivity().isDestroyed()) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "updateCounter, but activity is finishing or destroyed!");
            }
            return;
        }

        long now = System.currentTimeMillis();

        if ((now - mStartTime) >= mCountDownTime) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "updateCounter done!");
            }
            mCountDownView.setProgress(100.0f);
            mCountDownView.setText("0s");
        } else {
            float progress = (now - mStartTime) * 1.0f / mCountDownTime;
            mCountDownView.setProgress(progress);
            String text = String.valueOf((int) ((mCountDownTime - now + mStartTime) * 1.0 / 1000) + 1) + "s";
            mCountDownView.setText(text);
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "updateCounter, progress: " + progress + ", text: " + text);
            }
            mHandler.sendEmptyMessage(MSG_UPDATE_COUNTER);
        }
    }

    interface Callback {
        void onVideoToken();
    }
}
