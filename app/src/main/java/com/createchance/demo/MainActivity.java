package com.createchance.demo;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.createchance.simplevideoeditor.VideoBgmAddAction;
import com.createchance.simplevideoeditor.VideoBgmRemoveAction;
import com.createchance.simplevideoeditor.VideoCutAction;
import com.createchance.simplevideoeditor.VideoEditCallback;
import com.createchance.simplevideoeditor.VideoEditorManager;
import com.createchance.simplevideoeditor.VideoMergeAction;
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
//        Test.addBackgroundMusic(new File(Environment.getExternalStorageDirectory(), "videoeditor/music.aac"),
//                new File(Environment.getExternalStorageDirectory(), "videoeditor/input2.mp4"),
//                new File(Environment.getExternalStorageDirectory(), "videoeditor/output.mp4"));

//        try {
//            MediaExtractor mediaExtractor = new MediaExtractor();
//            mediaExtractor.setDataSource(new File(Environment.getExternalStorageDirectory(),
//                    "videoeditor/output.mp4").getAbsolutePath());
//            Log.d(TAG, "Edit done, output track count: " + mediaExtractor.getTrackCount());
//            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
//                MediaFormat mediaFormat = mediaExtractor.getTrackFormat(i);
//                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
//                Log.d(TAG, "Edit done, output mime: " + mime);
//            }
//
//            return;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        VideoWatermarkAddAction watermarkAddAction = new VideoWatermarkAddAction.Builder()
                .watermark(BitmapFactory.decodeResource(getResources(), R.drawable.watermark))
                .atXPos(200)
                .atYPos(400)
                .build();
        VideoBgmAddAction bgmAddAction = new VideoBgmAddAction.Builder()
                .bgmFile(new File(Environment.getExternalStorageDirectory(), "videoeditor/music.mp3"))
                .videoFrom(5 * 1000)
//                .videoDuration(7 * 1000)
//                .bgmFrom(5 * 1000)
                .build();
        VideoBgmRemoveAction bgmRemoveAction = new VideoBgmRemoveAction.Builder()
                .from(3 * 1000)
                .duration(8 * 1000)
                .build();
        VideoCutAction cutAction = new VideoCutAction.Builder()
                .from(5 * 1000)
                .duration(10 * 1000)
                .build();
        VideoMergeAction mergeAction = new VideoMergeAction.Builder()
                .merge(new File(Environment.getExternalStorageDirectory(), "/DCIM/Camera/VID_20180313_152651.mp4"))
                .merge(new File(Environment.getExternalStorageDirectory(), "/DCIM/Camera/VID_20180330_181524.mp4"))
                .merge(new File(Environment.getExternalStorageDirectory(), "/DCIM/Camera/VID_20180401_103412.mp4"))
                .merge(new File(Environment.getExternalStorageDirectory(), "/DCIM/Camera/VID_20180404_104840.mp4"))
                .build();
        VideoEditorManager.getManager()
                .edit(new File(Environment.getExternalStorageDirectory(), "videoeditor/input2.mp4"))
                .withAction(bgmAddAction)
                .withAction(watermarkAddAction)
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
