package com.createchance.demo.views;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.createchance.demo.R;
import com.createchance.demo.model.Scene;
import com.createchance.demo.model.SimpleModelManager;

import java.io.File;

public class VideoTakeAndCheckActivity extends Activity implements VideoTakeFragment.Callback, VideoCheckFragment.Callback {

    private static final String TAG = "VideoTakeAndCheckActivity";

    private static final String EXTRA_SCENE_INDEX = "scene_index";

    private Scene mScene;

    private FragmentManager mFragmentManager;
    private VideoTakeFragment mVideoTakeFragment;
    private VideoCheckFragment mVideoCheckFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_take_and_check);

        mFragmentManager = getFragmentManager();
        mVideoTakeFragment = new VideoTakeFragment();
        mVideoCheckFragment = new VideoCheckFragment();

        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(option);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        Intent intent = getIntent();
        if (intent != null) {
            int sceneIndex = intent.getIntExtra(EXTRA_SCENE_INDEX, -1);
            if (sceneIndex > 0) {
                mScene = SimpleModelManager.getInstance().getSceneList().get(sceneIndex);
                mScene.video = new File(getFilesDir(), System.currentTimeMillis() + ".mp4");
                mVideoTakeFragment.setScene(mScene);
                mVideoTakeFragment.setCallback(this);
                mVideoCheckFragment.setScene(mScene);
                mVideoCheckFragment.setCallback(this);

                FragmentTransaction transaction = mFragmentManager.beginTransaction();
                transaction.replace(R.id.vw_fragment_container, mVideoTakeFragment);
                transaction.commit();
            }
        }

    }

    @Override
    public void onBackPressed() {
        mScene.video.delete();
        super.onBackPressed();
    }

    public static void start(Activity context, int sceneIndex, int requestCode) {
        Intent intent = new Intent(context, VideoTakeAndCheckActivity.class);
        intent.putExtra(EXTRA_SCENE_INDEX, sceneIndex);
        context.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onRedo() {
        Log.d(TAG, "onRedo: ");
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        transaction.replace(R.id.vw_fragment_container, mVideoTakeFragment);
        transaction.commit();
    }

    @Override
    public void onDone() {
        Log.d(TAG, "onDone: ");
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onVideoToken() {
        Log.d(TAG, "onVideoToken: ");
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        transaction.replace(R.id.vw_fragment_container, mVideoCheckFragment);
        transaction.commit();
    }
}
