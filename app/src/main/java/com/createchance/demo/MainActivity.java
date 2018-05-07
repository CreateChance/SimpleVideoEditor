package com.createchance.demo;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

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
//        VideoCutAction cutAction = new VideoCutAction.Builder()
//                .cut(new File(Environment.getExternalStorageDirectory(), "videoeditor/input3.mp4"))
//                .from(5 * 1000)
//                .duration(20 * 1000)
//                .saveAs(new File(Environment.getExternalStorageDirectory(), "videoeditor/clip.mp4"))
//                .build();
//        cutAction.start(new ActionCallback() {
//            @Override
//            public void onStarted(int event) {
//
//            }
//
//            @Override
//            public void onProgress(int event, float progress) {
//
//            }
//
//            @Override
//            public void onSuccess(int event) {
//
//            }
//
//            @Override
//            public void onFailed(int event) {
//
//            }
//        });

//        TransAacHandlerPure transAacHandlerPure = new TransAacHandlerPure(
//                new File(Environment.getExternalStorageDirectory(), "videoeditor/music.mp3").getAbsolutePath(),
//                new File(Environment.getExternalStorageDirectory(), "videoeditor/music.aac").getAbsolutePath()
//        );
//        transAacHandlerPure.start();

//        AudioTransCodeAction transCodeAction = new AudioTransCodeAction.Builder()
//                .transCode(new File(Environment.getExternalStorageDirectory(), "videoeditor/music1.mp3"))
//                .to(new File(Environment.getExternalStorageDirectory(), "videoeditor/transcode.aac"))
////                .from(20 * 1000)
////                .duration(10 * 1000)
//                .targetFormat(AudioTransCodeAction.FORMAT.AAC)
//                .build();
//        transCodeAction.start(new ActionCallback() {
//            @Override
//            public void onStarted() {
//                Log.d(TAG, "onStarted: ");
//            }
//
//            @Override
//            public void onProgress(float progress) {
////                Log.d(TAG, "onProgress: " + progress);
//            }
//
//            @Override
//            public void onSuccess() {
//                Log.d(TAG, "onSuccess: ");
//            }
//
//            @Override
//            public void onFailed() {
//                Log.d(TAG, "onFailed: ");
//            }
//        });

//        Test.getPcmData(new File(Environment.getExternalStorageDirectory(), "videoeditor/music.mp3"),
//                new File(Environment.getExternalStorageDirectory(), "videoeditor/output1.pcm"));
//        Test.playPcm(new File(Environment.getExternalStorageDirectory(), "videoeditor/tmp.pcm"));
//        Test.mixPcm(
//                new File(Environment.getExternalStorageDirectory(), "videoeditor/output.pcm"),
//                new File(Environment.getExternalStorageDirectory(), "videoeditor/output1.pcm"),
//                new File(Environment.getExternalStorageDirectory(), "videoeditor/mixed.pcm")
//        );

//        VideoBgmAddAction bgmAddAction = new VideoBgmAddAction.Builder()
//                .edit(new File(Environment.getExternalStorageDirectory(), "videoeditor/input1.mp4"))
//                .withBgm(new File(Environment.getExternalStorageDirectory(), "videoeditor/music.aac"))
//                .videoFrom(5 * 1000)
//                .videoDuration(20 * 1000)
//                .bgmFrom(15 * 1000)
//                .override(true)
//                .saveAs(new File(Environment.getExternalStorageDirectory(), "videoeditor/withbgm.mp4"))
//                .build();
//        VideoEditorManager.getManager()
//                .edit(new File(Environment.getExternalStorageDirectory(), "videoeditor/input1.mp4"),
//                new VideoEditCallback())
//                .withAction(bgmAddAction)
//                .saveAs(new File(Environment.getExternalStorageDirectory(), "videoeditor/output.mp4"))
//                .commit();

//        Test.addBackgroundMusic(
//                new File(Environment.getExternalStorageDirectory(), "videoeditor/music.aac"),
//                new File(Environment.getExternalStorageDirectory(), "videoeditor/input3.mp4"),
//                new File(Environment.getExternalStorageDirectory(), "videoeditor/withbgm.mp4")
//        );


