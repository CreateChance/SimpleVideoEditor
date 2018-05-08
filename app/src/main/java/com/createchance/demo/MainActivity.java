package com.createchance.demo;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.createchance.simplevideoeditor.VideoBgmAddAction;
import com.createchance.simplevideoeditor.VideoEditCallback;
import com.createchance.simplevideoeditor.VideoEditorManager;
import com.createchance.simplevideoeditor.VideoWatermarkAddAction;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void start(View view) {
        Log.d(TAG, "start click!!!!!!!!!!!!");
        VideoWatermarkAddAction watermarkAddAction = new VideoWatermarkAddAction.Builder()
                .watermark(BitmapFactory.decodeResource(getResources(), R.drawable.watermark))
                .atXPos(200)
                .atYPos(400)
                .build();
        VideoBgmAddAction bgmAddAction = new VideoBgmAddAction.Builder()
                .bgmFile(new File(Environment.getExternalStorageDirectory(), "videoeditor/music.aac"))
                .videoFrom(5 * 1000)
                .videoDuration(8 * 1000)
                .build();
        VideoEditorManager.getManager()
                .edit(new File(Environment.getExternalStorageDirectory(), "videoeditor/input1.mp4"))
                .withAction(bgmAddAction)
                .saveAs(new File(Environment.getExternalStorageDirectory(), "videoeditor/output.mp4"))
                .commit(new VideoEditCallback() {
                    @Override
                    public void onStart(String action) {
                        super.onStart(action);
                        Log.d(TAG, "onStart: " + action);
                    }

                    @Override
                    public void onProgress(String action, float progress) {
                        super.onProgress(action, progress);
                        Log.d(TAG, "onProgress: " + action + ", progress: " + progress);
                    }

                    @Override
                    public void onSucceeded(String action) {
                        super.onSucceeded(action);
                        Log.d(TAG, "onSucceeded: " + action);
                    }

                    @Override
                    public void onFailed(String action) {
                        super.onFailed(action);
                        Log.d(TAG, "onFailed: " + action);
                    }
                });
    }
}
