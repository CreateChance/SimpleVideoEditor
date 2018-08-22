package com.createchance.demo.views;

import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;

import com.createchance.PlayRequest;
import com.createchance.SimpleVideoPlayer;
import com.createchance.demo.R;
import com.createchance.demo.model.Scene;

import java.io.File;

public class VideoCheckFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "VideoCheckFragment";

    private Scene mScene;

    private Callback mCallback;

    private VideoView mVideoPlayer;

    public void setScene(Scene scene) {
        this.mScene = scene;
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_video_check, container, false);

        root.findViewById(R.id.iv_redo).setOnClickListener(this);
        root.findViewById(R.id.iv_done).setOnClickListener(this);
        mVideoPlayer = root.findViewById(R.id.vw_video_player);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

//        PlayRequest request = new PlayRequest.Builder()
//                .loop(true)
//                .source(Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "videoeditor/input.mp4")))
//                .build();
//        mVideoPlayer.start(request);
        mVideoPlayer.setVideoURI(Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "videoeditor/input.mp4")));
        mVideoPlayer.start();
    }

    @Override
    public void onStop() {
        super.onStop();

        mVideoPlayer.stopPlayback();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_redo:
                if (mCallback != null) {
                    mCallback.onRedo();
                }
                break;
            case R.id.iv_done:
                if (mCallback != null) {
                    mCallback.onDone();
                }
                break;
            default:
                break;
        }
    }

    interface Callback {
        void onRedo();

        void onDone();
    }
}