//        VideoBgmRemoveAction removeAction = new VideoBgmRemoveAction.Builder()
//                .removeBgm(new File(Environment.getExternalStorageDirectory(), "videoeditor/input3.mp4"))
//                .from(5 * 1000)
//                .duration(10 * 1000)
//                .saveAs(new File(Environment.getExternalStorageDirectory(), "videoeditor/removebgm.mp4"))
//                .build();
//        removeAction.start(new ActionCallback() {
//            @Override
//            public void onStarted(int event) {
//
//            }
//
//            @Override
//            public void onProgress(int event, float progress) {
//
//            }
//
//            @Override
//            public void onSuccess(int event) {
//
//            }
//
//            @Override
//            public void onFailed(int event) {
//
//            }
//        });

//        VideoMergeAction mergeAction = new VideoMergeAction.Builder()
//                .merge(new File(Environment.getExternalStorageDirectory(), "videoeditor/input1.mp4"))
//                .merge(new File(Environment.getExternalStorageDirectory(), "videoeditor/input2.mp4"))
//                .merge(new File(Environment.getExternalStorageDirectory(), "videoeditor/input2.mp4"))
//                .merge(new File(Environment.getExternalStorageDirectory(), "videoeditor/input2.mp4"))
//                .merge(new File(Environment.getExternalStorageDirectory(), "videoeditor/input2.mp4"))
//                .merge(new File(Environment.getExternalStorageDirectory(), "videoeditor/input2.mp4"))
//                .merge(new File(Environment.getExternalStorageDirectory(), "videoeditor/input2.mp4"))
//                .merge(new File(Environment.getExternalStorageDirectory(), "videoeditor/input2.mp4"))
//                .merge(new File(Environment.getExternalStorageDirectory(), "videoeditor/input2.mp4"))
//                .merge(new File(Environment.getExternalStorageDirectory(), "videoeditor/input2.mp4"))
//                .saveAs(new File(Environment.getExternalStorageDirectory(), "videoeditor/concat.mp4"))
//                .build();
//        mergeAction.start(new ActionCallback() {
//            @Override
//            public void onStarted(int event) {
//                Log.d(TAG, "onStarted: ");
//                mStartTime = System.currentTimeMillis();
//            }
//
//            @Override
//            public void onProgress(int event, float progress) {
//                Log.d(TAG, "onProgress: ");
//            }
//
//            @Override
//            public void onSuccess(int event) {
//                Log.d(TAG, "onSuccess: ");
//                mEndTime = System.currentTimeMillis();
//                Log.d(TAG, "Time consumed : " + (mEndTime - mStartTime) + "ms.");
//            }
//
//            @Override
//            public void onFailed(int event) {
//                Log.d(TAG, "onFailed: ");
//            }
//        });

        VideoWatermarkAddAction watermarkAddAction = new VideoWatermarkAddAction.Builder()
                .watermark(BitmapFactory.decodeResource(getResources(), R.drawable.watermark))
                .atXPos(200)
                .atYPos(400)
                .build();
        VideoEditorManager.getManager()
                .edit(new File(Environment.getExternalStorageDirectory(), "videoeditor/input1.mp4"))
                .withAction(watermarkAddAction)
                .saveAs(new File(Environment.getExternalStorageDirectory(), "videoeditor/withwatermark.mp4"))
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

//        VideoClipper clipper = new VideoClipper();
//        clipper.setInputVideoPath(new File(Environment.getExternalStorageDirectory(), "videoeditor/input1.mp4").getAbsolutePath());
//        clipper.setOutputVideoPath(new File(Environment.getExternalStorageDirectory(), "videoeditor/withwatermark.mp4").getAbsolutePath());
//        clipper.setOnVideoCutFinishListener(new VideoClipper.OnVideoCutFinishListener() {
//            @Override
//            public void onFinish() {
//                Log.d(TAG, "onFinish: ");
//            }
//        });
//        try {
//            clipper.clipVideo(0, 15 * 1000 * 1000);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}
